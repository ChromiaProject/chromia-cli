package com.chromia.api.impl.compile

import com.chromia.cli.model.BlockchainModel
import net.postchain.base.BaseBlockBuildingStrategy
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.builder.GtvBuilder
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import net.postchain.gtx.StandardOpsGTXModule
import net.postchain.rell.module.RellPostchainModuleFactory

val standardGtxModules = listOf(
        RellPostchainModuleFactory::class.qualifiedName!!,
        StandardOpsGTXModule::class.qualifiedName!!,
)

internal fun GtvBuilder.addDefaultEntries(blockchainModel: BlockchainModel, extraModules: List<String> = listOf()) = apply {
    // TODO: override these from config ([BlockchainModel.config])
    update(gtv("name" to gtv(BaseBlockBuildingStrategy::class.qualifiedName!!)), "blockstrategy")
    update(gtv(GTXBlockchainConfigurationFactory::class.qualifiedName!!), "configurationfactory")
    update(gtv(true), "add_primary_key_to_header")
    update(gtv("HEADER_HASH"), "config_consensus_strategy")
    update(gtv(2000), "revolt", "fast_revolt_status_timeout")
    update(gtv(true), "revolt", "revolt_when_should_build_block")
    update(gtv(1000), "blockstrategy", "mininterblockinterval")

    val modulesGtv: MutableList<Gtv> = mutableListOf()
    extraModules.forEach { modulesGtv.add(gtv(it)) }
    modulesGtv.add(gtv(StandardOpsGTXModule::class.qualifiedName!!))
    blockchainModel.config["modules"]?.let {
        modulesGtv.add(it)
    }
    update(gtv(modulesGtv), "gtx", "modules")
    blockchainModel.config.filterKeys { it != "modules" }
            .forEach { (path, value) -> update(value, path) }
    renameIcmfBrid()
}

private fun GtvBuilder.renameIcmfBrid() = apply {
    build()["icmf"]?.get("receiver")?.get("local")?.asArray()
            ?.map { it.asDict().toMutableMap() }
            ?.map(::renameBridKeyName)
            ?.map { GtvBuilder.GtvNode.decode(gtv(it)) }
            ?.let { GtvBuilder.GtvArrayNode(it, GtvBuilder.GtvArrayMerge.REPLACE) }
            ?.apply { update(this, "icmf", "receiver", "local") }
}

private fun renameBridKeyName(localReceiver: MutableMap<String, Gtv>) = localReceiver.also {
    it.remove("brid")?.let { brid -> it["bc-rid"] = brid }
}
