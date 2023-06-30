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
            """.trimIndent())
        }
        with(File(dir.toFile(), "config.yml")) {
            writeText("""
                blockchains:
                  hello:
                    module: main
            """.trimIndent())
        }
    }
}