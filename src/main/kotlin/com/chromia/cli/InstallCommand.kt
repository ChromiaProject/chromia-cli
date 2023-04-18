package com.chromia.cli

import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory

interface RepositoryCloner {
    fun clone(registry: String, dir: File)
}

class BaseRepositoryCloner : RepositoryCloner {
    override fun clone(registry: String, dir: File) {
        Git.cloneRepository()
                .setDirectory(dir)
                .setURI(registry)
                .setTimeout(60)
                .setProgressMonitor(TextProgressMonitor())
                .call()
    }
}

class InstallCommand(
        private val cloner: () -> RepositoryCloner = { BaseRepositoryCloner() }
) : CliktCommand(help = "Install libs dependencies") {
    private val settings by settingsOption()
    private val target by option(help = "Optional parameter so set the download target")
            .defaultLazy { settings.target.absolutePath + "/lib/" }
    private val installAttributes get() = settings.blockchains.values.flatMap { it.libs.values }.distinct()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        installAttributes.forEach {
            try {
                println(it)
                val dir = createTempDirectory()
                //TODO get from the request so it is standardized
                val name = it.rid.toString()

                cloner().clone(it.registry, dir.toFile())

                val targetPath = Path(target, name)
                val resolvePath = dir.resolve(it.lib)
                copyDir(resolvePath, targetPath)
            } catch (e: Exception) {
                echo(e.message)
            }
        }
    }

    private fun copyDir(src: Path, dest: Path) {
        Files.walk(src).forEach {
            println(it)
            Files.copy(it, dest.resolve(src.relativize(it)),
                    StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
