package com.chromia.cli.model

import com.chromia.cli.parser.listMapAndPrimitivesToGtv
import net.postchain.gtv.Gtv

data class BlockchainModel(
        val module: String,
        val moduleArgs: Map<String, Map<String, Gtv>> = mapOf(),
        val config: Map<String, Gtv> = mapOf(),
        val test: TestModel = TestModel()
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun load(data: Map<String, Any>) = BlockchainModel(
                module = data["module"] as String,
                moduleArgs = (data["moduleArgs"] as Map<String, Any>?)?.mapValues { a ->
                    (a.value as Map<String, Any?>).mapValues { b -> listMapAndPrimitivesToGtv(b.value) }
                } ?: mapOf(),
                config = (data["config"] as Map<String, Any?>?)?.mapValues { listMapAndPrimitivesToGtv(it.value) } ?: mapOf(),
                test = data["test"]?.let { TestModel.load(it as Map<String, Any>) } ?: TestModel()
        )
    }
}
