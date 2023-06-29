package com.chromia.cli.util

import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.exception.ClientError
import net.postchain.client.request.Endpoint
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement

class HeightFinder(private val clientProvider: PostchainClientProvider, private val templateConfig: PostchainClientConfig, private val clusterManagement: ClusterManagement) {

    fun findSafeHeight(blockchainRid: BlockchainRid, safety: Long = 10): Long {
        return findHeight(EndpointPool.default(clusterManagement.getBlockchainApiUrls(blockchainRid).toList()), blockchainRid)
                .maxOf { it.height }
                .also { if (it < 0) throw IllegalArgumentException("No height found for any node") }
                .let { it + safety }
    }

    fun findHeight(endpointPool: EndpointPool, blockchainRid: BlockchainRid): Collection<HeightResult> {
        return endpointPool.map { endpoint -> findHeight(endpoint, blockchainRid) }
    }

    fun findHeight(endpoint: Endpoint, blockchainRid: BlockchainRid) =
            try {
                clientProvider.createClient(templateConfig.copy(blockchainRid, EndpointPool.singleUrl(endpoint.url)))
                        .currentBlockHeight()
                        .let { HeightResult.OkResult(it) }
            } catch (e: ClientError) {
                HeightResult.FailedResult(e.message!!)
            }

    sealed class HeightResult(val height: Long, val status: String) {
        class OkResult(height: Long) : HeightResult(height, "OK")
        class FailedResult(msg: String) : HeightResult(-1, msg)
    }
}