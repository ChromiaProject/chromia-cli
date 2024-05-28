package com.chromia.build.tools.model

import com.chromia.build.tools.compile.ValidationException
import net.postchain.common.BlockchainRid

inline fun <reified R> ensureNamedObject(any: Any?, transform: (Map<String, Any>, String) -> R, vararg path: String): Map<String, R> =
        ensureType<Map<String, Map<String, Any>>?>(any, *path)?.mapValues { (key, value) -> transform(value, key) }
                ?: mapOf()

inline fun <reified R> ensureObject(any: Any?, transform: (Map<String, Any>) -> R, default: R, vararg path: String): R =
        ensureType<Map<String, Any>?>(any, *path)?.let(transform) ?: default

inline fun <reified T> ensureType(any: Any?, vararg path: String): T {
    if (any !is T) {
        throw ValidationException("Incorrect type, expected ${T::class.simpleName} (location: ${path.joinToString("->")})")
    }
    return any
}

fun ensureBrid(any: Any?, vararg path: String): BlockchainRid {
    return when (any) {
        is String -> {
            require(any.length == 64) { throw ValidationException("Brid must be 64 characters, got $any of length ${any.length}") }
            BlockchainRid.buildFromHex(any)
        }

        is ByteArray -> BlockchainRid(any)
        else -> throw ValidationException("Incorrect type, expected ByteArray or hex string (location: ${path.joinToString("->")})")
    }
}