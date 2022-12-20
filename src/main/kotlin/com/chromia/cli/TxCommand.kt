package com.chromia.cli

import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.groups.defaultByName
import com.github.ajalt.clikt.parameters.groups.groupSwitch
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.client.config.PostchainClientConfig
import net.postchain.gtv.parse.GtvParser
import org.apache.commons.configuration2.BaseConfiguration
import java.util.*

class TxCommand : CliktCommand(help = "Make a transaction") {
    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    private val settings by settingsOptionNotRequired()
    private val secret by secretOption()
    private val target by option(help = "Make tx towards this target (default: --local)").groupSwitch(
            "--deployment" to RemoteDeploymentOption { settings?.model ?: settingsOptionDefault() },
            "--local" to LocalDeploymentOption()
    ).defaultByName("--local")
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
        val clientConfig = BaseConfiguration().run {
            setProperty("brid", target.brid.toHex())
            setProperty("api.url", target.url)
            secret?.let { s ->
                Properties().apply { load(s.inputStream()) }.let { p ->
                    p["pubkey"]?.let { setProperty("pubkey", it) }
                    p["privkey"]?.let { setProperty("privkey", it) }
                }
            }
            PostchainClientConfig.fromConfiguration(this)
        }
        val res = target.createClient(clientConfig)
                .transactionBuilder()
                .addOperation(opName, *args.toTypedArray())
                .run {
                    if (nop) addNop()
                    if (awaitConfirmation) postAwaitConfirmation() else post()
                }
        println("transaction with rid ${res.txRid} was posted ${res.status}${res.rejectReason?.let { ": $it" } ?: ""}")
    }
}
