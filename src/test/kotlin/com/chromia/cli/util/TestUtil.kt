package com.chromia.cli.util

import java.io.File

val isWindows get() = File.separatorChar == '\\'

fun String.separatorsToSystem() : String {
    val replaceChar = if (isWindows) '/' else '\\'
    return this.replace(replaceChar, File.separatorChar)
}