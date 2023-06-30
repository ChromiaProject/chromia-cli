package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import com.chromia.build.tools.lib.InstallDirTarget
import com.chromia.cli.util.CommandExtension
import com.chromia.cli.util.TestConsole
import com.chromia.cli.util.TestRepositoryCloner
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.FileNotFound
import com.github.ajalt.clikt.core.context
import net.postchain.gtv.gtvml.GtvMLParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import kotlin.test.assertFailsWith

internal class BuildCommandTest {

    val testConsole = TestConsole()

    @RegisterExtension
    val command = CommandExtension(BuildCommand().context { console = testConsole })
    val dir get() = command.dir

    @Test
    fun testCanNotFindSettings() {
        val exception = assertFailsWith<FileNotFound> {
            BuildCommand().parse(listOf())
        }
        assertTrue(exception.message?.contains("config.yml not found") ?: false)
    }

    @Test
    fun testMultipleRun() {
        command.parse()
        command.parse()
        command.parse()

        assertEquals(File(dir, "build").list()?.size ?: 0, 1)
        assertTrue(File(dir, "build/hello.xml").exists())
    }

    @Test
    fun testSingleRun() {
        command.parse()

        assertEquals(File(dir, "build").list()?.size ?: 0, 1)
        assertTrue(File(dir, "build/hello.xml").exists())
    }

    @Test
    fun failsValidation() {
        File(dir, "config.yml").writeText("""
            blockchains:
              hello:
                module: main
                moduleArgs: 
                  main:
                    my_args: "wrong_type"
        """.trimIndent())
        File(dir, "src/main.rell").writeText("""
            module;
            struct module_args { my_args: integer; }
        """.trimIndent())
        val e = assertFailsWith<CliktError> { command.parse() }
        assertThat(e.message!!).contains("Bad module_args for module 'main': Decoding type 'integer': expected INTEGER, actual STRING")
    }

    @Test
    fun simpleLibraryExistTest() {
        File(dir, "config.yml").writeText("""
            blockchains:
              hello:
                module: main
            libs:
                bar:
                  registry: http://bar.com
                  path: lib
                  rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5" 
        """.trimIndent())
        TestRepositoryCloner().clone("http://bar.com", dir.resolve("src/lib/bar"), "")
        command.parse()
    }

    @Test
    fun libraryMissingTest() {
        File(dir, "config.yml").writeText("""
            blockchains:
              hello:
                module: main
            libs:
                missing:
                  registry: http://missing.com
                  path: lib
                  rid: x"11" 
        """.trimIndent())
        val e = assertFailsWith<CliktError> { command.parse() }
        assertThat(e.message!!).contains("Library missing is not installed, install before building")
    }

    @Test
    fun libraryTamperedTest() {
        File(dir, "config.yml").writeText("""
            blockchains:
              hello:
                module: main
            libs:
                bar:
                  registry: http://bar.com
                  path: lib
                  rid: x"11" 
        """.trimIndent())
        TestRepositoryCloner().clone("http://bar.com", dir.resolve("src/${InstallDirTarget.SOURCE.target}/bar"), "")
        assertFailsWith<IllegalArgumentException> { command.parse() }
        testConsole.assertContains("Should be: 11")
        testConsole.assertContains("Was: 615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5")
    }

    @Test
    fun whiteListedGtxModules() {
        File(dir, "config.yml").writeText("""
            blockchains:
              hello:
                module: main
                config:
                  gtx:
                    modules:
                      - "net.postchain.d1.anchoring.system.SystemAnchoringGTXModule"
                      - "net.postchain.d1.anchoring.cluster.ClusterAnchoringGTXModule"
                      - "net.postchain.d1.icmf.IcmfSenderGTXModule"
                      - "net.postchain.d1.icmf.IcmfReceiverGTXModule"
                      - "net.postchain.d1.iccf.IccfGTXModule"
        """.trimIndent())
        File(dir, "src/main.rell").writeText("""
            module;
        """.trimIndent())
        command.parse()
        val outputFile = File(dir, "build/hello.xml")
        assertTrue(outputFile.exists())
        val outputGtv = GtvMLParser.parseGtvML(outputFile.readText())
        assertThat(outputGtv["gtx"]?.get("modules")?.asArray()?.map { it.asString() }!!).containsAll(
                "net.postchain.d1.anchoring.system.SystemAnchoringGTXModule",
                "net.postchain.d1.anchoring.cluster.ClusterAnchoringGTXModule",
                "net.postchain.d1.icmf.IcmfSenderGTXModule",
                "net.postchain.d1.icmf.IcmfReceiverGTXModule",
                "net.postchain.d1.iccf.IccfGTXModule"
        )
    }
}
