package com.chromia.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.rell.gtx.PostchainBaseUtils

class BuildInfoCommand : CliktCommand(name = "info", help = "Calculate blockchain rid from a blockchain configuration file") {
    private val bcConfig by argument(help = "Blockchain configuration file (.gtv/.xml)")
            .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    override fun run() {
        val gtv = if (bcConfig.extension == "gtv") {
            bcConfig.inputStream().use {
                GtvDecoder.decodeGtv(it)
            }
        } else {
            GtvMLParser.parseGtvML(bcConfig.readText())
        }
        echo(PostchainBaseUtils.calcBlockchainRid(gtv).toHex())
    }
}
