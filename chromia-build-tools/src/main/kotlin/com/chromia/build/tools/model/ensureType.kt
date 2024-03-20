package com.chromia.build.tools.model

import com.chromia.build.tools.compile.ValidationException

inline fun <reified R> ensureNamedObject(any: Any?, transform: (Map<String, Any>, String) -> R, vararg path: String): Map<String, R> =
        ensureType<Map<String, Map<String, Any>>?>(any, *path)?.mapValues { (key, value) -> transform(value, key) } ?: mapOf()
inline fun <reified R> ensureObject(any: Any?, transform: (Map<String, Any>) -> R, default: R, vararg path: String): R =
        ensureType<Map<String, Any>?>(any, *path)?.let(transform) ?: default
inline fun <reified T> ensureType(any: Any?, vararg path: String): T {
    if (any !is T) {
        throw ValidationException("Incorrect type, expected ${T::class.simpleName} (location: ${path.joinToString("->")})")
    }
    return any
}