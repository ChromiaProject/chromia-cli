package com.chromia.cli.tools.config

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.parseModel
import java.io.File
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.PropertiesFileLoader
import net.postchain.crypto.KeyPair
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration

class ChromiaConfig(
        private val config: Configuration,
        val nullableModel: ChromiaModel?,
        val nullableModelFile: File?
) : Configuration by config {
    val model get() = nullableModel ?: throw IllegalArgumentException("Chromia model is not configured")
    val modelFile get() = nullableModelFile ?: throw IllegalArgumentException("Chromia model is not configured")

    val sourceDir get() = model.compile.sourceFile(modelFile.parentFile)
    val targetDir get() = model.compile.targetFile(modelFile.parentFile)
    val compileModel get() = model.compile
    val testModel get() = model.test

    fun clientConfig(apiurl: String, blockchainRid: BlockchainRid, secret: File? = null): PostchainClientConfig {
        val tempConfig = PropertiesConfiguration().apply {
            copy(config)
            setProperty("api.url", apiurl)
            setProperty("brid", blockchainRid.toHex())
        }
        return clientConfig(tempConfig, secret)
    }

    fun clientConfig(template: Configuration = config, secret: File? = null, network: String? = null, blockchain: String? = null): PostchainClientConfig {
        val config = if (network == null) {
            PostchainClientConfig.fromConfiguration(template)
        } else {
            require(nullableModel != null) { "Chromia model is null" }
            val deploymentModel = nullableModel.deployments[network]
            require(deploymentModel != null) { "Network $network not configured in deployments part of ${nullableModelFile?.path}" }
            require(blockchain == null || deploymentModel.chains[blockchain] != null) { "Configured blockchain $blockchain is not configured in ${nullableModelFile?.path}" }
            PostchainClientConfig(
                    endpointPool = EndpointPool.default(deploymentModel.urls),
                    blockchainRid = blockchain?.let { deploymentModel.chains[blockchain]!! }
                            ?: deploymentModel.blockchainRid,
            )
        }
        return config.withSecret(secret)
    }

    private fun PostchainClientConfig.withSecret(secret: File?): PostchainClientConfig {
        if (secret == null) return this
        val secretProps = PropertiesFileLoader.load(secret.absolutePath)
        return copy(
                signers = listOf(
                        KeyPair.of(secretProps.getString("pubkey"), secretProps.getString("privkey"))
                )
        )
    }

    fun client(provider: PostchainClientProvider, network: String? = null, blockchain: String? = null): PostchainClient {
        return provider.createClient(clientConfig(network = network, blockchain = blockchain))
    }

    companion object {
        private const val DEFAULT_CONFIG_FILENAME = ".chromia/config"
        private const val DEFAULT_CHROMIA_MODEL_FILENAME = "chromia.yml"
        private const val DEFAULT_CONFIG_MODEL_FILENAME = "config.yml"

        fun fromModel(modelFile: File?) = load(modelFile, null)
        fun load(modelFile: File?, extraFile: File?): ChromiaConfig {
            val config = PropertiesConfiguration()
            config.setProperty("status.poll-interval", 2000)
            loadFromFileIfExists(globalConfigurationFile(), config)
            loadFromFileIfExists(localConfigurationFile(), config)
            loadFromFileIfExists(extraFile, config)
            val model = chromiaModel(modelFile)
            return ChromiaConfig(config, model?.second, model?.first?.absoluteFile)
        }

        private fun loadFromFileIfExists(file: File?, config: Configuration) {
            if (file != null && file.exists()) {
                val c = PropertiesFileLoader.load(file.absolutePath)
                c.keys.forEach { key -> config.setProperty(key, c.getProperty(key)) }
            }
        }

        fun globalConfigurationFile() = File("${System.getProperty("user.home")}/$DEFAULT_CONFIG_FILENAME")
        fun localConfigurationFile() = File(DEFAULT_CONFIG_FILENAME)
        private fun chromiaModel(explicitFile: File?): Pair<File, ChromiaModel>? {
            if (explicitFile != null) {
                require(explicitFile.isFile) { "File $explicitFile is not a regular file" }
                return explicitFile to parseModel(explicitFile)
            }
            val chromiaModelFile = File(DEFAULT_CHROMIA_MODEL_FILENAME)
            if (chromiaModelFile.isFile) {
                return chromiaModelFile to parseModel(chromiaModelFile)
            }

            val configModelFile = File(DEFAULT_CONFIG_MODEL_FILENAME)
            if (configModelFile.isFile) {
                return chromiaModelFile to parseModel(configModelFile)
            }

            return null
        }
    }
}