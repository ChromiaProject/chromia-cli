package com.chromia.cli.check_compatilibity

import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.CompileModel
import com.chromia.cli.model.DatabaseModel
import com.chromia.cli.model.parseModel
import com.chromia.cli.util.addWhitelistedGTXModules
import net.postchain.common.exception.UserMistake
import net.postchain.gtv.Gtv
import net.postchain.rell.api.base.RellApiBaseInternal
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.gtx.RellApiGtxInternal
import net.postchain.rell.api.gtx.RellApiRunTests
import net.postchain.rell.base.compiler.base.utils.C_SourceDir
import net.postchain.rell.base.compiler.base.utils.C_SourcePath
import net.postchain.rell.base.compiler.base.utils.C_TextSourceFile
import net.postchain.rell.base.model.R_ModuleName
import net.postchain.rell.base.utils.PostchainGtvUtils
import net.postchain.rell.base.utils.RellGtxConfigConstants
import net.postchain.rell.base.utils.toImmList
import net.postchain.rell.base.utils.toImmMap
import java.io.File
import kotlin.io.path.Path

/**
 * Loads blockchains from XML/model files and build apps
  */
open class BlockchainSource(
        val source: C_SourceDir,
        val model: ChromiaModel,
) {
    companion object {

        fun fromChromiaFile(modelFile: String): BlockchainSource {
            return fromChromiaFile(File(modelFile))
        }

        fun fromChromiaFile(modelFile: File): BlockchainSource {
            val model by lazy { parseModel(modelFile) }
            val source = C_SourceDir.diskDir(model.compile.source.toFile())

            return BlockchainSource(source, cloneModel(model))
        }

        fun fromXmlFile(blockchainName: String, configXmlFile: String): BlockchainSource {
            return fromXmlFile(blockchainName, File(configXmlFile))
        }

        fun fromXmlFile(blockchainName: String, configXmlFile: File): BlockchainSource {

            val (model, source) = loadXmlSource(configXmlFile, blockchainName)

            return BlockchainSource(source, cloneModel(model))
        }

        fun cloneModel(model: ChromiaModel): ChromiaModel {
            return ChromiaModel(
                    database = DatabaseModel(),
                    compile = model.compile,
                    blockchains = model.blockchains,
                    deployments = model.deployments,
            )
        }

        private fun loadXmlSource(xmlFile: File, blockchainName: String): Pair<ChromiaModel, C_SourceDir> {
            val xmlToGtv = PostchainGtvUtils.xmlToGtv(
                    xmlFile.readText())

            val gtxNode = xmlToGtv.asDict().getValue("gtx").asDict()
            val rellNode = gtxNode.getValue("rell").asDict()

            val ma = rellNode["moduleArgs"]?.asDict()
                    ?.mapValues { value -> value.value.asDict() }
                    ?: mapOf()

            val config = mutableMapOf<String, Gtv>()
            config.putAll(xmlToGtv.asDict())
            config.remove("gtx")
            val blockchainModel = BlockchainModel(
                    rellNode["modules"]!![0].asString(),
                    moduleArgs = ma,
                    config = config,
                    type = BlockchainModel.Type.BLOCKCHAIN,
            )

            val source = createSource(rellNode)
            val chromiaModel = ChromiaModel(
                    compile = CompileModel(
                            source = Path("."),
                            target = Path("."),
                            rellVersion = rellNode[RellGtxConfigConstants.LANG_VERSION_KEY]!!.asString(),
                            root = Path("."),
                    ),
                    blockchains = mapOf(blockchainName to blockchainModel)
            )

            return chromiaModel to source
        }

        private fun createSource(rellNode: Map<String, Gtv>): C_SourceDir {
            val sourcesNode = rellNode.getValue(RellGtxConfigConstants.SOURCES_KEY)

            val fileMap = sourcesNode.asDict()
                    .mapValues { (_, v) -> v.asString() }
                    .mapKeys { (k, _) -> parseSourcePath(k) }
                    .mapValues { (k, v) -> C_TextSourceFile(k, v) }
                    .toImmMap()


            return C_SourceDir.mapDir(fileMap)
        }

        private fun parseSourcePath(s: String): C_SourcePath {
            val path = C_SourcePath.parseOpt(s)
            return path ?: throw UserMistake("Invalid file path: '$s'")
        }
    }

    fun buildApp(blockchainName: String, logger: LogWrapper): CompiledApp {

        val chainConfig = model.blockchains[blockchainName]
                ?: throw UserMistake("Blockchain not found")

        val appModules = listOf(chainConfig.module)
        val additionalModules = chainConfig.config["gtx"]?.get("modules")?.asArray()?.map { it.asString() }

        val moduleArgs = (chainConfig.moduleArgs.asSequence() + chainConfig.test.moduleArgs.asSequence())
                .groupBy({ it.key }, { it.value })
                .mapValues { (_, values) ->
                    values.flatMap { map -> map.entries }.associate(Map.Entry<String, Gtv>::toPair)
                }
        val config = createTestConfig(moduleArgs, additionalModules, model, logger)

        val rAppModules = appModules.map { R_ModuleName.of(it) }.toImmList()

        val compileConfig = config.compileConfig
        val options = RellApiGtxInternal.makeRunTestsCompilerOptions(config)
        val (_, app) = RellApiBaseInternal.compileApp(compileConfig, options, source, rAppModules, listOf())

        return CompiledApp(config, options, app, rAppModules)
    }

    private fun createTestConfig(moduleArgs: Map<String, Map<String, Gtv>>,
                                 additionalGtxModules: List<String>? = null,
                                 model: ChromiaModel,
                                 logger: LogWrapper): RellApiRunTests.Config {
        val compileConf = RellApiCompile.Config.Builder()
                .moduleArgs(moduleArgs)
                .includeTestSubModules(true)
                .appModuleInTestsError(true)
                .addWhitelistedGTXModules(additionalGtxModules)
                .moduleArgsMissingError(true)
                .mountConflictError(true)
                .version(model.compile.langVersion)
                .quiet(false)
                .build()
        return RellApiRunTests.Config.Builder()
                .compileConfig(compileConf)
                .databaseUrl("${model.databaseUrl}&currentSchema=${model.databaseSchema}_tests")
                .stopOnError(true)
                .sqlErrorLog(true)
                .logPrinter(logger::echo)
                .outPrinter(logger::echo)
                .printTestCases(false)
                .sqlLog(logger.verbose)
                .build()
    }
}