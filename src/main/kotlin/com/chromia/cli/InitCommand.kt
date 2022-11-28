package com.chromia.cli

import com.github.ajalt.clikt.core.CliktCommand
import java.io.File

class InitCommand : CliktCommand(help = "Generates a template project"){

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
        File("config.yml").writeText("""
            blockchains:
              hello:
                module: main
            compile:
              rellVersion: 0.10.10
        """.trimIndent())
    }
}
