package com.chromia.cli.command.tools

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.util.OutputFormat
import com.chromia.cli.util.formatJson
import com.chromia.cli.util.formatRaw
import com.chromia.cli.util.formatXml
import com.chromia.cli.util.formatYaml
import com.chromia.cli.util.outputFormat
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.GtvException
import net.postchain.gtv.merkle.makeMerkleHashCalculator
import net.postchain.gtv.merkleHash
import net.postchain.gtv.pretty

class GtvCommand : ChromiaCommand(help = """
    Decode and convert GTV data
    
    Use `--hex` option or pipe binary GTV data to the command.
    
    Examples:
    ```
    chr gtv --hex A41A3018300A0C0161A2050C03464F4F300A0C0162A2050C03424152
    chr gtv --output-format yaml < data.gtv
    ```    
""".trimIndent()
) {
    private val hex by option("--hex", help = "Hex encoded GTV data", metavar = "hex").convert { it.hexStringToByteArray() }
    private val outputFormat by outputFormat()
    private val hash by option("--hash", help = "Calculate Merkle hash of the GTV data", metavar = "version").long()
            .validate { require(it >0) { "Merkle hash version must be greater than 0"} }

    override fun run() {
        val data = hex ?: if (terminal.terminalInfo.inputInteractive) {
            val input = terminal.readLineOrNull(hideInput = true)
            if (!input.isNullOrEmpty()) {
                try {
                    input.hexStringToByteArray()
                } catch (e: IllegalArgumentException) {
                    throw CliktError(e.message!!)
                }
            } else {
                throw UsageError("Please provide some GTV input")
            }
        } else {
            System.`in`.readAllBytes()
        }

        val gtv = try {
            GtvDecoder.decodeGtv(data)
        } catch (e: GtvException) {
            throw CliktError("Invalid GTV data: ${e.message!!}")
        }

        if (hash != null) {
            echo(gtv.merkleHash(makeMerkleHashCalculator(hash!!)).toHex())
        } else {
            echo(when (outputFormat) {
                OutputFormat.pretty -> gtv.pretty()
                OutputFormat.raw -> formatRaw(gtv)
                OutputFormat.JSON -> formatJson(gtv)
                OutputFormat.XML -> formatXml(gtv)
                OutputFormat.YAML -> formatYaml(gtv)
            })
        }
    }
}
