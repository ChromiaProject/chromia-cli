package com.chromia.cli.util

import java.io.File
import java.nio.file.Path

object DeploymentTestDataCreator {

    val keyIdName = "keyIdUsedForTesting"

    fun unitTestApp(dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                import a_file.*;
                query hello() = "Hi!";
                operation call_op(value: integer) {}
            """.trimIndent())
        }
        with(File(dir.toFile(), "src/a_file.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                query hello_a() = "Hi!";
                operation call_op_a(value: integer) {}
            """.trimIndent())
        }
        with(File(dir.toFile(), "chromia.yml")) {
            writeText("""
                blockchains:
                  my_rell_dapp:
                    module: main
                    config:
                      blockstrategy:
                        maxblocktime: 1000
                  deployed:
                    module: main      
                  wrongConfig: 
                    module: main
                    moduleArgs:
                      main:
                        name: { nameIsInterprededAsDict }
                deployments:
                  test:
                    url: "https://localhost:7740"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                    container: foo
                    chains:
                      deployed: x"0000000000000000000000000000000000000000000000000000000000000002"
                      wrongConfig: x"0000000000000000000000000000000000000000000000000000000000000003"
            """.trimIndent())
        }
        with(File(dir.toFile(), ".secret")) {
            writeText("""
            privkey = BBBDFE956021912512E14BB081B27A35A0EABC4098CB687E973C434006BCE114
            pubkey = 03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05
            """.trimIndent())
        }

        with(File(dir.toFile(), "config")) {
            writeText("""
            keyId = $keyIdName
            """.trimIndent())
        }
    }
}
