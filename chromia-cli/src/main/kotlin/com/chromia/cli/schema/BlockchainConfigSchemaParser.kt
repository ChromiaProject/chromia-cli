package com.chromia.cli.schema

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvString
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.base.model.R_App
import net.postchain.rell.base.model.R_KeyIndexKind
import java.io.File
import java.nio.file.Files

class BlockchainConfigSchemaParser {

    fun parse(blockchainConfig: Gtv): Schema {
        val sources = extractSources(blockchainConfig)
        val rellVersion = extractRellVersion(blockchainConfig)
        val appModules = extractAppModules(blockchainConfig)
        return parseSources(sources, rellVersion, appModules)
    }

    private fun extractRellVersion(blockchainConfig: Gtv): String {
        val rellVersion = blockchainConfig["gtx"]
                ?.get("rell")
                ?.get("version") as? GtvString
                ?: throw IllegalArgumentException("No rell version found in blockchain config")

        return rellVersion.asString()
    }

    private fun extractAppModules(blockchainConfig: Gtv): List<String>? {
        val appModules = blockchainConfig["gtx"]
                ?.get("rell")
                ?.get("modules") as? GtvArray

        return appModules?.asArray()?.map { it.asString() }
    }
    private fun parseSources(sources: Map<String, String>, rellVersion: String, appModules: List<String>?): Schema {
        val sourceDir = createFilesInTempFolder(sources)
        val conf = RellApiCompile.Config.Builder()
                .moduleArgsMissingError(false)
                .mountConflictError(false)
                .docSymbolsEnabled(false)
                .version(rellVersion)
                .quiet(true)
                .build()
        val app = RellApiCompile.compileApp(conf, sourceDir, appModules)
        return createSchema(app)
    }

    fun createFilesInTempFolder(filesMap: Map<String, String>): File {
        val tempDir = Files.createTempDirectory("rell_source_files_").toFile()

        filesMap.forEach { (relativePath, content) ->
            val fullPath = tempDir.resolve(relativePath)
            fullPath.parentFile.mkdirs()
            fullPath.writeText(content)
        }

        return tempDir
    }

    private fun extractSources(blockchainConfig: Gtv): Map<String, String> {
        val sources = blockchainConfig["gtx"]
                ?.get("rell")
                ?.get("sources") as? GtvDictionary
                ?: throw IllegalArgumentException("No sources found in blockchain config")

        return sources.dict.mapValues { it.value.asString() }
    }

    private fun createSchema(app: R_App): Schema {
        val entities = app.modules.flatMap { module ->
            module.entities.values.map {
                val fields = it.strAttributes.map {
                    // TODO: extract default values
                    Field(it.key, it.value.type.str(), false, null, getIndexKind(it.value.keyIndexKind))
                }
                Entity(it.mountName.str(), fields)
            }
        }

        val objects = app.modules.flatMap { module ->
            module.objects.values.map {
                val fields = it.rEntity.strAttributes.map {
                    // TODO: extract default values
                    Field(it.key, it.value.type.str(), false, null, getIndexKind(it.value.keyIndexKind))
                }
                Entity(it.rEntity.mountName.str(), fields, isObject = true)
            }
        }

        return Schema(entities + objects)
    }

    private fun getIndexKind(kind: R_KeyIndexKind?): IndexKind? {
        return when(kind) {
            R_KeyIndexKind.KEY -> IndexKind.UNIQUE
            R_KeyIndexKind.INDEX -> IndexKind.INDEX
            else -> null
        }
    }
}
