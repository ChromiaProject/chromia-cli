package com.chromia.cli.util

import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson.auto

class HeightChecker(private val httpHandler: HttpHandler, private val clusterManagement: ClusterManagement) {

    fun findSafeHeight(blockchainRid: BlockchainRid, safety: Long = 10): Long {
        val apiUrls = clusterManagement.getBlockchainApiUrls(blockchainRid)
        return findBestHeight(apiUrls, blockchainRid) + safety
    }

    private fun findBestHeight(apiUrls: Collection<String>, blockchainRid: BlockchainRid) =
            apiUrls.map {
                try {
                    val res = httpHandler(Request(Method.GET, "$it/blockchain/${blockchainRid.toHex()}/height"))
                    Body.auto<Height>().toLens()(res).height
                } catch (e: Exception) {
                    -1
                }
            }.max().also {
                if (it <= 0) throw IllegalArgumentException("Deployment failed, no nodes are building blocks for chain $blockchainRid")
            }

    internal class Height(val height: Long)
}
