package com.chromia.cli.compile.config

import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.util.withSigner
import com.github.ajalt.clikt.core.CliktError
import com.chromia.cli.model.CompileModel
import net.postchain.base.BaseBlockBuildingStrategy
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import net.postchain.gtx.StandardOpsGTXModule
import net.postchain.rell.RellConfigGen
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.module.ConfigConstants
import net.postchain.rell.module.RellPostchainModuleFactory
import net.postchain.rell.tools.runcfg.RunConfigGtvBuilder
import net.postchain.rell.utils.PostchainUtils
import net.postchain.rell.utils.RellCliEnv

class BlockchainConfigurationGenerator(
        private val cliEnv: RellCliEnv,
        private val compileModel: CompileModel,
        private val blockchainModels: Map<String, BlockchainModel>,
        private val sourceDir: C_SourceDir) {
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
            GTXBlockchainConfigurationFactory.validateConfiguration(withSigner(configuration, "000000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray()), generatedBlockchainRid)
        } catch (e: UserMistake) {
            throw CliktError(e.message)
        }
    }

    private fun generateGtv(blockchainModel: BlockchainModel): Gtv {
        val b = RunConfigGtvBuilder()
        addDefault(b, blockchainModel)

        val configGen = RellConfigGen.create(cliEnv, sourceDir, listOf(R_ModuleName.of(blockchainModel.module)))
        val sources = configGen.getModuleSources()

        val srcGtv = gtv(
                "modules" to gtv(listOf(gtv(blockchainModel.module))),
                ConfigConstants.RELL_SOURCES_KEY to gtv(sources.files.mapValues { (_, v) -> gtv(v) }),
                ConfigConstants.RELL_VERSION_KEY to gtv(compileModel.langVersion.str())
        )
        b.update(srcGtv, "gtx", "rell")

        if (blockchainModel.moduleArgs.isNotEmpty()) {
            b.update(gtv(blockchainModel.moduleArgs.mapValues { gtv(it.value) }), "gtx", "rell", "moduleArgs")
        }
        blockchainModel.config.filterKeys { it != "modules" }
                .forEach { (path, value) -> b.update(value, path) }

        return b.build()
    }

    private fun addDefault(b: RunConfigGtvBuilder, blockchainModel: BlockchainModel) {
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
