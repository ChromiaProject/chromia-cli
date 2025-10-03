package com.chromia.cli.util

import com.chromia.build.tools.blockchain.BridFetcher
import com.chromia.build.tools.config.ChromiaClientConfig.Companion.DEFAULT_API_URL
import com.github.ajalt.clikt.core.PrintMessage
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.defaultHttpHandler
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import org.http4k.core.HttpHandler

object LibraryChainNetworkUtils {
    const val CHROMIA_MAINNET = "mainnet"
    const val TESTNET = "testnet"
    const val LOCALHOST = "localhost"

    private const val MAINNET_NODE_URL = "https://dapps0.chromaway.com"
    private const val TESTNET_NODE_URL = "https://node0.testnet.chromia.com:7740"

    data class NetworkConfig(val url: String, val brid: BlockchainRid)

    val libraryPredefinedNetworks: Map<String, () -> NetworkConfig> by lazy {
        mapOf(
            CHROMIA_MAINNET to {
                NetworkConfig(
                    url = MAINNET_NODE_URL,
                    brid = BlockchainRid.buildFromHex(
                        "C9051571CD822507DDD1F3B43F2DC066B54CC5A25ECD758A1B5A42913483CF20"
                    )
                )
            },
            TESTNET to {
                NetworkConfig(
                    url = TESTNET_NODE_URL,
                    brid = BlockchainRid.buildFromHex(
                        "76693857DEDCCA049BA3546ACADB2F73648B1A83FF8A8210F3F89EBD59DBC7C7"
                    )
                )
            },
            LOCALHOST to {
                NetworkConfig(
                    url = DEFAULT_API_URL,
                    brid = fetchBridFromLocalNode()
                )
            }
        )
    }

    private fun fetchBridFromLocalNode() = runCatching {
        val config = PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.singleUrl(DEFAULT_API_URL))
        val httpHandler: HttpHandler = defaultHttpHandler(config)
        BridFetcher(httpHandler, DEFAULT_API_URL).fetchBlockchainRid(0)
    }.onFailure { _ ->
        throw PrintMessage("Unable to fetch brid from local node")
    }.getOrThrow()
}
