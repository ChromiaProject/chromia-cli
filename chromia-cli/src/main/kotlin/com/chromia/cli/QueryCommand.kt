package com.chromia.cli

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.LocalDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.parse.GtvParser


class QueryCommand : CliktCommand(help = "Make a query towards a running node") {

    private val settings by optionalChromiaModelConfigOption()
    private val explicitTarget by LocalDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel() }.cooccurring()

    private val queryName by argument(help = "name of the query to make.")
    private val args by argument(help = "arguments to pass to the query. The dict is passed either as key-value pairs or as a single dict element.")
            .multiple()
            .transformAll {
                try {
                    createDict(it)
                } catch (e: Exception) {
                    echo(e.message)
                }
            }
            .validate { require(it is GtvDictionary) { "query must be done with named parameters in a dict" } }

    private fun createDict(args: List<String>): Gtv {
        return when {
            args.isEmpty() -> GtvFactory.gtv(mapOf())
            args.size == 1 && args[0].startsWith("{") && args[0].endsWith("}") -> GtvParser.parse(args[0])
            else -> GtvParser.parse("{${args.joinToString(",")}}")
        }
    }


    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val clientConfig = settings.config.get(target.url, target.brid)
            val res = target.createClient(clientConfig)
                    .query(queryName, args as Gtv)
            echo(res)
    }
}
