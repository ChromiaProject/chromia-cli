package com.chromia.build.tools.compile

import com.chromia.build.tools.iccf.SingleNodeIccfGtxModule
import com.chromia.build.tools.icmf.InMemoryIcmfReceiverGtxModule
import com.chromia.build.tools.icmf.InMemoryIcmfReceiverSynchronizationInfrastructureExtension
import com.chromia.build.tools.icmf.InMemoryIcmfSenderGtxModule
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.CompileModel
import com.chromia.cli.model.MinimalRellVersionStrictGtv
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
import java.io.File

internal class BlockchainConfigurationGenerator(
        private val cliEnv: RellCliEnv,
        private val compileModel: CompileModel,
        private val blockchainModels: Map<String, BlockchainModel>,
        private val sourceDir: File,
        private val filterModules: Boolean,
        private val inMemoryIcmf: Boolean,
        private val validateGtv: Boolean) {

    private val whiteListedGtxModules = listOf(
            "net.postchain.d1.anchoring.system.SystemAnchoringGTXModule",
            "net.postchain.d1.anchoring.cluster.ClusterAnchoringGTXModule",
            "net.postchain.d1.icmf.IcmfSenderGTXModule",
            "net.postchain.d1.icmf.IcmfReceiverGTXModule",
            "net.postchain.d1.iccf.IccfGTXModule",
    )

    private val inMemoryGtxModules = mapOf(
            "net.postchain.d1.icmf.IcmfSenderGTXModule" to InMemoryIcmfSenderGtxModule::class.qualifiedName!!,
            "net.postchain.d1.icmf.IcmfReceiverGTXModule" to InMemoryIcmfReceiverGtxModule::class.qualifiedName!!,
            "net.postchain.d1.iccf.IccfGTXModule" to SingleNodeIccfGtxModule::class.qualifiedName!!,
    )

    private val inMemorySyncInfraExt = mapOf(
            "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension" to InMemoryIcmfReceiverSynchronizationInfrastructureExtension::class.qualifiedName!!
    )

    fun generate(): Collection<ChromiaCompileResult> {
        return blockchainModels.toList().map { generateConfiguration(it.first, it.second) }
    }

    private fun generateConfiguration(name: String, model: BlockchainModel): ChromiaCompileResult {
        val gtvModel = generateGtv(model)
        if (validateGtv) {
            validateGtvConfiguration(gtvModel)
        }
        val configholder = ChromiaCompileResult(name, gtvModel)
        return configholder
    }

    private fun validateGtvConfiguration(configuration: Gtv) {
        try {
            val filteredConfiguration = filterModules(configuration)
            GTXBlockchainConfigurationFactory.validateConfiguration(
                    withSigner(filteredConfiguration, "000000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray()),
                    BlockchainRid.ZERO_RID // dummy blockchain RID, works with Rell and all standard GTX modules, might not work properly with custom GTX modules
            )
        } catch (e: UserMistake) {
            throw ValidationException(e.message!!)
        } catch (e: ClassNotFoundException) {
            throw ValidationException("Could not find Gtx module: ${e.message!!}")
        }
    }

    private fun filterModules(configuration: Gtv): Gtv {

        val configModules = configuration["gtx"]?.get("modules")!!.asArray() // Not null since default values are added
        val intersect = configModules.intersect(whiteListedGtxModules)

        if (intersect.isNotEmpty()) {
            cliEnv.error("Warning filtering out modules from configuration;\n ${intersect.joinToString("\n")}")
        }

        val gtvBuilder = GtvBuilder()
        gtvBuilder.update(configuration)
        val gtxModules = configModules
                .filter { it.asString() !in whiteListedGtxModules }
                .map { GtvNode.decode(it) }
                .let { GtvArrayNode(it, GtvArrayMerge.REPLACE) }

        gtvBuilder.update(gtxModules, "gtx", "modules")
        return gtvBuilder.build()
    }

    private fun replaceInMemoryIcmf(configuration: Gtv): Gtv {
        val gtvBuilder = GtvBuilder()
        gtvBuilder.update(configuration)

        val configModules = configuration["gtx"]?.get("modules")!!.asArray().map { it.asString() } // Not null since default values are added
        if (configModules.intersect(inMemoryGtxModules.keys).isNotEmpty()) {
            cliEnv.error("WARNING: Replacing GTX Module with in-memory version, all unprocessed messages will be lost upon node restart")
            cliEnv.error("DO NOT RUN IN PRODUCTION")
        }
        configModules
                .map { gtv(inMemoryGtxModules.getOrDefault(it, it)) }
                .map { GtvNode.decode(it) }
                .let { GtvArrayNode(it, GtvArrayMerge.REPLACE) }
                .apply { gtvBuilder.update(this, "gtx", "modules") }

        configuration["sync_ext"]?.asArray()
                ?.map { gtv(inMemorySyncInfraExt.getOrDefault(it.asString(), it.asString())) }
                ?.map { GtvNode.decode(it) }
                ?.let { GtvArrayNode(it, GtvArrayMerge.REPLACE) }
                ?.apply { gtvBuilder.update(this, "sync_ext") }
        return gtvBuilder.build()
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

        val rellBcConfig = RellApiCompile.compileGtv(config, compileModel.sourceFile(sourceDir), blockchainModel.module)
        b.update(rellBcConfig, "gtx", "rell")

        blockchainModel.config.filterKeys { it != "modules" }
                .forEach { (path, value) -> b.update(value, path) }
        return b.build()
                .let { renameIcmfBrid(it) }
                .let { if (inMemoryIcmf) replaceInMemoryIcmf(it) else it }
                .let { if (filterModules) filterModules(it) else it }
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
