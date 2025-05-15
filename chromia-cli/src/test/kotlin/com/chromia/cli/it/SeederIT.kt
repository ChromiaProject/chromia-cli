package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.testData
import com.chromia.cli.command.node.INITILIZED_LOG
import com.chromia.cli.command.seeder.SeederGenerateCommand
import com.chromia.cli.command.seeder.SeederInitConfigCommand
import com.github.ajalt.clikt.core.parse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Duration

class SeederIT {
    @Test
    fun `seed operation is executed successfully for all supported entity types`(@TempDir tempDir: Path) {
        setupWorkspace(tempDir)

        SeederInitConfigCommand().parse(listOf("--settings", tempDir.resolve("chromia.yml").toString()))
        SeederGenerateCommand().parse(listOf("--settings", tempDir.resolve("chromia.yml").toString()))

        // Re-configure to use generated seeder module as entry point of blockchain
        tempDir.resolve("chromia.yml").toAbsolutePath().toFile().writeText(
                """
                blockchains:
                  hello:
                    module: seeder.seed_hello
                    config:
                      blockstrategy:
                        maxblocktime: 1000
                    config:
                      blockstrategy:
                        maxblocktime: 1000
                """.trimIndent()
        )

        TestProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .setConfig(tempDir.resolve("chromia.yml").toFile())
                .start { process ->
                    TestProcess.Builder("tx", "--await", "hello.seed_data")
                            .setWorkingDir(tempDir.toFile())
                            .start { process ->
                                process.waitUntil("was posted and confirmed", Duration.ofSeconds(5))
                            }
                }
    }

    @Test
    fun `seed operation is executed successfully for all supported entity types with predefined values`(@TempDir tempDir: Path) {
        setupWorkspace(tempDir)

        SeederInitConfigCommand().parse(listOf("--settings", tempDir.resolve("chromia.yml").toString()))
        overrideDefaultConfigWithPredefinedValues(tempDir)

        SeederGenerateCommand().parse(listOf("--settings", tempDir.resolve("chromia.yml").toString()))

        // Re-configure to use generated seeder module as entry point of blockchain
        tempDir.resolve("chromia.yml").toAbsolutePath().toFile().writeText(
                """
                blockchains:
                  hello:
                    module: seeder.seed_hello
                    config:
                      blockstrategy:
                        maxblocktime: 1000
                    config:
                      blockstrategy:
                        maxblocktime: 1000
                """.trimIndent()
        )

        TestProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .setConfig(tempDir.resolve("chromia.yml").toFile())
                .start { process ->
                    TestProcess.Builder("tx", "--await", "hello.seed_data")
                            .setWorkingDir(tempDir.toFile())
                            .start { process ->
                                process.waitUntil("was posted and confirmed", Duration.ofSeconds(5))
                            }
                }
    }

    private fun setupWorkspace(tempDir: Path) {
        testData(tempDir) {
            addSourceFile("main.rell",
                    """
                        module;

                        entity bar {
                            name;
                        }
                        
                        enum my_enum {
                            A,
                            B,
                            C
                        }
                        
                        entity foo {
                            name;
                            last_name: text;
                            age: integer;
                            bar;
                            my_enum;
                            salary: decimal;
                            active: boolean;
                            meta: json;
                            pubkey;
                            tuid;
                            image: byte_array;
                            some_big_number: big_integer;
                            some_row_id: rowid;
                            time: timestamp;
                        }
                    """.trimIndent())
            secret()
        }
    }

    private fun overrideDefaultConfigWithPredefinedValues(tempDir: Path) {
        val moduleConfig = tempDir.resolve(".chromia/seeder/hello/modules/main.yml").toFile()
        moduleConfig.writeText("""
            module: main

            bar:
              count: 10
              attributes:
                name:
                  generator: text
            
            foo:
              count: 10
              attributes:
                name:
                  generator: predefined
                  values: ["a", "name", "value"]
                last_name:
                  generator: predefined
                  values: ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"]
                age:
                  generator: predefined
                  values: [1, 2, 3, 4, 5]
                  min: 0
                  max: 1000
                my_enum:
                  generator: predefined
                  values: ["A"]
                salary:
                  generator: predefined
                  values: [1.0, 1.12312321313123131231231231231231231231231233123121231, 1.33123123123]
                active:
                  generator: predefined
                  values: [true]
                meta:
                  generator: predefined
                  values: [["a", 1], ['b': 2], ["c", ["cc", 1]], ['c', ['cc', 1]], {"d": 1}, {'e': 1}, {"f": {"ff": 1}}, {'g': {'gg': 1}} ]
                pubkey:
                  generator: predefined
                  values: ["02C897E0CC66B78383C85AB57989CD96F4499C2FF632F02EB9E5AAD8C329D05B6C", "02C897E0CC66B78383C85AB57989CD96F4499C2FF632F02EB9E5AAD8C329D05B6C"]
                tuid:
                  generator: predefined
                  values: ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"]
                image:
                  generator: predefined
                  values: ["02C897E0CC66B78383C85AB57989CD96F4499C2FF632F02EB9E5AAD8C329D05B6C", "02C897E0CC66B78383C85AB57989CD96F4499C2FF632F02EB9E5AAD8C329D05B6C"]
                some_big_number:
                  generator: predefined
                  values: [100000000000000000000000000000000000000000000000000000000000000000000000000000, 1]
                some_row_id:
                  generator: predefined
                  values: [1, 2, 3, 4, 5]
                time:
                  generator: predefined
                  values: [1, 2, 3, 4, 5]
        """.trimIndent())
    }
}
