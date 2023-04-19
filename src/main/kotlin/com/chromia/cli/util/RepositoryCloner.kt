package com.chromia.cli.util

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import java.io.File

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