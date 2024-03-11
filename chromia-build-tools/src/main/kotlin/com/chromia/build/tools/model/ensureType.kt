package com.chromia.build.tools.model

import com.chromia.build.tools.compile.ValidationException

inline fun <reified T> ensureType(any: Any?, vararg path: String): T {
    if (any !is T) {
        throw ValidationException("Incorrect type, expected ${T::class.simpleName} (location: ${path.joinToString("->")})")
    }
    return any
}