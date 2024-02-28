package com.chromia.cli.model

import net.postchain.gtv.listMapAndPrimitivesToGtv
import net.postchain.gtv.Gtv

data class TestModel(
        val modules: List<String> = listOf(),
        val moduleArgs: Map<String, Map<String, Gtv>> = mapOf(),
        val failOnError: Boolean = true,
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun load(data: Map<String, Any>) = TestModel(
                modules = data["modules"]?.let { it as List<String> } ?: listOf(),
                moduleArgs = (data["moduleArgs"] as Map<String, Any>?)?.mapValues { a ->
                    (a.value as Map<String, Any?>).mapValues { b -> listMapAndPrimitivesToGtv(b.value) }
                } ?: mapOf(),
                failOnError = data["failOnError"]?.let { it as Boolean } ?: true
        )
    }
}
