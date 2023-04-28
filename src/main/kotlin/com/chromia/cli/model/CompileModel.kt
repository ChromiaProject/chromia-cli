package com.chromia.cli.model

import net.postchain.rell.model.R_LangVersion

data class CompileModel(
        val rellVersion: String = "0.12.0",
        val source: String = "src",
        val target: String = "build",
        private val deprecatedError: Boolean = false,
        val quiet: Boolean = true
) {
    val langVersion get() = R_LangVersion.of(rellVersion)
}
