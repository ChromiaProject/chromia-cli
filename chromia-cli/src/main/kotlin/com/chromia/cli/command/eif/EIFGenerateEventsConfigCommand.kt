package com.chromia.cli.command.eif

import com.chromia.cli.command.ChromiaCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
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
import kotlin.io.path.listDirectoryEntries

//Reference implementation from:
// https://gitlab.com/chromaway/postchain-eif/-/blob/0.2.10/postchain-eif-core/src/main/kotlin/net/postchain/eif/cli/GenerateEventsConfigCommand.kt?ref_type=tags

class EIFGenerateEventsConfigCommand : ChromiaCommand(name = "generate-events-config", help = "Generate solidity events that EIF will listen to") {
    private val abiSource by option("--abi", help = "Path to a JSON ABI file or a directory of JSON ABI files")
            .file(mustExist = true, mustBeReadable = true)
            .required()

    private val eventNames by option("--events", help = "Names of the relevant events (Comma separated)", metavar = "TEXT")
            .split(",")
            .required()

    private val target by option("--target", help = "Target file to generate events in (defaults to \"build/eif-events.yaml\")")
            .file()
            .validate {
                require(it.name.endsWith(fileFormat.name.lowercase()))
                {
                    "Unexpected format of target file found: ${it.name}, expected type: ${fileFormat.name.lowercase()}. " +
                            "Change suffix or use --format option"
                }
            }

    private val fileFormat by option("--format", help = "Output file format").enum<FileFormat>()
            .default(FileFormat.YAML)

    override fun run() {
        val targetFile = target ?: File("build/eif-events.${fileFormat.name.lowercase()}")
        if (targetFile.exists()) {
            if (terminal.terminalInfo.inputInteractive) {
                if (YesNoPrompt("Target file: $targetFile already exists. Do you want to overwrite its content?",
                                terminal, default = false
                        ).ask() != true) throw PrintMessage("Generation of events was aborted")
            } else {
                throw CliktError("Target file: $targetFile already exists.")
            }
        }

        val abiJson = readAbiSource()
        val eventsConfig = generate(abiJson, eventNames, fileFormat)

        if (!targetFile.absoluteFile.parentFile.exists()) targetFile.absoluteFile.parentFile.mkdirs()
        targetFile.writeText(eventsConfig)
        echo("Events generated at: $targetFile")
    }


    private fun readAbiSource(): String {
        return if (abiSource.isDirectory) {
            abiSource.toPath().listDirectoryEntries("*.json").sortedDescending().joinToString(",", "[", "]") {
                it.toFile().readText().trim().trim('[', ']')
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

        val foundEvents = events.map { it.asDict()["name"]!!.asString() }
        val skippedEvents = eventNames.filter { it !in foundEvents }

        if (skippedEvents.isNotEmpty()) throw PrintMessage("Events: $skippedEvents not found in abi source: ${abiSource.toPath()}. Generation of events was aborted", 1)
        echo("Generating events for: $foundEvents")

        return when (format) {
            FileFormat.XML -> GtvMLEncoder.encodeXMLGtvStrict(gtv(events))
            FileFormat.YAML -> GtvYaml().dump(gtv(events))
        }
    }

    enum class FileFormat() {
        XML, YAML;
    }
}
