package com.chromia.cli

import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.client.config.PostchainClientConfig
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.parse.GtvParser
import org.apache.commons.configuration2.BaseConfiguration


class QueryCommand : CliktCommand(help = "Make a query towards a running node") {
    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    private val settings by settingsOptionNotRequired()
    private val explicitTarget by LocalDeploymentOption()
    private val deploymentTarget by RemoteDeploymentOption { settings?.model ?: settingsOptionDefault() }.cooccurring()

    private val queryName by argument(help = "name of the query to make.")
    private val args by argument(help = "arguments to pass to the query. The dict is passed either as key-value pairs or as a single dict element.")
            .multiple()
            .transformAll { try { createDict(it) } catch (e: Exception){ echo(e.message)} }
            .validate { require(it is GtvDictionary) { "query must be done with named parameters in a dict" } }

    private fun createDict(args: List<String>): Gtv {
        return when {
            args.isEmpty() -> GtvFactory.gtv(mapOf())
            args.size == 1 && args[0].startsWith("{") && args[0].endsWith("}") -> GtvParser.parse(args[0])
            else -> GtvParser.parse("{${args.joinToString(",")}}")
        }
    }


    override fun run() {
        val target = deploymentTarget ?: explicitTarget!!
        val clientConfig = BaseConfiguration().run {
            setProperty("brid", target.brid.toHex())
            setProperty("api.url", target.url)
            PostchainClientConfig.fromConfiguration(this)
        }
        try {
            val res = target
                    .createClient(clientConfig)
                    .query(queryName, args as Gtv)
            echo(res)
        } catch (e: Exception) {
            echo(e.message)
        }
    }
}
