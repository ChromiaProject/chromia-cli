package com.chromia.build.tools.compile

import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.CompileModel
import java.io.File
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
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.base.RellCliEnv
import net.postchain.rell.module.RellPostchainModuleFactory

internal class BlockchainConfigurationGenerator(
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

    fun generate(): Collection<ChromiaCompileResult> {
        return blockchainModels.toList().map { generateConfiguration(it.first, it.second) }
    }

    fun generateConfiguration(name: String, model: BlockchainModel): ChromiaCompileResult {
        val gtvModel = generateGtv(model)
        val configholder = ChromiaCompileResult(name, gtvModel)
        validateGtvConfiguration(gtvModel)
        return configholder
    }

    private fun validateGtvConfiguration(configuration: Gtv) {
        try {
            val gtvBuilder = GtvBuilder()
            gtvBuilder.update(configuration)
            val gtxModules = configuration["gtx"]?.get("modules")!!.asArray() // Not null since default values are added
                    .filter { it.asString() !in whiteListedGtxModules }
                    .map { GtvNode.decode(it) }
                    .let { GtvArrayNode(it, GtvArrayMerge.REPLACE) }

            gtvBuilder.update(gtxModules, "gtx", "modules")

            GTXBlockchainConfigurationFactory.validateConfiguration(
                    gtvBuilder.build(),
                    BlockchainRid.ZERO_RID // dummy blockchain RID, works with Rell and all standard GTX modules, might not work properly with custom GTX modules
            )
        } catch (e: UserMistake) {
            throw ValidationException(e.message!!)
        }
    }

    private fun generateGtv(blockchainModel: BlockchainModel): Gtv {
        val b = GtvBuilder()
        addDefault(b, blockchainModel)

        val config = RellApiCompile.Config.Builder()
                .cliEnv(cliEnv)
                .moduleArgs(blockchainModel.moduleArgs)
                .mountConflictError(true)
                .moduleArgsMissingError(true)
                .version(compileModel.langVersion)
                .quiet(compileModel.quiet)
                .build()

        val rellBcConfig = RellApiCompile.compileGtv(config, compileModel.sourceFile(sourceDir), blockchainModel.module)
        b.update(rellBcConfig, "gtx", "rell")

        blockchainModel.config.filterKeys { it != "modules" }
                .forEach { (path, value) -> b.update(value, path) }

        return b.build()
    }

    private fun addDefault(b: GtvBuilder, blockchainModel: BlockchainModel) {
        // TODO: override these from config ([BlockchainModel.config])
        b.update(gtv("name" to gtv(BaseBlockBuildingStrategy::class.qualifiedName!!)), "blockstrategy")
        b.update(gtv(GTXBlockchainConfigurationFactory::class.qualifiedName!!), "configurationfactory")
        b.update(gtv("HEADER_HASH"), "config_consensus_strategy")
        b.update(gtv(2000), "revolt", "fast_revolt_status_timeout")
        b.update(gtv(listOf()), "signers")

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
