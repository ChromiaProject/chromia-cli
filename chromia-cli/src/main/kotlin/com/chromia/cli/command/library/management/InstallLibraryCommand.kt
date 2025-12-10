package com.chromia.cli.command.library.management

import com.chromia.api.ChromiaLibrariesApi
import com.chromia.api.filterLibraries
import com.chromia.build.tools.lib.GitRepositoryCloner
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.tools.formatter.info
import com.chromia.cli.util.BuildCliEnv
import com.chromia.cli.util.DependencyUpdatedMarker
import com.chromia.cli.util.libraryOption
import com.chromia.cli.util.updateChromiaYamlForLibrary
import com.chromia.library.chain.versioning.external.getLatestLibraryVersion
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import java.io.File
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
                "Specified library(ies) $it not found in ${settings.modelFilePath}"
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
            "Project settings file not found at ${settings.modelFilePath}"
        }

        if (explicitLibraryId != null) {
            installByLibraryId(explicitLibraryId!!)
        } else {
            installFromLibraryModel()
        }

    }.fold(
        onSuccess = { echo(info("Dependencies installed successfully")) },
        onFailure = { echo("""
            Failed to install dependencies: 
            ${it.message}
        """.trimIndent(), err = true) }
    )

    private fun installByLibraryId(libraryIdentifier: String) {
        val (libraryId, version) = extractLibraryIdAndVersion(libraryIdentifier)
        val chromiaModuleWithExplicitLib = settings.model!!.copy(
                libs = mapOf(libraryId to RellLibraryModel(
                        registry = remoteTarget.url,
                        brid = remoteTarget.brid,
                        version = version,
                ))
        )
        installLibraryModules(chromiaModuleWithExplicitLib)

        // note: this preserves the original YAML structure except for indentation,
        updateChromiaYamlForLibrary(File(settings.modelFilePath!!), libraryName = libraryId, version!!)
        updateDependencyMarker()
    }

    private fun installFromLibraryModel() {
        val filteredModel = settings.model!!.filterLibraries(libsToInclude.takeIf { it.isNotEmpty() })
        require(filteredModel.libs.isNotEmpty()) {"No libraries found in: ${settings.modelFilePath}"}

        val modelsWithExplicitTarget = filteredModel.copy(
                libs = filteredModel.libs.map {
                    val libmodel = it.value
                    val insecure = if (force) { true } else { libmodel.insecure}
                    val modelWithExplicitTarget = RellLibraryModel(
                            registry = remoteTarget.url ?: libmodel.registry,
                            brid = remoteTarget.brid ?: libmodel.brid,
                            tagOrBranch = libmodel.tagOrBranch,
                            path = libmodel.path,
                            insecure = insecure,
                            rid = libmodel.rid,
                            version = libmodel.version
                    )
                    it.key to modelWithExplicitTarget
                }.toMap()
        )

        installLibraryModules(modelsWithExplicitTarget)
        updateDependencyMarker()
    }

    private fun installLibraryModules(model: ChromiaModel) {
        ChromiaLibrariesApi.install(
                BuildCliEnv(this@InstallLibraryCommand),
                model,
                repositoryClonerFactory(true),
                force
        )
    }

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
