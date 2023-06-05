package com.chromia.cli.util

import com.chromia.cli.interfaces.RepositoryCloner
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import java.io.File

class GitRepositoryCloner : RepositoryCloner {
    override fun clone(registry: String, target: File) {
        Git.cloneRepository()
                .setDirectory(target)
                .setURI(registry)
                .setTimeout(60)
                .setProgressMonitor(TextProgressMonitor())
                .call()
    }
}