package com.chromia.cli.util

import com.chromia.build.tools.blockchain.BridFetcher
import com.github.ajalt.clikt.core.PrintMessage
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.defaultHttpHandler
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import org.http4k.core.HttpHandler

object PredefinedNetworks {
    const val CHROMIA_MAINNET = "https://system.chromaway.com:7740"
    const val LOCAL_HOST = "http://localhost:7740"

    // TODO: add library-chain (url, brid) on mainnet when it will be deployed
    val predefinedNetworks by lazy {
        mapOf(
            "mainnet" to {
                Pair(
                    CHROMIA_MAINNET,
                    BlockchainRid.buildFromHex(
                        "C9051571CD822507DDD1F3B43F2DC066B54CC5A25ECD758A1B5A42913483CF20"
                    )
                )
            },
            "testnet" to {
                Pair(
                    "https://node0.testnet.chromia.com:7740",
                    BlockchainRid.buildFromHex(
                        "E93362BE11F3BEDB98B4A4CC61FC1FC9ED0A8DD372983070EAC9DD3FBB666ACE"
                    )
                )
            },
            "devnet" to {
                Pair(
                    "https://node8.devnet1.chromia.dev:7740",
                    BlockchainRid.buildFromHex(
                        "6933A4AB594C85FCAF8D3B7EA14F11CA4B06826EE1A3A823055D1CC923E71FF9"
                    )
                )
            },
            "localhost" to {
                Pair(
                    LOCAL_HOST,
                    fetchBridFromLocalNode()
                )
            }
        )
    }

    private fun fetchBridFromLocalNode() = runCatching {
        val config = PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.singleUrl(LOCAL_HOST))
        val httpHandler: HttpHandler = defaultHttpHandler(config)
        BridFetcher(httpHandler, LOCAL_HOST).fetchBlockchainRid(0)
    }.onFailure { _ ->
        throw PrintMessage("Unable to fetch brid from local node")
    }.getOrThrow()
}
