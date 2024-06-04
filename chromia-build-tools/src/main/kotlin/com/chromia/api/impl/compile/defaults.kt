package com.chromia.api.impl.compile

import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.CompileModel
import com.chromia.cli.model.MinimalRellVersionStrictGtv
import net.postchain.base.BaseBlockBuildingStrategy
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.builder.GtvBuilder
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import net.postchain.gtx.StandardOpsGTXModule
import net.postchain.rell.module.RellPostchainModuleFactory

internal fun GtvBuilder.addDefaultEntries(blockchainModel: BlockchainModel, compileModel: CompileModel) = apply {
    // TODO: override these from config ([BlockchainModel.config])
    update(GtvFactory.gtv("name" to GtvFactory.gtv(BaseBlockBuildingStrategy::class.qualifiedName!!)), "blockstrategy")
    update(GtvFactory.gtv(GTXBlockchainConfigurationFactory::class.qualifiedName!!), "configurationfactory")
    update(GtvFactory.gtv(true), "add_primary_key_to_header")
    update(GtvFactory.gtv("HEADER_HASH"), "config_consensus_strategy")
    update(GtvFactory.gtv(2000), "revolt", "fast_revolt_status_timeout")
    update(GtvFactory.gtv(true), "revolt", "revolt_when_should_build_block")
    update(GtvFactory.gtv(1000), "blockstrategy", "mininterblockinterval")

    val modulesGtv: MutableList<Gtv> = mutableListOf(
            GtvFactory.gtv(RellPostchainModuleFactory::class.qualifiedName!!),
            GtvFactory.gtv(StandardOpsGTXModule::class.qualifiedName!!)
    )
    blockchainModel.config["modules"]?.let {
        modulesGtv.add(it)
    }
    update(GtvFactory.gtv(modulesGtv), "gtx", "modules")
    blockchainModel.config.filterKeys { it != "modules" }
            .forEach { (path, value) -> update(value, path) }
    if (compileModel.langVersion >= MinimalRellVersionStrictGtv) {
        update(GtvFactory.gtv(compileModel.strictGtvConversion), "gtx", "rell", "strictGtvConversion")
    }
    renameIcmfBrid()
}

private fun GtvBuilder.renameIcmfBrid() = apply {
    build()["icmf"]?.get("receiver")?.get("local")?.asArray()
            ?.map { it.asDict().toMutableMap() }
            ?.map(::renameBridKeyName)
            ?.map { GtvBuilder.GtvNode.decode(GtvFactory.gtv(it)) }
            ?.let { GtvBuilder.GtvArrayNode(it, GtvBuilder.GtvArrayMerge.REPLACE) }
            ?.apply { update(this, "icmf", "receiver", "local") }
}

private fun renameBridKeyName(localReceiver: MutableMap<String, Gtv>) = localReceiver.also {
    it.remove("brid")?.let { brid -> it["bc-rid"] = brid }
}
