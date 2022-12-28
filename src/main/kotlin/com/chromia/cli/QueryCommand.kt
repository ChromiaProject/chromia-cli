package com.chromia.cli

import com.chromia.cli.util.LocalDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.settingsOptionDefault
import com.chromia.cli.util.settingsOptionNotRequired
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.groups.defaultByName
import com.github.ajalt.clikt.parameters.groups.groupSwitch
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
    private val target by option(help = "Make query towards this target (default: --local)").groupSwitch(
            "--deployment" to RemoteDeploymentOption { settings?.model ?: settingsOptionDefault() },
            "--local" to LocalDeploymentOption()
    ).defaultByName("--local")

    private val queryName by argument(help = "name of the query to make.")
    private val args by argument(help = "arguments to pass to the query. The dict is passed either as key-value pairs or as a singe dict element.")
            .multiple()
            .transformAll { createDict(it) }
            .validate { require(it is GtvDictionary) { "query must be done with named parameters in a dict" } }

    private fun createDict(args: List<String>): Gtv {
        return when {
            args.isEmpty() -> GtvFactory.gtv(mapOf())
            args.size == 1 && args[0].startsWith("{") && args[0].endsWith("}") -> GtvParser.parse(args[0])
            else -> GtvParser.parse("{${args.joinToString(",")}}")
        }
    }


    override fun run() {
        val clientConfig = BaseConfiguration().run {
            setProperty("brid", target.brid.toHex())
            setProperty("api.url", target.url)
            PostchainClientConfig.fromConfiguration(this)
        }
        val res = target
                .createClient(clientConfig)
                .query(queryName, args)

        echo(res)
    }
}
