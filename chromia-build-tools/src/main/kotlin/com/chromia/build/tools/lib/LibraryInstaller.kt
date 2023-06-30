package com.chromia.build.tools.lib

import com.chromia.cli.model.RellLibraryModel
import net.postchain.rell.api.base.RellCliEnv
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

class LibraryInstaller(
        private val repositoryCloner: RepositoryCloner,
        private val env: RellCliEnv,
        private val installationTarget: Path,
        private val tempDir: Path
) {

    private val libraryVerifyer = LibraryVerifyer(env)
    fun installLibrary(name: String, model: RellLibraryModel) {
        val installDir = installationTarget.resolve(name)
        if (installDir.exists()) {
            if (libraryVerifyer.verifyLib(model, name, readFiles(installDir), true)) return
            env.print("Library $name not up to date, reinstalling")
        }
        val tmpInstallDir = tempDir.resolve(name)
        if (tmpInstallDir.exists()) tmpInstallDir.toFile().deleteRecursively()
        repositoryCloner.clone(model.registry, tmpInstallDir.toFile(), model.tagOrBranch)
        val files = readFiles(tmpInstallDir.resolve(model.path))
        val isLibraryOk = libraryVerifyer.verifyLib(model, name, files)
        if (isLibraryOk) {
            copyDir(files, tmpInstallDir.resolve(model.path), installDir)
            tmpInstallDir.toFile().deleteRecursively()
        } else {
            tmpInstallDir.toFile().deleteRecursively()
            throw LibraryInstallException("Failed to install lib $name")
        }
    }

    private fun readFiles(src: Path): List<File> {
        val returnList: MutableList<File> = mutableListOf()
        Files.walk(src).forEach {
            returnList.add(it.toFile())
        }
        return returnList
    }

    private fun copyDir(files: List<File>, src: Path, dest: Path) {
        if (!dest.exists()) dest.toFile().mkdirs() else dest.toFile().deleteRecursively()
        files.map { it.toPath() }.forEach {
            Files.copy(it, dest.resolve(src.relativize(it)),
                    StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
