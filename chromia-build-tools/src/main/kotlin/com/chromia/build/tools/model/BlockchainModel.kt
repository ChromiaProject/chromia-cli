package com.chromia.cli.model

import com.chromia.build.tools.model.ensureType
import net.postchain.gtv.Gtv
import net.postchain.gtv.listMapAndPrimitivesToGtv
import java.nio.file.Path

data class BlockchainModel(
        val module: String? = null,
        val type: Type,
        val moduleArgs: Map<String, Map<String, Gtv>> = mapOf(),
        val config: Map<String, Gtv> = mapOf(),
        val test: TestModel = TestModel(),
        val webStatic: Path? = null,
        val webCacheTtlSeconds: Int? = null,
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun load(data: Map<String, Any>, additionalParameter: String, dir: Path) = BlockchainModel(
                module = ensureType<String?>(data["module"], "blockchain", "module"),
                type = ensureType<String?>(data["type"], "blockchain", "library")?.let { Type.valueOf(it.uppercase()) } ?: Type.BLOCKCHAIN,
                moduleArgs = ensureType<Map<String, Any?>?>(data["moduleArgs"], "blockchain", additionalParameter, "moduleArgs")
                        ?.mapValues { a ->
                            (a.value as Map<String, Any?>).mapValues { b -> listMapAndPrimitivesToGtv(b.value) }
                        } ?: mapOf(),
                config = ensureType<Map<String, Any?>?>(data["config"], "blockchain", additionalParameter, "config")
                        ?.mapValues {
                            listMapAndPrimitivesToGtv(it.value)
                        } ?: mapOf(),
                test = data["test"]?.let { TestModel.load(it as Map<String, Any>, "blockchain", additionalParameter) }
                        ?: TestModel(),
                webStatic = ensureType<String?>(data["webStatic"], "blockchain", "webStatic")?.let { dir.resolve(it) },
                webCacheTtlSeconds = ensureType<Int?>(data["webCacheTtlSeconds"], "blockchain", "webCacheTtlSeconds"),
        )
    }

    enum class Type {
        BLOCKCHAIN,
        LIBRARY,
    }
}
