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
            """.trimIndent())
        }
    }

    fun unitTestApp(dir: Path) {
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
                  gtxConfig:
                    module: main
                    config:
                      gtx:
                        modules:
                          - "net.postchain.d1.anchoring.system.SystemAnchoringGTXModule"
                          - "net.postchain.d1.anchoring.cluster.ClusterAnchoringGTXModule"
                          - "net.postchain.d1.icmf.IcmfSenderGTXModule"
                          - "net.postchain.d1.icmf.IcmfReceiverGTXModule"
                          - "net.postchain.d1.iccf.IccfGTXModule"
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
    }
}