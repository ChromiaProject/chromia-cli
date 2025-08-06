package com.chromia.cli.util

inline fun <T> safe(code: () -> T): T? {
    return try {
        code()
    } catch (_: Exception) {
        null
    }
}
