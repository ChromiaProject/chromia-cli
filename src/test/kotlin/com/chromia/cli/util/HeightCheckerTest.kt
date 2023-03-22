package com.chromia.cli.util

import assertk.assert
import assertk.assertions.contains
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
        assert(hc.findSafeHeight(testBrid)).isEqualTo(7L + 10L)
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
        assert(it.headers).contains("Accept" to "application/json")
        when (it.uri.host) {
            "host1" -> Response(Status.OK).body("{\"blockHeight\":7}")
            "host2" -> Response(Status.INTERNAL_SERVER_ERROR).body("{\"error\":\"Module initialization error\"}")
            "host3" -> Response(Status.OK).body("{\"blockHeight\":5}")
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