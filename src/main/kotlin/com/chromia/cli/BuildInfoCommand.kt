package com.chromia.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.gtv.GtvDecoder
import net.postchain.rell.utils.PostchainUtils

class BuildInfoCommand: CliktCommand(name = "info", help = "Calculate blockchain rid from a blockchain configuration file") {
    private val bcConfig by argument(help = "Blockchain configuration file (.gtv/.xml)")
            .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    override fun run() {
        val gtv = if (bcConfig.extension == "gtv") {
            bcConfig.inputStream().use {
                GtvDecoder.decodeGtv(it)
            }
        } else {
            PostchainUtils.xmlToGtv(bcConfig.readText())
        }
        echo(PostchainUtils.calcBlockchainRid(gtv).toHex())
    }
}
