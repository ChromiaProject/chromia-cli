package com.chromia.cli.lib

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import java.io.File

class GitRepositoryCloner : RepositoryCloner {
    override fun clone(registry: String, target: File, tagOrBranch: String?) {

        Git.cloneRepository()
                .apply { if (tagOrBranch != null) setBranch(tagOrBranch) }
                .setDirectory(target)
                .setURI(registry)
                .setTimeout(60)
                .setProgressMonitor(TextProgressMonitor())
                .call()

    }
}