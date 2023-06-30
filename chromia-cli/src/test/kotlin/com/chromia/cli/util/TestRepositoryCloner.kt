package com.chromia.cli.util

import com.chromia.build.tools.lib.InstallDirTarget
import com.chromia.build.tools.lib.LibraryInstallException
import com.chromia.build.tools.lib.RepositoryCloner
import net.postchain.common.exception.UserMistake
import org.eclipse.jgit.api.errors.InvalidRemoteException
import java.io.File

class TestRepositoryCloner : RepositoryCloner {
    override fun clone(registry: String, target: File, tagOrBranch: String?) {
        when (registry) {
            "http://bar.com" -> createFile(target, "${InstallDirTarget.SOURCE.target}/d.rell")
            "http://foo.com" -> {
                createFile(target, "${InstallDirTarget.SOURCE.target}/a.rell")
                createFile(target, "${InstallDirTarget.SOURCE.target}/nested/b.rell")
                createFile(target, "not/include/c.rell")
            }

            "http://filter.com" -> {
                createFile(target, "${InstallDirTarget.SOURCE.target}/a.rell")
                createFile(target, "${InstallDirTarget.SOURCE.target}/a.yml")
                createFile(target, "${InstallDirTarget.SOURCE.target}/nested/b.rell")
                createFile(target, "${InstallDirTarget.SOURCE.target}/nested/b.yml")
            }

            "http://wrongAddress.com" -> throw LibraryInstallException("This is an error")
        }
    }

    fun createFile(dir: File, name: String) {
        with(File(dir, name)) {
            parentFile.mkdirs()
            writeText("""
                    module; 
                """.trimIndent())
        }
    }
}