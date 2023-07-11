package com.chromia.cli

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.LocalDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.secretOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.gtv.parse.GtvParser


class TxCommand : CliktCommand(help = "Make a transaction") {

    private val settings by optionalChromiaModelConfigOption()
    private val secret by secretOption()
    private val explicitTarget by LocalDeploymentOption()
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel() }.cooccurring()
    private val awaitConfirmation by option("--await", "-a", help = "Wait for transaction to be included in a block").flag()
    private val nop by option("-nop", help = "Adds a nop to the transaction").flag()

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

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val res = target.createClient(settings.config.get(target.url, target.brid, secret))
                .transactionBuilder()
                .addOperation(opName, *args.toTypedArray())
                .run {
                    if (nop) addNop()
                    if (awaitConfirmation) postAwaitConfirmation() else post()
                }
        echo("transaction with rid ${res.txRid} was posted ${res.status}${res.rejectReason?.let { ": $it" } ?: ""}")
    }
}
