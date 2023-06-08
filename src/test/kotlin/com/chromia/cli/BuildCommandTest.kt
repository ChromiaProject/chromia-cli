package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import com.chromia.cli.lib.LibraryMismatchException
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
    fun testBridOutput() {
        command.parse(listOf("--show-brid"))
        testConsole.assertContains("hello BA111FA5A710E923989A09E56A2F9029016A5E677B43467BB3FC267A55A31B13")
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
        TestRepositoryCloner().clone("http://bar.com", dir.resolve("build/libs/bar"))
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
        TestRepositoryCloner().clone("http://bar.com", dir.resolve("build/libs/bar"))
        val e = assertFailsWith<LibraryMismatchException> { command.parse() }
        assertThat(e.message!!).contains("The rid for library bar does not match the configured value.\n" +
                "Should be: 11\n" +
                "Was: 615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5\n" +
                "Do not blindly copy the calculated rid as it might be tampered with.")
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
