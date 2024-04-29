package com.chromia.build.tools.config

import com.chromia.build.tools.keystore.ChromiaKeyStore
import com.chromia.cli.model.DeploymentModel
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.PropertiesFileLoader
import net.postchain.crypto.KeyPair
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration

class ChromiaClientConfig private constructor(
        private var config: PostchainClientConfig
) {
    val blockchainRid get() = config.blockchainRid
    val signers get() = config.signers
    val endpointPool get() = config.endpointPool
    val apiUrls get() = config.endpointPool.joinToString(",") { it.url }

    fun setBrid(blockchainRid: BlockchainRid) = apply {
        config = config.copy(blockchainRid = blockchainRid)
    }

    fun setApiUrls(vararg url: String) = apply {
        config = config.copy(endpointPool = EndpointPool.default(url.toList()))
    }

    fun setSigner(vararg keyPair: KeyPair) = apply {
        config = config.copy(signers = keyPair.toList())
    }

    fun setSignerFromSecret(path: Path) = apply {
        val secretProps = PropertiesFileLoader.load(path.absolutePathString())
        if (secretProps.containsKey("pubkey") && secretProps.containsKey("privkey")) {
            setSigner(KeyPair.of(secretProps.getString("pubkey"), secretProps.getString("privkey")))
        }
    }

    fun setDeployment(deploymentModel: DeploymentModel) = apply {
        setBrid(deploymentModel.blockchainRid)
        setApiUrls(*deploymentModel.urls.toTypedArray())
    }

    fun client(provider: PostchainClientProvider) = provider.createClient(config)

    companion object {

        val EMPTY = from(PropertiesConfiguration())
        fun from(config: Configuration): ChromiaClientConfig {
            return PropertiesConfiguration().apply {
                copy(config)
                if (!config.containsKey("api.url")) setProperty("api.url", "http://not-set")
                if (!config.containsKey("brid")) setProperty("brid", BlockchainRid.ZERO_RID)
            }.let { PostchainClientConfig.fromConfiguration(it) }
                    .let { ChromiaClientConfig(it) }
                    .let { res ->
                        if (config.containsKey("keyId")) {
                           ChromiaKeyStore(config.getString("keyId")).findKeyPair()?.let { res.setSigner(it)  }
                        }
                        res
                    }
        }
    }
}
