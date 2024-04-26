package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.testData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Duration


class ReplIT {

    @Test
    fun enforceConsistentLocale() {
        TestProcess.Builder("repl", "-c", "\"title\".upper_case()")
                .env("JAVA_ARGS" to "-Duser.country=TR -Duser.language=tr")
                .verbose()
                .startCondition("\"TITLE\"")
                .start()
    }

    @Test
    fun canStartReplSession() {
        TestProcess.Builder("repl")
                .awaitCompletion(false)
                .verbose()
                .startCondition("Rell")
                .start { process ->
                    process.waitUntil("Type '\\q' to quit or '\\?' for help.", Duration.ofSeconds(2))
                    process.close()
                }
    }

    @Test
    fun canStartReplWithQuery(@TempDir dir: Path) {
        testData(dir)
        TestProcess.Builder("repl", "--module", "main", "-c", "hello()")
                .setConfig(dir.resolve("chromia.yml").toFile())
                .startCondition("Hi!")
                .start()
    }

    @Test
    fun canStartReplWithOperation(@TempDir dir: Path) {
        testData(dir) {
            addFile("main.rell",
                    """
                    module;
                    
                    object repl_my_name {
                      mutable name= "World";
                     }
                    
                    operation repl_set_name(name) {
                      repl_my_name.name = name;
                    }
                    
                    query repl_hello_world() = "Hello %s!".format(repl_my_name.name);
            """.trimIndent())
        }
        TestProcess.Builder("repl", "--use-db", "--module", "main", "-c", "rell.test.tx(repl_set_name(\"foo\")).run()")
                .awaitCompletion(true)
                .setConfig(dir.resolve("chromia.yml").toFile())
                .verbose()
                .startCondition("INFO")
                .start()

        TestProcess.Builder("repl", "--use-db", "--module", "main", "-c", "repl_hello_world()")
                .awaitCompletion(false)
                .setConfig(dir.resolve("chromia.yml").toFile())
                .verbose()
                .startCondition("Hello foo!")
                .start()
    }
}
