package com.chromia.build.tools.compile

import com.chromia.api.result.BlockchainConfiguration
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.CompileModel
import com.chromia.cli.model.MinimalRellVersionStrictGtv
import net.postchain.base.BaseBlockBuildingStrategy
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
        private val compileModel: CompileModel) {

    fun generate(blockchainModelMap: Map<String, BlockchainModel>): List<BlockchainConfiguration> {
        return blockchainModelMap.toList().map { generateConfiguration(it.first, it.second) }
    }

    private fun generateConfiguration(name: String, model: BlockchainModel): BlockchainConfiguration {
        val gtvModel = generateGtv(model)
        val configholder = BlockchainConfiguration(name, gtvModel)
        return configholder
    }

    private fun renameIcmfBrid(configuration: Gtv): Gtv {
        val gtvBuilder = GtvBuilder()
        gtvBuilder.update(configuration)

        configuration["icmf"]?.get("receiver")?.get("local")?.asArray()
                ?.map { it.asDict().toMutableMap() }
                ?.map(::renameBridKeyName)
                ?.map { GtvNode.decode(gtv(it)) }
                ?.let { GtvArrayNode(it, GtvArrayMerge.REPLACE) }
                ?.apply { gtvBuilder.update(this, "icmf", "receiver", "local") }

        return gtvBuilder.build()
    }

    private fun renameBridKeyName(localReceiver: MutableMap<String, Gtv>) = localReceiver.also {
        it.remove("brid")?.let { brid -> it["bc-rid"] = brid }
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

        val rellBcConfig = RellApiCompile.compileGtv(config, compileModel.source.toFile(), blockchainModel.module)
        b.update(rellBcConfig, "gtx", "rell")

        blockchainModel.config.filterKeys { it != "modules" }
                .forEach { (path, value) -> b.update(value, path) }
        return b.build()
                .let { renameIcmfBrid(it) }
    }

    private fun addDefault(b: GtvBuilder, blockchainModel: BlockchainModel) {
        // TODO: override these from config ([BlockchainModel.config])
        b.update(gtv("name" to gtv(BaseBlockBuildingStrategy::class.qualifiedName!!)), "blockstrategy")
        b.update(gtv(GTXBlockchainConfigurationFactory::class.qualifiedName!!), "configurationfactory")
        b.update(gtv("HEADER_HASH"), "config_consensus_strategy")
        b.update(gtv(2000), "revolt", "fast_revolt_status_timeout")
        b.update(gtv(1000), "blockstrategy", "mininterblockinterval")

        val modulesGtv: MutableList<Gtv> = mutableListOf(
                gtv(RellPostchainModuleFactory::class.qualifiedName!!),
                gtv(StandardOpsGTXModule::class.qualifiedName!!)
        )
        blockchainModel.config["modules"]?.let {
            modulesGtv.add(it)
        }
        b.update(gtv(modulesGtv), "gtx", "modules")

        if (compileModel.langVersion >= MinimalRellVersionStrictGtv) {
            b.update(gtv(compileModel.strictGtvConversion), "gtx", "rell", "strictGtvConversion")
        }
    }
}
