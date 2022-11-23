package com.chromia.cli.compile.config

import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.ChromiaCliModel
import net.postchain.common.BlockchainRid
import net.postchain.config.app.AppConfig
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.rell.RellConfigGen
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.module.ConfigConstants
import net.postchain.rell.tools.runcfg.RunConfigGtvBuilder
import net.postchain.rell.utils.MainRellCliEnv
import net.postchain.rell.utils.PostchainUtils

class BlockchainConfigurationGenerator(private val config: ChromiaCliModel, private val sourceDir: C_SourceDir, private val nodeProperties: AppConfig) {
    fun generateMaps(): Map<BlockchainRid, Gtv> {
        return config.blockchains.toList()
                .map { (k,v) -> generateGtv(v) }
                .associateBy { BlockchainRid(PostchainUtils.calcBlockchainRid(it).toByteArray()) }
    }

    fun generateForOne(model: BlockchainModel): Pair<BlockchainRid, Gtv> {
        val gtvModel = generateGtv(model)
        return Pair(
                BlockchainRid(PostchainUtils.calcBlockchainRid(gtvModel).toByteArray()),
                gtvModel
        )
    }

    fun generateGtv(bcConfig: BlockchainModel): Gtv {
        val b = RunConfigGtvBuilder()
        addDefault(b, bcConfig)

        val configGen = RellConfigGen.create(MainRellCliEnv, sourceDir, listOf(R_ModuleName.of(bcConfig.module)))
        val sources = configGen.getModuleSources()

        val srcGtv = gtv(
                "modules" to gtv(listOf(gtv(bcConfig.module))),
                ConfigConstants.RELL_SOURCES_KEY to gtv(sources.files.mapValues { (_, v) -> gtv(v) }),
                ConfigConstants.RELL_VERSION_KEY to gtv(config.rellVersion.str())
        )
        b.update(srcGtv, "gtx", "rell")

        if (bcConfig.moduleArgs.isNotEmpty()) {
            println(bcConfig.moduleArgs)
            b.update(gtv(bcConfig.moduleArgs.map { gtv(it.value) }), "gtx", "rell", "moduleArgs")
        }

        return b.build()
    }

    fun addDefault(b: RunConfigGtvBuilder, config: BlockchainModel) {
        b.update(gtv("name" to gtv("net.postchain.base.BaseBlockBuildingStrategy")), "blockstrategy")
        b.update(gtv("net.postchain.gtx.GTXBlockchainConfigurationFactory"), "configurationfactory")

        val modulesGtv: MutableList<Gtv> = mutableListOf(
                gtv("net.postchain.rell.module.RellPostchainModuleFactory"),
                gtv("net.postchain.gtx.StandardOpsGTXModule")
        )
        if (config.additionalGtv != GtvNull) {
            config.additionalGtv.get("gtx")?.get("modules")?.let {
                modulesGtv.add(it)
            }
        }
        b.update(gtv(modulesGtv), "gtx", "modules")
        b.update(gtv(listOf(gtv(this.nodeProperties.pubKeyByteArray))), "signers" )
    }
}