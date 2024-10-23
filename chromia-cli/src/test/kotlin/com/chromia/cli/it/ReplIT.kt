package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.testData
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

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
    fun canRunNoninteractiveReplSession() {
        TestProcess.Builder("repl")
                .input("5+5\n5*5\n")
                .wholeOutput("10\n25")
                .start()
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
            addSourceFile("main.rell",
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
