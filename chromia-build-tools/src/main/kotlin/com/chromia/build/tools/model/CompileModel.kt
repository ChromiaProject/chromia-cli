package com.chromia.cli.model

import com.chromia.build.tools.model.ensureType
import java.nio.file.Path
import net.postchain.rell.base.model.R_LangVersion

data class CompileModel(
        val rellVersion: String = DefaultChromiaModelRellVersion,
        val source: Path,
        val target: Path,
        private val deprecatedError: Boolean = false,
        val quiet: Boolean = true,
        val strictGtvConversion: Boolean = true,
        val root: Path,
) {
    val libFolder: Path = source.resolve("lib")
    val langVersion get() = R_LangVersion.of(rellVersion)

    companion object {
        fun load(data: Map<String, Any>, dir: Path) = CompileModel(
                rellVersion = ensureType<String?>(data["rellVersion"], "compile", "rellVersion") ?: DefaultChromiaModelRellVersion,
                source = dir.resolve(ensureType<String?>(data["source"], "compile", "source") ?: "src"),
                target = dir.resolve(ensureType<String?>(data["target"], "compile", "target") ?: "build"),
                deprecatedError = ensureType<Boolean?>(data["deprecatedError"], "compile", "deprecatedError")
                        ?: false,
                quiet = ensureType<Boolean?>(data["quiet"], "compile", "quiet") ?: false,
                strictGtvConversion = ensureType<Boolean?>(data["strictGtvConversion"], "compile", "strictGtvConversion")
                        ?: true,
                dir,
        )
        fun default(dir: Path) = load(mapOf(), dir)
    }
}
