package com.chromia.cli.util

import com.github.ajalt.clikt.core.CliktError
import net.postchain.client.exception.ClientError
import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement

class CliktClusterManagement(private val cm: ClusterManagement) : ClusterManagement by cm {
    override fun getClusterOfBlockchain(blockchainRid: BlockchainRid) =
            safeCall { cm.getClusterOfBlockchain(blockchainRid) }

    override fun getBlockchainApiUrls(blockchainRid: BlockchainRid) =
            safeCall { cm.getBlockchainApiUrls(blockchainRid) }

    private fun <T> safeCall(msg: String = "Cannot find cluster for chain", f: () -> T): T {
        return try {
            f()
        } catch (e: ClientError) {
            throw CliktError("$msg: ${e.message}")
        }
    }
}
