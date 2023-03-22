package com.chromia.cli.util

import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.exception.ClientError
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.request.SingleEndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson.auto

class HeightChecker(private val httpHandler: HttpHandler, private val clusterManagement: ClusterManagement, private val templateConfig: PostchainClientConfig) {

    fun findSafeHeight(blockchainRid: BlockchainRid, safety: Long = 10): Long {
        val apiUrls = clusterManagement.getBlockchainApiUrls(blockchainRid)
        return findBestHeight(apiUrls, blockchainRid) + safety
    }

    private fun findBestHeight(apiUrls: Collection<String>, blockchainRid: BlockchainRid) =
            apiUrls.map {
                try {
                    PostchainClientImpl(templateConfig.copy(blockchainRid, SingleEndpointPool(it)), httpHandler).currentBlockHeight()
                } catch (e: ClientError) {
                    -1
                }
            }.max().also {
                if (it <= 0) throw IllegalArgumentException("Deployment failed, no nodes are building blocks for chain $blockchainRid")
            }
}
