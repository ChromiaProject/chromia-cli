package com.chromia.build.tools.lib

import assertk.assertThat
import assertk.assertions.isTrue
import org.apache.commons.lang3.SystemUtils
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.exists

class GitRepositoryClonerIT {

    @Test
    fun clonePublicHttpsRepository(@TempDir testDir: Path) {
        val cloner = GitRepositoryCloner()
        cloner.clone("https://gitlab.com/chromaway/core-tools/chromia-images.git", testDir.toFile(), "dev")
        assertThat(testDir.resolve(".git").exists()).isTrue()
    }

    @Test
    fun clonePrivateRepositoryUsingNotEncryptedKey(@TempDir testDir: Path) {
        val sshDir = testDir.resolve("ssh_test").toFile()
        sshDir.mkdirs()
        val resourcesSshDirectory = getResourceFile("ssh_test")
        resourcesSshDirectory.copyRecursively(sshDir)
        File(sshDir, "config").writeText("""
            Host *
                IdentityFile ${sshDir.resolve("ssh_test").absolutePath}
                IdentitiesOnly yes
        """.trimIndent())

        val cloner = GitRepositoryCloner(sshDir)
        val cloneDir = testDir.resolve("cloned-project")
        Files.createDirectory(cloneDir)
        cloner.clone("git@gitlab.com:chromaway/core-tools/gitclonetestrepo.git", cloneDir.toFile(), "main")

        assertThat(cloneDir.resolve(".git").exists()).isTrue()
    }

    @Test
    fun clonePrivateRepositoryWithSshAgent(@TempDir testDir: Path) {
        Assumptions.assumeFalse(SystemUtils.IS_OS_WINDOWS)
        val sshDir = testDir.resolve("ssh_test").toFile()
        sshDir.mkdirs()
        val resourcesSshDirectory = getResourceFile("ssh_test")
        resourcesSshDirectory.copyRecursively(sshDir)
        sshDir.resolve("known_hosts").copyTo(sshDir.resolve("agent/known_hosts"))

        val keyFile = sshDir.resolve("ssh_test")
        val keyFilePermissions = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
        Files.setPosixFilePermissions(keyFile.toPath(), keyFilePermissions)

        val startSshAgentScript = sshDir.resolve("start-ssh-agent.sh")
        startSshAgentScript.setExecutable(true)
        Runtime.getRuntime().exec(startSshAgentScript.absolutePath + " " + keyFile.absolutePath).waitFor()

        val cloner = GitRepositoryCloner(sshDir = sshDir.resolve("agent"))
        val cloneDir = testDir.resolve("cloned-project")
        Files.createDirectory(cloneDir)
        cloner.clone("git@gitlab.com:chromaway/core-tools/gitclonetestrepo.git", cloneDir.toFile(), "main")

        assertThat(cloneDir.resolve(".git").exists()).isTrue()
    }

    private fun getResourceFile(resourceName: String): File {
        return File(this.javaClass.classLoader.getResource(resourceName)!!.file)
    }
}
