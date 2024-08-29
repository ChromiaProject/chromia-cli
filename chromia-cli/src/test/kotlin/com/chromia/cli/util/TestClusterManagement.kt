package com.chromia.cli.util

import net.postchain.client.exception.ClientError
import net.postchain.client.request.Endpoint
import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement
import org.http4k.core.Status

open class TestClusterManagement(
        private val clusterOfBlockchain: Map<BlockchainRid, String> = mapOf(),
) : ClusterManagement {
    override fun getActiveBlockchains(clusterName: String) = TODO("Not yet implemented")
    override fun getBlockchainApiUrls(blockchainRid: BlockchainRid) = listOf("http://myhost:7740", "http://myhost2:7740")
    override fun getBlockchainPeers(blockchainRid: BlockchainRid, height: Long) = TODO("Not yet implemented")
    override fun getClusterAnchoringChains() = TODO("Not yet implemented")
    override fun getClusterInfo(clusterName: String) = TODO("Not yet implemented")
    override fun getClusterNames() = TODO("Not yet implemented")
    override fun getSystemAnchoringChain() = TODO("Not yet implemented")

    override fun getClusterOfBlockchain(blockchainRid: BlockchainRid) = clusterOfBlockchain[blockchainRid]
            ?: if (!blockchainRid.toHex().endsWith("4")) "my_cluster"
            else throw ClientError("", Status(404, null), "", Endpoint(""))

    override fun getRemovedClusterAnhcoringChains(removedAfter: Long): Collection<BlockchainRid> = TODO("Not yet implemented")
    override fun getRemovedClusterBlockchains(clusterName: String, removedAfter: Long): Collection<BlockchainRid> = TODO("Not yet implemented")
}
