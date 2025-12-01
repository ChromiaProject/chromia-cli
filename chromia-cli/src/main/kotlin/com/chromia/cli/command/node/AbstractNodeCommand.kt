package com.chromia.cli.command.node

import com.chromia.api.ChromiaCompileApi
import com.chromia.api.filterBlockchains
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.build.tools.iccf.SingleNodeIccfGtxModule
import com.chromia.build.tools.icmf.InMemoryIcmfReceiverGtxModule
import com.chromia.build.tools.icmf.InMemoryIcmfReceiverSynchronizationInfrastructureExtension
import com.chromia.build.tools.icmf.InMemoryIcmfSenderGtxModule
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.compile.NodeConfig
import com.chromia.cli.d1.ManagementChainFactory
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.util.BuildCliEnv
import com.chromia.cli.util.hideLibWarningsOption
import com.chromia.cli.util.nodePropertiesOption
import com.chromia.cli.util.readBlockchainConfigFile
import com.chromia.cli.util.removeKnownGtxModules
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.builder.GtvBuilder

abstract class AbstractNodeCommand(help: String) : ChromiaCommand(help = help) {
    protected val settings by chromiaModelOption()
    protected val blockchainConfigs by option(
            "-bc", "--blockchain-config",
            help = "Manually specify which blockchain-configs to run"
    )
            .file(mustExist = true, canBeDir = false)
            .multiple()

    protected val name by option(help = "Only start specified blockchains (multiple)", metavar = "NAME")
            .multiple()

    protected val managedMode by option("-mm", "--managed-mode", help = "Start in managed mode", hidden = true).flag()
    private val overrides by option("-p", help = "Override any property value (usage: -p key=value)", metavar = "KEY=VALUE").associate()
    private val nodeConfigFile by nodePropertiesOption()
    protected val nodeConfig by lazy {
        nodeConfigFile ?: NodeConfig.getDefaultNodeConfig(settings.model, managedMode, overrides)
    }
    private val directoryChainMock by option(
            help = """
                Adds a blockchain on ID 0 that responds to the cluster management api and anchoring api.
                Used together with integration tests involving frontend clients. 
                Can be used with node discovery features, ICCF and cross-chain transfers using the FT-protocol
            """.trimIndent()
    ).flag()

    protected val hideLibWarnings by hideLibWarningsOption()

    protected fun extractConfigs(): Collection<BlockchainConfiguration> {
        val configsToAdd = if (blockchainConfigs.isEmpty()) {
            val model = settings.model.filterBlockchains { bc, model ->
                model.type == BlockchainModel.Type.BLOCKCHAIN && (name.isEmpty() || name.contains(bc))
            }
            require(model.blockchains.isNotEmpty()) {
                "No blockchain configurations found in: ${settings.modelFilePath}"
            }
            ChromiaCompileApi.build(BuildCliEnv(this, hideLibWarnings), model)
        } else {
            blockchainConfigs
                    .filter { name.isEmpty() || name.contains(it.nameWithoutExtension) }
                    .associate {
                        it.nameWithoutExtension to readBlockchainConfigFile(it)
                    }
                    .map { BlockchainConfiguration(it.key, it.value) }

        }

        return configsToAdd
                .let { if (directoryChainMock) addDirectoryChain(it) else it }
                .map { if (managedMode) it else replaceInMemoryIcmf(it) }
                .map { if (managedMode) it else it.removeKnownGtxModules(CliktCliEnv(this)) }
                .onEach { it.validate() }
    }

    private fun addDirectoryChain(chains: List<BlockchainConfiguration>): List<BlockchainConfiguration> {
        return buildList {
            add(BlockchainConfiguration("directory-chain", ManagementChainFactory.createManagementChain()))
            addAll(chains)
        }
    }

    private fun replaceInMemoryIcmf(configuration: BlockchainConfiguration): BlockchainConfiguration {
        val cliEnv = CliktCliEnv(this)
        val gtvBuilder = GtvBuilder()
        gtvBuilder.update(configuration.config)

        val configModules = configuration.config["gtx"]?.get("modules")!!.asArray().map { it.asString() } // Not null since default values are added
        if (configModules.intersect(inMemoryGtxModules.keys).isNotEmpty()) {
            cliEnv.error("WARNING: Replacing GTX Module with in-memory version, all unprocessed messages will be lost upon node restart")
            cliEnv.error("DO NOT RUN IN PRODUCTION")
        }
        configModules
                .map { GtvFactory.gtv(inMemoryGtxModules.getOrDefault(it, it)) }
                .map { GtvBuilder.GtvNode.decode(it) }
                .let { GtvBuilder.GtvArrayNode(it, GtvBuilder.GtvArrayMerge.REPLACE) }
                .apply { gtvBuilder.update(this, "gtx", "modules") }

        configuration.config["sync_ext"]?.asArray()
                ?.map { GtvFactory.gtv(inMemorySyncInfraExt.getOrDefault(it.asString(), it.asString())) }
                ?.map { GtvBuilder.GtvNode.decode(it) }
                ?.let { GtvBuilder.GtvArrayNode(it, GtvBuilder.GtvArrayMerge.REPLACE) }
                ?.apply { gtvBuilder.update(this, "sync_ext") }
        return BlockchainConfiguration(configuration.name, gtvBuilder.build())
    }

    companion object {

        private val inMemoryGtxModules = mapOf(
                "net.postchain.d1.icmf.IcmfSenderGTXModule" to InMemoryIcmfSenderGtxModule::class.qualifiedName!!,
                "net.postchain.d1.icmf.IcmfReceiverGTXModule" to InMemoryIcmfReceiverGtxModule::class.qualifiedName!!,
                "net.postchain.d1.iccf.IccfGTXModule" to SingleNodeIccfGtxModule::class.qualifiedName!!,
        )

        private val inMemorySyncInfraExt = mapOf(
                "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension" to InMemoryIcmfReceiverSynchronizationInfrastructureExtension::class.qualifiedName!!
        )
    }
}
