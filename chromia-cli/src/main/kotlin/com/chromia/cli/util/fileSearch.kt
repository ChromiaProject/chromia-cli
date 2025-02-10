package com.chromia.cli.util

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.PathMatcher

fun matches(fileUri: URI, globMatchers: List<PathMatcher>): Boolean {
    if (globMatchers.isEmpty()) return true
    return globMatchers.any {
        it.matches(FileSystems.getDefault().getPath(fileUri.path))
    }
}