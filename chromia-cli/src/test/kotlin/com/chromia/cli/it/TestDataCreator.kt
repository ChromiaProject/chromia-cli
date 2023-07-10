package com.chromia.cli.it

import java.io.File
import java.nio.file.Path

object TestDataCreator {
    fun basicApp(dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                query hello() = "Hi!";
                operation call_op(value: integer) {}
            """.trimIndent())
        }
        with(File(dir.toFile(), "config.yml")) {
            writeText("""
                blockchains:
                  hello:
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
                    url: "localhost:7740"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                    container: foo
                    chains:
                      deployed: x"0000000000000000000000000000000000000000000000000000000000000002"
                      wrongConfig: x"0000000000000000000000000000000000000000000000000000000000000003"
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"
                    bar:
                      registry: http://bar.com
                      path: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                    insecureBar:
                      registry: http://bar.com
                      path: lib
                      rid: x"11"
                      insecure: true

            """.trimIndent())
        }
        with(File(dir.toFile(), ".secret")) {
            writeText("""
            privkey = BBBDFE956021912512E14BB081B27A35A0EABC4098CB687E973C434006BCE114
            pubkey = 03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05
            """.trimIndent())
        }
    }
}