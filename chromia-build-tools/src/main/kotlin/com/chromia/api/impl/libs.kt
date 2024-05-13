package com.chromia.api.impl

import com.chromia.build.tools.lib.LibraryInstaller
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.model.ChromiaModel
import net.postchain.rell.api.base.RellCliEnv


fun install(cliEnv: RellCliEnv, repositoryCloner: RepositoryCloner, model: ChromiaModel) {
    LibraryInstaller(repositoryCloner, cliEnv, model.compile.source, model.compile.target).installLibs(model.libs)
}