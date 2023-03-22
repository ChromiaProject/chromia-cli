package com.chromia.cli.util

import assertk.assert
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HeightCheckerTest {
    val testBrid = BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000001")

    @Test
    fun tenHigherThatnHighest() {
        val hc = HeightChecker(testClient(), TestClusterManagement("http://host1", "http://host2", "http://host3"))
        assert(hc.findSafeHeight(testBrid)).isEqualTo(570320L + 10L)
    }

    @Test
    fun noAvailableNodes() {
        assertThrows<IllegalArgumentException> {
            val hc = HeightChecker(testClient(), TestClusterManagement("http://host2", "http://wronghost"))
            hc.findSafeHeight(testBrid)
        }
    }

    private fun testClient(): (Request) -> Response = {
        assert(it.uri.path).endsWith("/blockchain/${testBrid.toHex()}/height")
        when (it.uri.host) {
            "host1" -> Response(Status.OK).body("{\"state\":\"WaitBlock\",\"height\":569889,\"serial\":159157543161,\"round\":0,\"revolting\":false}")
            "host2" -> Response(Status.INTERNAL_SERVER_ERROR).body("{\"error\":\"Module initialization error\"}")
            "host3" -> Response(Status.OK).body("{\"state\":\"HaveBlock\",\"height\":570320,\"serial\":158306890607,\"round\":1,\"blockRid\":\"13F8AE0B71917DFCBB612600BEBA8F3AE1BB788AA23357AE8C62DF9D3FE9EAC0\",\"revolting\":false}")
            else -> Response(Status.NOT_FOUND).body("{\"error\":\"Can't find blockchain\"}")
        }
    }

    class TestClusterManagement(vararg val apiUrls: String): ClusterManagement {

        override fun getClusterOfBlockchain(blockchainRid: BlockchainRid) = if (!blockchainRid.toHex().endsWith("1")) "my_cluster" else "system"
        override fun getBlockchainApiUrls(blockchainRid: BlockchainRid) = apiUrls.toList()
        override fun getActiveBlockchains(clusterName: String) = TODO("Not yet implemented")
        override fun getBlockchainPeers(blockchainRid: BlockchainRid, height: Long) = TODO("Not yet implemented")
        override fun getClusterInfo(clusterName: String) = TODO("Not yet implemented")
        override fun getClusterNames() = TODO("Not yet implemented")
        override fun getClusterAnchoringChains() = TODO("Not yet implemented")
        override fun getSystemAnchoringChain() = TODO("Not yet implemented")
    }
}