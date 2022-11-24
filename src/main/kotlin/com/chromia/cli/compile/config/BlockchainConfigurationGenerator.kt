package com.chromia.cli.compile.config

import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.ChromiaCliModel
import net.postchain.base.BaseBlockBuildingStrategy
import net.postchain.common.BlockchainRid
import net.postchain.config.app.AppConfig
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import net.postchain.gtx.StandardOpsGTXModule
import net.postchain.rell.RellConfigGen
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.module.ConfigConstants
import net.postchain.rell.module.RellPostchainModuleFactory
import net.postchain.rell.tools.runcfg.RunConfigGtvBuilder
import net.postchain.rell.utils.MainRellCliEnv
import net.postchain.rell.utils.PostchainUtils

class BlockchainConfigurationGenerator(private val model: ChromiaCliModel, private val sourceDir: C_SourceDir, private val nodeProperties: AppConfig? = null) {
    fun generate(): Map<NamedBlockchainRid, Gtv> {
        return model.blockchains.toList().associate { generateConfiguration(it.first, it.second) }
    }

    fun generateConfiguration(name: String, model: BlockchainModel): Pair<NamedBlockchainRid, Gtv> {
        val gtvModel = generateGtv(model)
        return Pair(
                NamedBlockchainRid(name, BlockchainRid(PostchainUtils.calcBlockchainRid(gtvModel).toByteArray())),
                gtvModel
        )
    }

    private fun generateGtv(blockchainModel: BlockchainModel): Gtv {
        val b = RunConfigGtvBuilder()
        addDefault(b, blockchainModel)

        val configGen = RellConfigGen.create(MainRellCliEnv, sourceDir, listOf(R_ModuleName.of(blockchainModel.module)))
        val sources = configGen.getModuleSources()

        val srcGtv = gtv(
                "modules" to gtv(listOf(gtv(blockchainModel.module))),
                ConfigConstants.RELL_SOURCES_KEY to gtv(sources.files.mapValues { (_, v) -> gtv(v) }),
                ConfigConstants.RELL_VERSION_KEY to gtv(model.rellVersion.str())
        )
        b.update(srcGtv, "gtx", "rell")

        if (blockchainModel.moduleArgs.isNotEmpty()) {
            println(blockchainModel.moduleArgs)
            b.update(gtv(blockchainModel.moduleArgs.map { gtv(it.value) }), "gtx", "rell", "moduleArgs")
        }

        return b.build()
    }

    private fun addDefault(b: RunConfigGtvBuilder, blockchainModel: BlockchainModel) {
        // TODO: override these from config ([BlockchainModel.config])
        b.update(gtv("name" to gtv(BaseBlockBuildingStrategy::class.qualifiedName!!)), "blockstrategy")
        b.update(gtv(GTXBlockchainConfigurationFactory::class.qualifiedName!!), "configurationfactory")

        val modulesGtv: MutableList<Gtv> = mutableListOf(
                gtv(RellPostchainModuleFactory::class.qualifiedName!!),
                gtv(StandardOpsGTXModule::class.qualifiedName!!)
        )
        if (blockchainModel.config != GtvNull) {
            blockchainModel.config.get("gtx")?.get("modules")?.let {
                modulesGtv.add(it)
            }
        }
        b.update(gtv(modulesGtv), "gtx", "modules")
        nodeProperties?.let { b.update(gtv(listOf(gtv(it.pubKeyByteArray))), "signers") }
    }
}