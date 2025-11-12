package com.chromia.cli.command.library.management

import com.chromia.api.ChromiaLibrariesApi
import com.chromia.api.filterLibraries
import com.chromia.build.tools.lib.GitRepositoryCloner
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.build.tools.util.ifNotEmpty
import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.tools.formatter.info
import com.chromia.cli.util.BuildCliEnv
import com.chromia.cli.util.DependencyUpdatedMarker
import com.chromia.cli.util.libraryOption
import com.chromia.library.chain.versioning.external.getLatestLibraryVersion
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.collections.emptyList

class InstallLibraryCommand(
    val repositoryClonerFactory: (quiet: Boolean) -> RepositoryCloner = { GitRepositoryCloner(quiet = it) }
) : AbstractLibraryCommand(
    name = "install",
    help = "Install library dependencies",
    hideKeyPairSourceHelpMessage = true
) {

    private val libsToInclude by libraryOption()
        .multiple(emptyList(), required = false)
        .validate {
            require(settings.model?.libs?.keys?.containsAll(it) == true) {
                "Specified library(ies) $it does not exist in config file"
            }
        }
    private val explicitLibraryId by argument(
        name = "library-id",
        help = "ID of the library to install with optional version, e.g. 'chromia-lib@1.0.0'. " +
            "if no version is specified the latest version will be installed"
    ).optional()

    private val force by option(
        "-f",
        "--force",
        help = "Force installation even if RID verification fails. This bypasses integrity checks " +
            "and should only be used if you trust the source. Use with caution as it may install " +
            "corrupted or tampered libraries."
    ).flag(default = false)

    override fun run() = runCatching {
        requireNotNull(settings.model) {
            "Project settings file not found"
        }

        var filteredModel = settings.model?.filterLibraries(libsToInclude.takeIf { it.isNotEmpty() })

        explicitLibraryId?.let {
            val (libraryId, version) = extractLibraryIdAndVersion(it)
            filteredModel = filteredModel?.copy(
                libs = mapOf(libraryId to RellLibraryModel(remoteTarget.url, version = version))
            )
            ChromiaLibrariesApi.install(
                BuildCliEnv(this),
                filteredModel!!,
                repositoryClonerFactory(true),
                force
            )

            // todo: Evaluate if we are able to write directly to yaml file
            echo(
                buildString {
                    appendLine("add this into chromia.yaml file under libs :")
                    appendLine(libraryId)
                    appendLine("\tversion: $version")
                    remoteTarget.url?.let { appendLine("\tregistry: $it") }
                    remoteTarget.brid?.let { appendLine("\tbrid: $it") }
                }
            )
            updateDependencyMarker()
            return@runCatching
        }

        runBlocking {
            coroutineScope {
                ChromiaLibrariesApi.install(
                    BuildCliEnv(this@InstallLibraryCommand),
                    filteredModel!!,
                    repositoryClonerFactory(true),
                    force
                )
            }
        }

        filteredModel?.libs?.ifNotEmpty {
            updateDependencyMarker()
        }
    }.fold(
        onSuccess = { echo(info("Dependencies installed successfully")) },
        onFailure = { echo("""
            Failed to install dependencies: 
            ${it.message}
        """.trimIndent(), err = true) }
    )

    private fun updateDependencyMarker() {
        settings.model?.compile?.target?.let {
            DependencyUpdatedMarker(it.toFile()).markAsInstalled()
        }
    }

    private fun extractLibraryIdAndVersion(libraryIdWithVersion: String): Pair<String, String?> {
        val delimiter = "@"
        return if (libraryIdWithVersion.contains(delimiter)) {
            libraryIdWithVersion.substringBeforeLast(delimiter) to libraryIdWithVersion.substringAfterLast(delimiter)
        } else {
            val latestVersion = client.getLatestLibraryVersion(libraryIdWithVersion)
                ?.version
                ?: throw CliktError("Library $libraryIdWithVersion not found")
            libraryIdWithVersion to latestVersion
        }
    }
}
