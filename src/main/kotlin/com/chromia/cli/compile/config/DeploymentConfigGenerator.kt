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

        if (deploymentModel == null) {
            throw RellCliErr("No deployment model found")
        }

        val blockchainConfiguration = BlockchainConfiguration(
                deploymentModel.bridBinary,
                generatedBrid,
                name,
                deploymentModel.containerName,
                gtvConfig
        )

        Files.createDirectories(outputDir)

        val xml = GtvMLEncoder.encodeXMLGtv(blockchainConfiguration.configuration)
        outputDir.resolve("${blockchainConfiguration.blockchainName}.xml").writeText(xml)

        val bytes = GtvEncoder.encodeGtv(blockchainConfiguration.configuration)
        outputDir.resolve("${blockchainConfiguration.blockchainName}.gtv").writeBytes(bytes)

        return blockchainConfiguration
    }

    fun generatePostchainClientConfig(blockchainRid: BlockchainRid, endpointPool: EndpointPool): PostchainClientConfig {
        val signers: List<KeyPair> = listOf()
        val statusPollCount: Int = STATUS_POLL_COUNT
        val statusPollInterval: Duration = STATUS_POLL_INTERVAL
        val failOverConfig = FailOverConfig()
        val cryptoSystem: CryptoSystem = Secp256K1CryptoSystem()
        val queryByChainId: Long? = null
        val maxResponseSize: Int = MAX_RESPONSE_SIZE
        val connectTimeout: Duration = CONNECT_TIMEOUT
        val responseTimeout: Duration = RESPONSE_TIMEOUT
        return PostchainClientConfig(
                blockchainRid,
                endpointPool,
                signers,
                statusPollCount,
                statusPollInterval,
                failOverConfig,
                cryptoSystem,
                queryByChainId,
                maxResponseSize,
                connectTimeout,
                responseTimeout
        )

    }
}