package com.chromia.cli.model

import com.chromia.build.tools.model.ensureType
import net.postchain.gtv.Gtv
import net.postchain.gtv.listMapAndPrimitivesToGtv

data class BlockchainModel(
        val module: String,
        val type: Type,
        val moduleArgs: Map<String, Map<String, Gtv>> = mapOf(),
        val config: Map<String, Gtv> = mapOf(),
        val test: TestModel = TestModel()
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun load(data: Map<String, Any>, additionalParameter: String) = BlockchainModel(
                module = ensureType<String>(data["module"], "blockchain", "module"),
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
                        ?: TestModel()
        )
    }

    enum class Type {
        BLOCKCHAIN,
        LIBRARY,
    }
}
