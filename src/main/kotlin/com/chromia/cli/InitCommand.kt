package com.chromia.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import java.io.File
import java.util.*

class InitCommand : CliktCommand(help = "Generates a template project") {
    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {

        val sourceDir = File("src")
        sourceDir.mkdir()
        File(sourceDir, "main.rell").writeText(
                """
                    module;
                    
                    object my_name {
                      mutable name= "World";
                     }
                     
                    operation set_name(name) {
                      my_name.name = name;
                    }
                    
                    query hello_world() = "Hello %s!".format(my_name.name);
                """.trimIndent()
        )
        val testDir = File("src/test")
        testDir.mkdir()
        File(testDir, "arithmetic_test.rell").writeText(
                """
                    @test module;
                    
                    function test_foo() {
                        assert_equals(2 + 2, 4);
                    }
                    
                    function test_bar() {
                        assert_not_equals(2 + 2, 5);
                    }
                """.trimIndent()
        )
        File(testDir, "data_test.rell").writeText(
                """
                @test module;
                import ^^.main.{my_name, set_name};
                
                function test_add_name() {
                    assert_equals(my_name.name, "World");
                
                    val tx = rell.test.tx(set_name("Bob"));
                    assert_equals(my_name.name, "World");
                
                    tx.run();
                    assert_equals(my_name.name, "Bob");
                }
                """.trimIndent()
        )

        val configFileContent = """
                    blockchains:
                      hello:
                        module: main
                    compile:
                      rellVersion: 0.10.10
                    test:
                      modules:
                        - test.arithmetic_test
                        - test.data_test
        """.trimIndent()

        if (File("config.yml").exists()) {
            println("A config.yml file exists in the working directory. Would you like to write over it? Y / N")
            val resp = Scanner(System.`in`).nextLine().equals("Y", true)
            if (resp) {
                File("config.yml").writeText(configFileContent)
            } else {
                println("Skipped creation on config.yml file")
            }
        } else {
            File("config.yml").writeText(configFileContent)
        }
    }
}
