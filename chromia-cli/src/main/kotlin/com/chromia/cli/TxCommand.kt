package com.chromia.cli

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.LocalDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.secretOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.common.types.WrappedByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.Name
import net.postchain.gtv.parse.GtvParser


class TxCommand : CliktCommand(help = "Make a transaction") {

    private val settings by optionalChromiaModelConfigOption()
    private val secret by secretOption()
    private val explicitTarget by LocalDeploymentOption()
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel() }.cooccurring()
    private val awaitConfirmation by option("--await", "-a", help = "Wait for transaction to be included in a block").flag()
    private val nop by option("-nop", help = "Adds a nop to the transaction").flag()
    private val ftAuth by option().flag()

    private val opName by argument(help = "name of the operation to execute.")

    private val args by argument(help = "arguments to pass to the operation.", helpTags = mapOf(
            "integer" to "123",
            "string" to "foo, \"bar\"",
            "bytearray" to "will be encoded using the rell notation x\"<myByteArray>\" and will initially be interpreted as a hex-string.",
            "array" to "[foo,123]",
            "dict" to "{key1=value1,key2=value2}"
    ))
            .multiple()
            .transformAll { args ->
                args.map { GtvParser.parse(it) }
            }

    data class AuthDescriptor(
            @Name("id") val id: WrappedByteArray,
            @Name("args") val args: List<Gtv>,
            @Name("created") val created: Long,
            @Name("auth_type") val authType: String, // Enum
            @Name("rules") val rules: Gtv // Can be GtvNull
    )

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val postchainClientConfig = settings.config.get(target.url, target.brid, secret)
        val client = target.createClient(postchainClientConfig)
        val transactionBuilder = client.transactionBuilder()
        if (ftAuth) {
            val signer = postchainClientConfig.signers.singleOrNull() ?: throw PrintMessage("A single keypair is required to use ft authentication")
            val accountId = client.query("ft4.get_accounts_by_participant_id", gtv(mapOf("id" to gtv(signer.pubKey.data)))).asArray()
            echo(accountId.map { it.toString() })
            if (accountId.isEmpty()) throw PrintMessage("No accounts found for pubkey ${signer.pubKey}")
            val authDescriptors = client.query("ft4.get_account_auth_descriptors_by_participant_id", gtv(mapOf("account_id" to accountId.first(), "participant_id" to gtv(signer.pubKey.data)))).asArray()
            echo(authDescriptors.map { it.asDict() })
            val flags = client.query("ft4.get_auth_flags", gtv(mapOf("op_name" to gtv(opName)))).asArray().map { it.asString() }
            val matchingDescriptor = authDescriptors.map { it.asDict() }.first { flags.any { flag -> it["args"]!!.asArray().first().asArray().map { it.asString() }.contains(flag) }}
            echo(flags)
            transactionBuilder.addOperation("ft4.ft_auth", accountId.first(), matchingDescriptor["id"]!!)
        }
        val res = transactionBuilder
                .addOperation(opName, *args.toTypedArray())
                .run {
                    if (nop) addNop()
                    if (awaitConfirmation) postAwaitConfirmation() else post()
                }
        echo("transaction with rid ${res.txRid} was posted ${res.status}${res.rejectReason?.let { ": $it" } ?: ""}")
    }
}
