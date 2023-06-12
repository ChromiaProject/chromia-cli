package com.chromia.cli.util

import com.chromia.cli.lib.RepositoryCloner
import org.eclipse.jgit.api.errors.InvalidRemoteException
import java.io.File

class TestRepositoryCloner : RepositoryCloner {
    override fun clone(registry: String, target: File, branch: String) {
        when (registry) {
            "http://bar.com" -> createFile(target, "lib/d.rell")
            "http://foo.com" -> {
                createFile(target, "lib/a.rell")
                createFile(target, "lib/nested/b.rell")
                createFile(target, "not/include/c.rell")
            }

            "http://filter.com" -> {
                createFile(target, "lib/a.rell")
                createFile(target, "lib/a.yml")
                createFile(target, "lib/nested/b.rell")
                createFile(target, "lib/nested/b.yml")
            }

            "http://wrongAddress.com" -> throw InvalidRemoteException("This is an error")
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