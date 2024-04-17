package com.chromia.api.impl

import com.chromia.build.tools.lib.InstallDirTarget
import com.chromia.build.tools.lib.LibraryInstaller
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.model.ChromiaModel
import net.postchain.rell.api.base.RellCliEnv
import java.nio.file.Path


fun install(cliEnv: RellCliEnv, repositoryCloner: RepositoryCloner, projectDir: Path, model: ChromiaModel) {
    val libraryInstaller = LibraryInstaller(repositoryCloner, cliEnv,
            model.compile.sourceFile(projectDir.toFile()).toPath().resolve(InstallDirTarget.SOURCE.target),
            model.compile.targetFile(projectDir.toFile()).toPath().resolve(InstallDirTarget.TEMP.target)
    )
    model.libs.forEach { libraryInstaller.installLibrary(it.key, it.value) }
}