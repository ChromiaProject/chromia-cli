package com.chromia.cli.model

import java.io.File
import net.postchain.rell.base.model.R_LangVersion

data class CompileModel(
        val rellVersion: String = RellVersion,
        private val source: String = "src",
        private val target: String = "build",
        private val deprecatedError: Boolean = false,
        val library: Boolean = false,
        val quiet: Boolean = true
) {
    val langVersion get() = R_LangVersion.of(rellVersion)
    fun sourceFile(parent: File) = File(parent, source)
    fun targetFile(parent: File) = File(parent, target)
}
