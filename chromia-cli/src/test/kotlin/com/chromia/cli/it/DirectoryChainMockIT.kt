package com.chromia.cli.it

import assertk.all
import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.startsWith
import com.chromia.build.tools.TestProcess
import com.chromia.cli.tools.blockchain.BridFetcher
import com.chromia.cli.util.testData
import com.chromia.directory1.anchoring_chain_common.getAnchoringTransactionForBlockRid
import com.chromia.directory1.anchoring_chain_common.getLastAnchoredBlock
import com.chromia.directory1.anchoring_chain_common.isBlockAnchored
import com.chromia.directory1.cm_api.cmGetBlockchainApiUrls
import com.chromia.directory1.cm_api.cmGetBlockchainCluster
import com.chromia.directory1.cm_api.cmGetClusterAnchoringChains
import com.chromia.directory1.cm_api.cmGetClusterBlockchains
import com.chromia.directory1.cm_api.cmGetClusterInfo
import com.chromia.directory1.cm_api.cmGetClusterNames
import com.chromia.directory1.cm_api.cmGetSystemAnchoringChain
import com.chromia.directory1.cm_api.cmGetSystemChains
import java.nio.file.Path
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.TxRid
import net.postchain.client.defaultHttpHandler
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.request.SingleEndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.common.toHex
import net.postchain.common.wrap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class DirectoryChainMockIT {
    @Test
    fun directoryChainMockIT(@TempDir dir: Path) {
        testData(dir)
        TestProcess.Builder("node", "start", "--directory-chain-mock", "--wipe")
                .awaitCompletion(false)
                .startCondition("Blockchain has been started")
                .verbose()
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start {
                    val config = PostchainClientConfig(BlockchainRid.ZERO_RID, endpointPool = SingleEndpointPool("http://localhost:7740"), queryByChainId = 0)
                    val client = PostchainClientImpl(config)
                    val managementChainBrid = BridFetcher(defaultHttpHandler(config), config.endpointPool.first().url).fetchBlockchainRid(0)

                    // Cluster Management API
                    assertThat(client.cmGetSystemChains().map { it.wrap() }).containsExactly(managementChainBrid.wData)
                    assertThat(client.cmGetClusterNames()).containsAll("test")
                    assertThat(client.cmGetSystemAnchoringChain()?.wrap()).isEqualTo(managementChainBrid.wData)
                    val clusterBlockchains = client.cmGetClusterBlockchains("test").map { it.wrap() }
                    assertThat(clusterBlockchains).all {
                        hasSize(2)
                        startsWith(managementChainBrid.wData)
                    }
                    val dappBrid = BlockchainRid(clusterBlockchains.last())

                    assertThat(client.cmGetBlockchainApiUrls(dappBrid)).containsExactly("http://localhost:7740")
                    val clusterInfo = client.cmGetClusterInfo("test")
                    assertThat(clusterInfo.peers).hasSize(1)
                    assertThat(clusterInfo.anchoringChain).isEqualTo(managementChainBrid.wData)

                    assertThat(client.cmGetBlockchainCluster(dappBrid.data)).isEqualTo("test")
                    assertThat(client.cmGetClusterAnchoringChains().map { it.wrap() }).containsExactly(managementChainBrid.wData)

                    // Anchoring API
                    val lastDappBlock = client.getLastAnchoredBlock(dappBrid)
                    assertThat(lastDappBlock).isNotNull()
                    assertThat(lastDappBlock!!.blockchainRid).isEqualTo(dappBrid.wData)
                    val lastAnchorBlock = client.getLastAnchoredBlock(managementChainBrid)
                    assertThat(lastAnchorBlock).isNotNull()
                    assertThat(lastAnchorBlock!!.blockchainRid).isEqualTo(managementChainBrid.wData)
                    assertThat(client.isBlockAnchored(managementChainBrid, lastAnchorBlock.blockRid.data)).isTrue()
                    assertThat(client.isBlockAnchored(dappBrid, lastDappBlock.blockRid.data)).isTrue()
                    assertThat(client.isBlockAnchored(dappBrid, ByteArray(32))).isFalse()

                    // Anchored ConfirmationProof can be found for arbritrary dapp block
                    val dappTx = client.getAnchoringTransactionForBlockRid(dappBrid, lastDappBlock.blockRid.data)
                    assertThat(client.confirmationProof(TxRid(dappTx!!.txRid.toHex())).wrap()).isNotNull()
                    // SAC Anchored ConfirmationProof can be found for arbritrary anchor block
                    val anchorTx = client.getAnchoringTransactionForBlockRid(managementChainBrid, lastAnchorBlock.blockRid.data)
                    assertThat(client.confirmationProof(TxRid(anchorTx!!.txRid.toHex()))).isNotNull()
                    // Confirmation proofs cannot be found for non-existing blocks/tx
                    assertThrows<UserMistake> {
                        client.getAnchoringTransactionForBlockRid(dappBrid, ByteArray(32))
                    }
                    assertThrows<UserMistake> {
                        client.confirmationProof(TxRid(ByteArray(32).toHex()))
                    }
                }
    }
}
