package com.chromia.cli.util

import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement

class TestClusterManagement(
        private val clusterOfBlockchain: Map<BlockchainRid, String> = mapOf(),
) : ClusterManagement {
    override fun getActiveBlockchains(clusterName: String) = TODO("Not yet implemented")
    override fun getBlockchainApiUrls(blockchainRid: BlockchainRid) = TODO("Not yet implemented")
    override fun getBlockchainPeers(blockchainRid: BlockchainRid, height: Long) = TODO("Not yet implemented")
    override fun getClusterAnchoringChains() = TODO("Not yet implemented")
    override fun getClusterInfo(clusterName: String) = TODO("Not yet implemented")
    override fun getClusterNames() = TODO("Not yet implemented")
    override fun getSystemAnchoringChain() = TODO("Not yet implemented")

    override fun getClusterOfBlockchain(blockchainRid: BlockchainRid) = clusterOfBlockchain[blockchainRid] ?: "system"
}
