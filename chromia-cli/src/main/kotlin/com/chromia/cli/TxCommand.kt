package com.chromia.cli

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.LocalDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.secretOption
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.common.types.WrappedByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.Name
import net.postchain.gtv.mapper.Nullable
import net.postchain.gtv.mapper.toList
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
            @Name("args") val args: Gtv,
            @Name("created") val created: Long,
            @Name("auth_type") val authType: String, // Enum
            @Name("rules") @Nullable val rules: Gtv?
    ) {
        val flags by lazy { args.asArray().first().asArray().map { it.asString() } }
        fun containsFlag(flag: String) = flags.contains(flag)
    }

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val postchainClientConfig = settings.config.get(target.url, target.brid, secret)
        val client = target.createClient(postchainClientConfig)
        val transactionBuilder = client.transactionBuilder()
        if (ftAuth) {
            val signer = postchainClientConfig.signers.singleOrNull()
                    ?: throw PrintMessage("A single keypair is required to use ft authentication", statusCode = 1)
            val accountIds = client.query("ft4.get_accounts_by_participant_id", gtv(mapOf("id" to gtv(signer.pubKey.data)))).asArray()
            echo(accountIds.map { it.toString() })
            if (accountIds.isEmpty()) throw PrintMessage("No ft accounts found for pubkey ${signer.pubKey}", statusCode = 1)
            val accountId = if (accountIds.size > 1) {
                terminal.prompt("More than one account found, which one should we use: ${accountIds.map { it.asByteArray().toHex() }}", choices = accountIds.map { it.asByteArray().toHex() })
                        ?.let { gtv(it.hexStringToByteArray()) }
                        ?: throw Abort()
            } else accountIds.first()
            val authDescriptors = client
                    .query(
                            "ft4.get_account_auth_descriptors_by_participant_id",
                            gtv(mapOf("account_id" to accountId, "participant_id" to gtv(signer.pubKey.data)))
                    )
                    .toList<AuthDescriptor>()
            val flags = client.query("ft4.get_auth_flags", gtv(mapOf("op_name" to gtv(opName)))).asArray().map { it.asString() }
            val matchingDescriptor = authDescriptors.firstOrNull { flags.any { flag -> it.containsFlag(flag) } }
                    ?: throw PrintMessage("No valid account descriptor found. User not authorized for operation $opName", statusCode = 1)
            transactionBuilder.addOperation("ft4.ft_auth", accountIds.first(), gtv(matchingDescriptor.id))
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
