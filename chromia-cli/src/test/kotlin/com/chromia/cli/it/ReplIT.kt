package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import org.junit.jupiter.api.Test
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
}
