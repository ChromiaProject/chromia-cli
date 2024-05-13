package com.chromia.build.tools.lib

import java.io.File
import java.nio.file.Path
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.internal.transport.sshd.agent.connector.Factory
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import org.eclipse.jgit.util.FS

class GitRepositoryCloner(val sshDir: File? = null, val quiet: Boolean = false) : RepositoryCloner {

    override fun clone(registry: String, target: Path, tagOrBranch: String?) {
        try {
            val sshdSessionFactory = createSshdSessionFactory()
            SshSessionFactory.setInstance(sshdSessionFactory)
            Git.cloneRepository()
                    .apply { if (tagOrBranch != null) setBranch(tagOrBranch) }
                    .setDirectory(target.toFile())
                    .setURI(registry)
                    .setTimeout(60)
                    .apply { if (!quiet) setProgressMonitor(TextProgressMonitor()) }
                    .call()
        } catch (e: InvalidRemoteException) {
            target.toFile().deleteRecursively()
            throw LibraryInstallException(e.message!!)
        }
    }

    private fun createSshdSessionFactory() = SshdSessionFactoryBuilder()
            .setConnectorFactory(Factory())
            .apply {
                if (sshDir != null) {
                    setSshDirectory(sshDir)
                } else {
                    setSshDirectory(File(FS.DETECTED.userHome(), ".ssh"))
                }
            }
            .setHomeDirectory(FS.DETECTED.userHome())
            .build(null)
}
