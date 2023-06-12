package com.chromia.cli.lib

import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import java.io.File

class GitRepositoryCloner : RepositoryCloner {
    override fun clone(registry: String, target: File, branch: String) {

        val cloner: CloneCommand = if (branch == "") {
            Git.cloneRepository()
        } else {
            Git.cloneRepository()
                    .setBranch(branch)
        }

        cloner.setDirectory(target)
                .setURI(registry)
                .setTimeout(60)
                .setProgressMonitor(TextProgressMonitor())
                .call()
    }
}