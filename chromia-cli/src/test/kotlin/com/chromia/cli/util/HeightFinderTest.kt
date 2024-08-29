package com.chromia.cli.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.HeightFinder
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.exception.ClientError
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock

class HeightFinderTest {
    val testBrid = BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000001")
    val testConfigTemplate = PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.singleUrl(""))

    @Test
    fun tenHigherThatnHighest() {
        val hc = HeightFinder(testClientProvider(), testConfigTemplate, TestClusterManagement("http://host1", "http://host2", "http://host3"))
        assertThat(hc.findSafeHeight(testBrid)).isEqualTo(7L + 10L)
    }

    @Test
    fun noAvailableNodes() {
        assertThrows<IllegalArgumentException> {
            val hc = HeightFinder(testClientProvider(), testConfigTemplate, TestClusterManagement("http://host2", "http://wronghost"))
            hc.findSafeHeight(testBrid)
        }
    }


    private fun testClientProvider(): PostchainClientProvider {
        return PostchainClientProvider {
            assertThat(it.endpointPool.size).isEqualTo(1)
            val endpoint = it.endpointPool.first()
            return@PostchainClientProvider when (endpoint.url) {
                "http://host1" -> mock { on { currentBlockHeight() } doReturn 7L }
                "http://host2" -> mock { on { currentBlockHeight() } doThrow ClientError("", Status.INTERNAL_SERVER_ERROR, "Module initialization error", endpoint) }
                "http://host3" -> mock { on { currentBlockHeight() } doReturn 5L }
                else -> mock { on { currentBlockHeight() } doThrow ClientError("", Status.NOT_FOUND, "Can't find blockchain", endpoint) }
            }
        }
    }


    class TestClusterManagement(vararg val apiUrls: String) : ClusterManagement {

        override fun getClusterOfBlockchain(blockchainRid: BlockchainRid) = if (!blockchainRid.toHex().endsWith("1")) "my_cluster" else "system"
        override fun getRemovedClusterAnhcoringChains(removedAfter: Long): Collection<BlockchainRid> = TODO("Not yet implemented")
        override fun getRemovedClusterBlockchains(clusterName: String, removedAfter: Long): Collection<BlockchainRid> = TODO("Not yet implemented")
        override fun getBlockchainApiUrls(blockchainRid: BlockchainRid) = apiUrls.toList()
        override fun getActiveBlockchains(clusterName: String) = TODO("Not yet implemented")
        override fun getBlockchainPeers(blockchainRid: BlockchainRid, height: Long) = TODO("Not yet implemented")
        override fun getClusterInfo(clusterName: String) = TODO("Not yet implemented")
        override fun getClusterNames() = TODO("Not yet implemented")
        override fun getClusterAnchoringChains() = TODO("Not yet implemented")
        override fun getSystemAnchoringChain() = TODO("Not yet implemented")
    }
}