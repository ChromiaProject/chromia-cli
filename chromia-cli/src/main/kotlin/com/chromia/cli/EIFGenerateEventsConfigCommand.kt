package com.chromia.cli

import com.chromia.cli.tools.config.chromiaModelOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.terminal.YesNoPrompt
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.gtvml.GtvMLEncoder
import net.postchain.gtv.make_gtv_gson
import net.postchain.gtv.yaml.GtvYaml
import java.io.File
import java.nio.file.Files

//Reference implementation from:
// https://gitlab.com/chromaway/postchain-eif/-/blob/0.2.10/postchain-eif-core/src/main/kotlin/net/postchain/eif/cli/GenerateEventsConfigCommand.kt?ref_type=tags

class GenerateEventsConfigCommand : CliktCommand(name = "generate-events-config", help = "Generate events config from JSON ABI") {
    private val settings by chromiaModelOption()

    private val abiSource by option("--abi", help = "Path to a JSON ABI file or a directory of JSON ABI files")
            .file(mustExist = true, mustBeReadable = true)
            .required()

    private val eventNames by option("--events", help = "Names of the relevant events. Separate events with ','")
            .split(",")
            .required()

    private val target by option("--target", help = "Target file to generate events in (defaults to \"build/events.yaml\")")
            .file()
            .validate {
                require(it.name.endsWith(fileFormat.name.lowercase()))
                { "Unexpected format of target file found: ${it.name}, expected type: ${fileFormat.name.lowercase()}. " +
                        "Change suffix or use --format option" }
            }


    private val fileFormat by option("--format", help = "Output file format").enum<FileFormat>()
            .default(FileFormat.YAML)

    override fun run() {
        val targetFile = target ?: File(settings.targetDir, "events.${fileFormat.name.lowercase()}")
        if (targetFile.exists() && YesNoPrompt("Target file: $targetFile already exists. Do you want to overwrite its content?",
                        terminal, default = false
                ).ask() != true) throw PrintMessage("Generation of events was aborted")
        val abiJson = readAbiSource()


        echo("Generating events for $eventNames.")
        val eventsConfig = generate(abiJson, eventNames, fileFormat)

        Files.createDirectories(targetFile.toPath().parent)
        targetFile.writeText(eventsConfig)
        echo("Events generated at: $targetFile")
    }


    private fun readAbiSource(): String {
        return if (abiSource.isDirectory) {
            abiSource.listFiles().joinToString(",", "[", "]") {
                it.readText().trim().trim('[', ']')
            }
        } else {
            abiSource.readText()
        }
    }

    private fun generate(json: String, eventNames: List<String>, format: FileFormat): String {
        val gson = make_gtv_gson()
        val gtv = gson.fromJson(json, Gtv::class.java)
        val events = gtv.asArray()
                .filter { it.asDict()["type"]!!.asString() == "event" && eventNames.contains(it.asDict()["name"]!!.asString()) }

        return when (format) {
            FileFormat.XML -> GtvMLEncoder.encodeXMLGtv(gtv(events))
            FileFormat.YAML -> GtvYaml().dump(gtv(events))
        }
    }

    enum class FileFormat() {
        XML, YAML;
    }
}
