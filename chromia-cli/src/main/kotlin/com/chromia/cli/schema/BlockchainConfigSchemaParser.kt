package com.chromia.cli.schema

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvString
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.base.model.KeyIndexKind
import net.postchain.rell.base.model.rr.RR_App
import net.postchain.rell.base.model.rr.RR_PrimitiveKind
import net.postchain.rell.base.model.rr.RR_Type
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

    private fun createSchema(app: RR_App): Schema {
        val entities = app.modules.flatMap { module ->
            module.entities.values.map { entity ->
                val fields = entity.strAttributes.map { (name, attr) ->
                    Field(name, typeToString(attr.type, app), false, null, getIndexKind(attr.keyIndexKind))
                }
                Entity(entity.mountName.str(), fields)
            }
        }

        val objects = app.modules.flatMap { module ->
            module.objects.values.map { obj ->
                val fields = obj.rEntity.strAttributes.map { (name, attr) ->
                    Field(name, typeToString(attr.type, app), false, null, getIndexKind(attr.keyIndexKind))
                }
                Entity(obj.rEntity.mountName.str(), fields, isObject = true)
            }
        }

        val enums = app.modules.flatMap { module ->
            module.enums.values.map { enum ->
                val fields = enum.attrs.map { EnumField(it.name.str, it.value) }
                Enum(enum.base.appLevelName, fields)
            }
        }

        return Schema(entities + objects, enums)
    }

    private fun getIndexKind(kind: KeyIndexKind?): IndexKind? {
        return when (kind) {
            KeyIndexKind.KEY -> IndexKind.UNIQUE
            KeyIndexKind.INDEX -> IndexKind.INDEX
            else -> null
        }
    }

    private fun typeToString(type: RR_Type, app: RR_App): String = when (type) {
        is RR_Type.Primitive -> when (type.kind) {
            RR_PrimitiveKind.BOOLEAN -> "boolean"
            RR_PrimitiveKind.INTEGER -> "integer"
            RR_PrimitiveKind.BIG_INTEGER -> "big_integer"
            RR_PrimitiveKind.DECIMAL -> "decimal"
            RR_PrimitiveKind.TEXT -> "text"
            RR_PrimitiveKind.BYTE_ARRAY -> "byte_array"
            RR_PrimitiveKind.ROWID -> "rowid"
            RR_PrimitiveKind.GUID -> "guid"
            RR_PrimitiveKind.SIGNER -> "signer"
            RR_PrimitiveKind.JSON -> "json"
            RR_PrimitiveKind.GTV -> "gtv"
            RR_PrimitiveKind.RANGE -> "range"
            RR_PrimitiveKind.UNIT -> "unit"
            RR_PrimitiveKind.NOTHING -> "nothing"
        }
        RR_Type.Null -> "null"
        is RR_Type.Entity -> app.allEntities[type.defIndex].base.appLevelName
        is RR_Type.Struct -> app.allStructs[type.defIndex].base.appLevelName
        is RR_Type.Enum -> app.allEnums[type.defIndex].base.appLevelName
        is RR_Type.Object -> app.allObjects[type.defIndex].base.appLevelName
        is RR_Type.Nullable -> "${typeToString(type.value, app)}?"
        is RR_Type.List -> "list<${typeToString(type.element, app)}>"
        is RR_Type.Set -> "set<${typeToString(type.element, app)}>"
        is RR_Type.Map -> "map<${typeToString(type.key, app)}, ${typeToString(type.value, app)}>"
        is RR_Type.Tuple -> type.fields.joinToString(", ", "(", ")") {
            if (it.name != null) "${it.name}: ${typeToString(it.type, app)}" else typeToString(it.type, app)
        }
        is RR_Type.Function -> "(${type.params.joinToString(", ") { typeToString(it, app) }}) -> ${typeToString(type.result, app)}"
        is RR_Type.VirtualList -> "virtual<list<${typeToString(type.element, app)}>>"
        is RR_Type.VirtualSet -> "virtual<set<${typeToString(type.element, app)}>>"
        is RR_Type.VirtualMap -> "virtual<map<${typeToString(type.key, app)}, ${typeToString(type.value, app)}>>"
        is RR_Type.VirtualStruct -> "virtual<${app.allStructs[type.defIndex].base.appLevelName}>"
        is RR_Type.VirtualTuple -> "virtual<${type.fields.joinToString(", ", "(", ")") {
            if (it.name != null) "${it.name}: ${typeToString(it.type, app)}" else typeToString(it.type, app)
        }}>"
        is RR_Type.Generic -> "${type.name}<${type.args.joinToString(", ") { typeToString(it, app) }}>"
        is RR_Type.Operation -> app.allOperations[type.defIndex].base.appLevelName
        RR_Type.Error -> "<error>"
    }
}
