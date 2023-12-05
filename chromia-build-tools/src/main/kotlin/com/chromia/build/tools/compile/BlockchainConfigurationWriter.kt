package com.chromia.build.tools.compile

import net.postchain.gtv.Gtv
import net.postchain.gtv.gtvml.GtvMLEncoder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

object BlockchainConfigurationWriter {
    fun storeConfig(
            gtvConfig: Gtv,
            outputName: String,
            outputDir: Path,
    ) {
        Files.createDirectories(outputDir)
        val xml = GtvMLEncoder.encodeXMLGtv(gtvConfig)
        outputDir.resolve("${outputName}.xml").writeText(xml)
    }
}
