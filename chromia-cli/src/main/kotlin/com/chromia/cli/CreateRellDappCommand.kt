package com.chromia.cli

import com.chromia.cli.model.RellVersion
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.util.Scanner

class CreateRellDappCommand : CliktCommand(name = "create-rell-dapp", help = "Generates a template project") {
    private val name by argument(help = "Dapp name").default("hello")

    private val baseDir by option("-d", "--base-dir", help = "Directory to generate template project in")
            .file(canBeFile = false)
            .default(File(System.getProperty("user.dir")))

    override fun run() {

        if (!baseDir.exists()) baseDir.mkdirs()
        if (File(baseDir, "chromia.yml").exists()) {
            echo("A chromia.yml file exists in the working directory. Would you like to write over it? (All file content will be lost) Type DELETE to proceed with the deletion")
            val resp = Scanner(System.`in`).nextLine().equals("DELETE", false)
            if (resp) {
                File(baseDir, "chromia.yml").delete()
            } else {
                return
            }
        }
        File(baseDir, "chromia.yml").writeText(
                this::class.java.getResource("init/chromia.yml")!!.readText()
                        .replace("hello", name)
                        .replace("RELL_VERSION", RellVersion)
                        .replace("RELL_SCHEMA", "schema_${name.replace("-", "_")}")
        )
        val sourceDir = File(baseDir, "src")
        sourceDir.mkdir()
        File(sourceDir, "main.rell").writeText(
                this::class.java.getResource("init/src/main.rell")!!.readText()
        )
        val testDir = File(sourceDir, "test")
        testDir.mkdir()
        File(testDir, "arithmetic_test.rell").writeText(
                this::class.java.getResource("init/src/test/arithmetic_test.rell")!!.readText()
        )
        File(testDir, "data_test.rell").writeText(
                this::class.java.getResource("init/src/test/data_test.rell")!!.readText()
        )
    }
}
