package com.chromia.cli.model

import com.chromia.build.tools.model.ensureType
import java.io.File
import net.postchain.rell.base.model.R_LangVersion

data class CompileModel(
        val rellVersion: String = RellVersion,
        private val source: String = "src",
        private val target: String = "build",
        private val deprecatedError: Boolean = false,
        val quiet: Boolean = true
) {
    val langVersion get() = R_LangVersion.of(rellVersion)
    fun sourceFile(parent: File) = File(parent, source)
    fun targetFile(parent: File) = File(parent, target)

    companion object {
        fun load(data: Map<String, Any>) = CompileModel(
                rellVersion = ensureType<String?>(data["rellVersion"], "compile", "rellVersion") ?: RellVersion,
                source = ensureType<String?>(data["source"], "compile", "source") ?: "src",
                target = ensureType<String?>(data["target"], "compile", "target") ?: "build",
                deprecatedError = ensureType<Boolean?>(data["deprecatedError"], "compile", "deprecatedError")
                        ?: false,
                quiet = ensureType<Boolean?>(data["quiet"], "compile", "quiet") ?: false
        )
    }
}
