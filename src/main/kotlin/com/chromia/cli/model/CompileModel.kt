package com.chromia.cli.model

import net.postchain.rell.base.model.R_LangVersion

data class CompileModel(
        val rellVersion: String = RellVersion,
        val source: String = "src",
        val target: String = "build",
        private val deprecatedError: Boolean = false,
        val quiet: Boolean = true
) {
    val langVersion get() = R_LangVersion.of(rellVersion)
}
