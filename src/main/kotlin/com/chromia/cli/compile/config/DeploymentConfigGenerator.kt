package com.chromia.cli.compile.config

import com.chromia.cli.model.DeploymentModel
import net.postchain.client.config.*
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.crypto.CryptoSystem
import net.postchain.crypto.KeyPair
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.deployment.BlockchainConfiguration
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.gtvml.GtvMLEncoder
import net.postchain.rell.utils.RellCliErr
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

object DeploymentConfigGenerator {
    fun generateConfig(gtvConfig: Gtv, deploymentModel: DeploymentModel?, name: String, generatedBrid: BlockchainRid, outputDir: Path): BlockchainConfiguration {

        val blockchainConfiguration = BlockchainConfiguration(
                deploymentModel?.blockchainRid,
                generatedBrid,
                name,
                deploymentModel?.licence,
                gtvConfig
        )

        Files.createDirectories(outputDir)

        val xml = GtvMLEncoder.encodeXMLGtv(blockchainConfiguration.configuration)
        outputDir.resolve("${blockchainConfiguration.blockchainName}.xml").writeText(xml)

        val bytes = GtvEncoder.encodeGtv(blockchainConfiguration.configuration)
        outputDir.resolve("${blockchainConfiguration.blockchainName}.gtv").writeBytes(bytes)

        return blockchainConfiguration
    }
}
