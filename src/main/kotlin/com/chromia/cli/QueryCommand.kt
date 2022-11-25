package com.chromia.cli

import com.chromia.cli.model.ChromiaCliModel
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.client.cli.encodeArg
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory
import org.apache.commons.configuration2.BaseConfiguration


sealed class DeploymentOption(name: String, help: String? = null) : OptionGroup(name, help) {
    abstract val brid: BlockchainRid
    abstract val url: String
}

class RemoteDeploymentOption(val settings: ChromiaCliModel) : DeploymentOption("Deployment", help = "Make query towards a configured deployment") {
    private val deployment by option().required()
    private val blockchain by option().required()

    init {
        require(settings.deployment[deployment] != null) { "Deployment named $deployment not found in configuration" }
        require(settings.deployment[deployment]!!.chains[blockchain] != null ) { "Blockchain named $blockchain not found in deployment configuration"}
    }

    override val brid: BlockchainRid
        get() = settings.deployment[deployment]!!.chains[blockchain]!!

    override val url: String
        get() = settings.deployment[deployment]!!.apiUrl
}

class LocalDeploymentOption : DeploymentOption("Node", help = "Make query towards a test node") {
    private val blockchainRid by option().required()
    private val apiUrl by option().default("http://localhost:7740")

    override val brid: BlockchainRid
        get() = BlockchainRid.buildFromHex(blockchainRid)

    override val url: String
        get() = apiUrl
}

class QueryCommand : CliktCommand(help = "Make a query towards a running node") {
    private val settings by settingsOption()
    private val target by option().groupSwitch(
            "--deployment" to RemoteDeploymentOption(settings),
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
            args.size == 1 && args[0].startsWith("{") && args[0].endsWith("}") -> encodeArg(args[0])
            else -> encodeArg("{${args.joinToString(",")}}")
        }
    }


    override fun run() {
        val clientConfig = BaseConfiguration().apply {
            setProperty("brid", target.brid.toHex())
            setProperty("api.url", target.url)
        }
        val res = PostchainClientImpl(PostchainClientConfig.fromConfiguration(clientConfig))
                .query(queryName, args)
        println(res)
    }
}