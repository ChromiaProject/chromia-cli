package com.chromia.build.tools.lib

import com.chromia.cli.model.RellLibraryModel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import net.postchain.rell.api.base.RellCliEnv

class LibraryInstaller(
        private val repositoryCloner: RepositoryCloner,
        private val env: RellCliEnv,
        sourceDir: Path,
        tempDir: Path,
) {

    private val libRoot: Path = sourceDir.resolve("lib")
    private val tmpLibRoot: Path = tempDir.resolve(".tmp/lib")
    private val libraryVerifyer = LibraryVerifyer(env, libRoot)

    fun installLibs(libs: Map<String, RellLibraryModel>) =
            libs.forEach { (libName, libModel) -> installLibrary(libName, libModel) }

    private fun installLibrary(name: String, model: RellLibraryModel) {
        val installDir = libRoot.resolve(name)
        if (installDir.exists()) {
            if (libraryVerifyer.verifyLib(name, model, true)) return
            env.print("Library $name not up to date, reinstalling")
            installDir.toFile().deleteRecursively()
        }
        cloneRepository(name, model, installDir)
        if (!libraryVerifyer.verifyLib(name, model)) {
            installDir.toFile().deleteRecursively()
            throw LibraryInstallException("Failed to install lib $name")
        }
    }

    private fun cloneRepository(name: String, model: RellLibraryModel, installDir: Path) {
        val tmpInstallDir = tmpLibRoot.resolve(name)
        if (tmpInstallDir.exists()) tmpInstallDir.toFile().deleteRecursively()
        repositoryCloner.clone(model.registry, tmpInstallDir, model.tagOrBranch)
        copyRellFilesInFolder(tmpInstallDir.resolve(model.path), installDir)
        tmpInstallDir.toFile().deleteRecursively()
    }

    private fun copyRellFilesInFolder(src: Path, dest: Path) {
        if (!dest.exists()) dest.toFile().mkdirs() else dest.toFile().deleteRecursively()
        Files.walk(src).filter { it.isDirectory() || it.extension == "rell" }.forEach {
            Files.copy(it, dest.resolve(src.relativize(it)), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
