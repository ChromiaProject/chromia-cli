package com.chromia.cli.tools.config

import com.chromia.cli.model.ChromiaModel
import java.io.File
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.PropertiesFileLoader
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration


class ChromiaConfig(private val config: Configuration) : Configuration by config {
    fun get(apiurl: String? = null, blockchainRid: BlockchainRid? = null, secret: File? = null) =
            PropertiesConfiguration().apply {
                copy(config)
                apiurl?.let { setProperty("api.url", it) }
                blockchainRid?.let { setProperty("brid", it.toHex()) }
                secret?.let {
                    val secretProps = PropertiesFileLoader.load(it.absolutePath)
                    if (secretProps.containsKey("pubkey")) setProperty("pubkey", secretProps.getString("pubkey"))
                    if (secretProps.containsKey("privkey")) setProperty("privkey", secretProps.getString("privkey"))
                }
            }.let { PostchainClientConfig.fromConfiguration(it) }
}

fun ChromiaModel.client(config: ChromiaConfig, network: String, blockchain: String?, secret: File? = null): PostchainClientConfig {
    val deploymentModel = deployments[network]
    require(deploymentModel != null) { "Network $network is not a configured deployment" }
    require(blockchain == null || deploymentModel.chains[blockchain] != null) { "Configured blockchain $blockchain is not a configured blockchain in deployment $network" }
    return config.get("http://dummyhost", BlockchainRid.ZERO_RID, secret).copy(
            endpointPool = EndpointPool.default(deploymentModel.urls),
            blockchainRid = blockchain?.let { deploymentModel.chains[blockchain]!! }
                    ?: deploymentModel.blockchainRid,
    )
}
