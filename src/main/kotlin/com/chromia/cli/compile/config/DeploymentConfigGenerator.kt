package com.chromia.cli.compile.config

import com.chromia.cli.model.DeploymentModel
import net.postchain.common.BlockchainRid
import net.postchain.deployment.BlockchainConfiguration
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.gtvml.GtvMLEncoder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

object DeploymentConfigGenerator {
    fun generateConfig(gtvConfig: Gtv, deploymentModel: DeploymentModel?, outputName: String, generatedBrid: BlockchainRid, outputDir: Path, blockchainName: String): BlockchainConfiguration {

        val blockchainConfiguration = BlockchainConfiguration(
                deploymentModel?.chains?.get(blockchainName),
                generatedBrid,
                blockchainName,
                deploymentModel?.licence,
                gtvConfig
        )

        Files.createDirectories(outputDir)

        val xml = GtvMLEncoder.encodeXMLGtv(blockchainConfiguration.configuration)
        outputDir.resolve("${outputName}.xml").writeText(xml)

        val bytes = GtvEncoder.encodeGtv(blockchainConfiguration.configuration)
        outputDir.resolve("${outputName}.gtv").writeBytes(bytes)

        return blockchainConfiguration
    }
}
