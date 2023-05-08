package com.chromia.cli.compile.config

import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.CompileModel
import com.chromia.cli.util.withSigner
import com.github.ajalt.clikt.core.CliktError
import net.postchain.base.BaseBlockBuildingStrategy
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.builder.GtvBuilder
import net.postchain.gtv.builder.GtvBuilder.GtvArrayMerge
import net.postchain.gtv.builder.GtvBuilder.GtvArrayNode
import net.postchain.gtv.builder.GtvBuilder.GtvNode
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import net.postchain.gtx.StandardOpsGTXModule
import net.postchain.rell.module.RellPostchainModuleFactory
import net.postchain.rell.utils.cli.RellCliApi
import net.postchain.rell.utils.cli.RellCliBasicException
import net.postchain.rell.utils.cli.RellCliCompileConfig
import net.postchain.rell.utils.cli.RellCliEnv
import java.io.File

class BlockchainConfigurationGenerator(
        private val cliEnv: RellCliEnv,
        private val compileModel: CompileModel,
        private val blockchainModels: Map<String, BlockchainModel>,
        private val sourceDir: File) {

    private val whiteListedGtxModules = listOf(
            "net.postchain.d1.anchoring.system.SystemAnchoringGTXModule",
            "net.postchain.d1.anchoring.cluster.ClusterAnchoringGTXModule",
            "net.postchain.d1.icmf.IcmfSenderGTXModule",
            "net.postchain.d1.icmf.IcmfReceiverGTXModule",
            "net.postchain.d1.iccf.IccfGTXModule",
            "net.postchain.eif.EifGTXModule",
    )

    fun generate(): Collection<BlockchainConfigHolder> {
        return blockchainModels.toList().map { generateConfiguration(it.first, it.second) }
    }

    fun generateConfiguration(name: String, model: BlockchainModel): BlockchainConfigHolder {
        val gtvModel = generateGtv(model)
        val configholder = BlockchainConfigHolder.from(name, gtvModel)
        validateGtvConfiguration(gtvModel, configholder.brid)
        return configholder
    }

    private fun validateGtvConfiguration(configuration: Gtv, generatedBlockchainRid: BlockchainRid) {
        try {
            val gtvBuilder = GtvBuilder()
            gtvBuilder.update(configuration)
            val gtxModules = configuration["gtx"]?.get("modules")!!.asArray() // Not null since default values are added
                    .filter { it.asString() !in whiteListedGtxModules }
                    .map { GtvNode.decode(it) }
                    .let { GtvArrayNode(it, GtvArrayMerge.REPLACE) }

            gtvBuilder.update(gtxModules, "gtx", "modules")

            GTXBlockchainConfigurationFactory.validateConfiguration(withSigner(gtvBuilder.build(), "000000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray()), generatedBlockchainRid)
        } catch (e: UserMistake) {
            throw CliktError(e.message)
        }
    }

    private fun generateGtv(blockchainModel: BlockchainModel): Gtv {
        val b = GtvBuilder()
        addDefault(b, blockchainModel)

        val config = RellCliCompileConfig.Builder()
                .cliEnv(cliEnv)
                .moduleArgs(blockchainModel.moduleArgs)
                .mountConflictError(true)
                .moduleArgsMissingError(true)
                .version(compileModel.langVersion)
                .quiet(compileModel.quiet)
                .build()

        try {
            val rellBcConfig = RellCliApi.compileGtv(config, sourceDir, blockchainModel.module)
            b.update(rellBcConfig, "gtx", "rell")
        } catch (e: RellCliBasicException) {
            throw CliktError(e.message, e)
        }

        blockchainModel.config.filterKeys { it != "modules" }
                .forEach { (path, value) -> b.update(value, path) }

        return b.build()
    }

    private fun addDefault(b: GtvBuilder, blockchainModel: BlockchainModel) {
        // TODO: override these from config ([BlockchainModel.config])
        b.update(gtv("name" to gtv(BaseBlockBuildingStrategy::class.qualifiedName!!)), "blockstrategy")
        b.update(gtv(GTXBlockchainConfigurationFactory::class.qualifiedName!!), "configurationfactory")
        b.update(gtv("HEADER_HASH"), "config_consensus_strategy")
        b.update(gtv(1000), "revolt", "fast_revolt_status_timeout")

        val modulesGtv: MutableList<Gtv> = mutableListOf(
                gtv(RellPostchainModuleFactory::class.qualifiedName!!),
                gtv(StandardOpsGTXModule::class.qualifiedName!!)
        )
        blockchainModel.config["modules"]?.let {
            modulesGtv.add(it)
        }
        b.update(gtv(modulesGtv), "gtx", "modules")
    }
}
