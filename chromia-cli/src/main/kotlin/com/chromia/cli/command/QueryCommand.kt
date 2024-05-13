package com.chromia.cli.command

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.DASH_DASH_DESCRIPTION
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
import net.postchain.gtv.GtvString
import net.postchain.gtv.parse.GtvParser


class QueryCommand : CliktCommand(help = """
    Make a query towards a running node
    
    Note: For query arguments:
    $DASH_DASH_DESCRIPTION
""".trimIndent()
) {

    private val settings by optionalChromiaModelConfigOption()
    private val explicitTarget by LocalDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()

    private val queryName by argument(help = "name of the query to make.")
    private val args by argument(help = "arguments to pass to the query, passed either as key=value pairs or as a single dict.", helpTags = mapOf(
            "integer" to "123",
            "big_integer" to "1234L",
            "string" to "foo, \"bar\"",
            "bytearray" to "will be encoded using the rell notation x\"<myByteArray>\" and will initially be interpreted as a hex-string.",
            "array" to "[foo,123]",
            "dict" to """["key1":value1,"key2":value2]""",
    ))
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
            args.size == 1 && args[0].startsWith("[") && args[0].endsWith("]") -> GtvParser.parse(args[0])
            else -> GtvDictionary.build(args.associate {
                val (key, value) = it.split("=", limit = 2)
                key to try {
                    GtvParser.parse(value)
                } catch (e: IllegalArgumentException) {
                    GtvString(value)
                }
            })
        }
    }


    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val clientConfig = settings.config.setApiUrls(target.url).setBrid(target.brid)
        val res = target.createClient(clientConfig)
                .query(queryName, args as Gtv)
        echo(res)
    }
}
