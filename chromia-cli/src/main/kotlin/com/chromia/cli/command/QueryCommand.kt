package com.chromia.cli.command

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.ExplicitDeploymentOption
import com.chromia.cli.util.OutputFormat
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.formatJson
import com.chromia.cli.util.formatRaw
import com.chromia.cli.util.formatXml
import com.chromia.cli.util.formatYaml
import com.chromia.cli.util.outputFormat
import com.chromia.cli.util.parseArgAsGtv
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.parse.GtvParser
import net.postchain.gtv.pretty

class QueryCommand : ChromiaCommand(help = """
    Make a query towards a running node
    
    Examples:
    ```
    # query primitive_args(arg1: integer, arg2: name, arg3: text, arg4: byte_array, arg5: my_enum)
    chr query primitive_args 'arg1=123' 'arg2=Alice' 'arg3="My Neighbor"' 'arg4=x"AB12"' 'arg5=0'
    # query dict_arg(arg: map<text, integer>)
    chr query dict_arg 'arg=["key": 12]'
    # query map_arg(arg: map<my_enum, text>)
    chr query map_arg 'arg=[[0, "first"],[1, "second"]]'
    # query struct_arg(arg: my_struct)
    chr query struct_arg 'arg=[12, "structs are arrays", x"AB"]'
    
    # Use -- do avoid additional quotes
    chr query my_query -- arg1=foo arg2=x"AB12"
    ```
""".trimIndent()
) {
    private val settings by optionalChromiaModelConfigOption()
    private val explicitTarget by ExplicitDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()

    private val outputFormat by outputFormat()
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
            args.isEmpty() -> gtv(mapOf())
            args.size == 1 && args[0].startsWith("[") && args[0].endsWith("]") -> GtvParser.parse(args[0])
            else -> GtvDictionary.build(args.associate {
                val (key, value) = it.split("=", limit = 2)
                key to parseArgAsGtv(value)
            })
        }
    }

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val clientConfig = settings.config.setApiUrls(target.urls).setBrid(target.brid)
        val client = target.createClient(clientConfig)

        val gtv = client.query(queryName, args as Gtv)
        echo(when (outputFormat) {
            OutputFormat.pretty -> gtv.pretty()
            OutputFormat.raw -> formatRaw(gtv)
            OutputFormat.JSON -> formatJson(gtv)
            OutputFormat.XML -> formatXml(gtv)
            OutputFormat.YAML -> formatYaml(gtv)
        })
    }
}
