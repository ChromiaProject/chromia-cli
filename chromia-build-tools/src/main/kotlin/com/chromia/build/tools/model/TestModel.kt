package com.chromia.cli.model

import com.chromia.build.tools.model.ensureType
import net.postchain.gtv.Gtv
import net.postchain.gtv.listMapAndPrimitivesToGtv

data class TestModel(
        val modules: List<String> = listOf(),
        val moduleArgs: Map<String, Map<String, Gtv>> = mapOf(),
        val failOnError: Boolean = true,
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun load(data: Map<String, Any>, vararg path: String) = TestModel(
                modules = ensureType<List<String>?>(data["modules"], *path, "test", "modules")
                        ?: listOf(),
                moduleArgs = ensureType<Map<String, Any>?>(data["moduleArgs"], *path, "test", "moduleArgs")
                        ?.mapValues { a ->
                            (a.value as Map<String, Any?>).mapValues { b -> listMapAndPrimitivesToGtv(b.value) }
                        } ?: mapOf(),
                failOnError = ensureType<Boolean?>(data["failOnError"], *path, "test", "failOnError")
                        ?: true
        )
    }
}
