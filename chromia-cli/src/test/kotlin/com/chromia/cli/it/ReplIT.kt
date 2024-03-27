package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import org.junit.jupiter.api.Test


class ReplIT {

    @Test
    fun enforceConsistentLocale() {
        TestProcess.Builder("repl", "-c", "\"title\".upper_case()")
                .env("JAVA_ARGS" to "-Duser.country=TR -Duser.language=tr")
                .verbose()
                .startCondition("\"TITLE\"")
                .start()
    }
}
