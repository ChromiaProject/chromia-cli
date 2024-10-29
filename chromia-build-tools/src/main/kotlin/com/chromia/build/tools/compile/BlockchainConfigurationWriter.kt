package com.chromia.build.tools.compile

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.GtvString
import net.postchain.gtv.gtvml.GtvMLEncoder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

object BlockchainConfigurationWriter {
    /**
     * Store configuration as GtvML (XML).
     *
     * @throws IllegalArgumentException if any [GtvString] in [gtvConfig] contains
     * [character not allowed in XML](https://www.w3.org/TR/xml/#NT-Char)
     */
    fun storeConfig(
            gtvConfig: Gtv,
            outputName: String,
            outputDir: Path,
    ) {
        Files.createDirectories(outputDir)
        outputDir.resolve("${outputName}.xml").writeText(GtvMLEncoder.encodeXMLGtvStrict(gtvConfig))
    }

    /**
     * Store configuration as GtvML (XML).
     *
     * Will output ill-formed XML if any [GtvString] in [gtvConfig] contains
     * [character not allowed in XML](https://www.w3.org/TR/xml/#NT-Char)
     */
    fun storeConfigLenient(
            gtvConfig: Gtv,
            outputName: String,
            outputDir: Path,
    ) {
        Files.createDirectories(outputDir)
        outputDir.resolve("${outputName}.xml").writeText(GtvMLEncoder.encodeXMLGtv(gtvConfig))
    }

    /**
     * Store configuration as binary GTV.
     */
    fun storeConfigAsBinaryGtv(
            gtvConfig: Gtv,
            outputName: String,
            outputDir: Path,
    ) {
        Files.createDirectories(outputDir)
        outputDir.resolve("${outputName}.gtv").writeBytes(GtvEncoder.encodeGtv(gtvConfig))
    }
}
