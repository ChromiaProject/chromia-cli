package com.chromia.cli

import assertk.assertThat
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep

internal class StartCommandTest {

    @Test
    fun `Start and update a dapp`(@TempDir dir: Path) {
        with(File(dir.toFile(), "config.yml")) {
            writeText("""
                blockchains:
                  dummy:
                    module: main
                    
                database:
                  schema: startcommandtest
            """.trimIndent())
        }
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                object my_name {
                  mutable name= "World";
                 }

                operation set_name(name) {
                  my_name.name = name;
                }

                query hello() = "Hello %s!".format(my_name.name);
            """.trimIndent())
        }
        val testConsole = TestConsole()

        runBlocking {
            StartCommand().main(listOf("-s", "${dir.absolutePathString()}/config.yml", "--wipe"))
            sleep(5000)
            QueryCommand().context { console = testConsole }.parse(listOf("--api-url", "http://localhost:7740", "hello"))
            testConsole.assertContains("\"Hello World!\"\n")
            TxCommand().parse(listOf("--api-url", "http://localhost:7740", "set_name", "Alfred", "--await"))
            QueryCommand().context { console = testConsole }.parse(listOf("--api-url", "http://localhost:7740", "hello"))
            testConsole.assertContains("\"Hello Alfred!\"\n")

            println(testConsole.out)

            with(File(dir.toFile(), "src/main.rell")) {
                appendText("""
                query new_query() = "Goodbye"; 
            """.trimIndent())
            }


            println("Resetting")
            testConsole.reset()
            assert(testConsole.out.isEmpty())


            println("Starting new process")
            //StartCommand().main(listOf("-s", "${dir.absolutePathString()}/config.yml"))


            //sleep(5000)
            println("New query")
            QueryCommand().context { console = testConsole }.parse(listOf("--api-url", "http://localhost:7740", "hello"))
            println(testConsole.out)
            testConsole.assertContains("\"Hello Alfred!\"\n")
            QueryCommand().context { console = testConsole }.parse(listOf("--api-url", "http://localhost:7740", "new_query"))
            testConsole.assertContains("\"Goodbye\"\n")


        }
    }
}