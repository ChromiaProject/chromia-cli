package com.chromia.cli.d1

import com.chromia.build.tools.iccf.SingleNodeIccfGtxModule
import com.chromia.cli.d1.NopAnchoringOperation.Companion.ANCHOR_BLOCK_HEADER
import com.chromia.directory1.anchoring_chain_common.AnchorBlock
import com.chromia.directory1.anchoring_chain_common.AnchoringTxWithOpIndex
import net.postchain.PostchainContext
import net.postchain.base.data.PostgreSQLDatabaseAccess
import net.postchain.base.gtv.BlockHeaderData
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.NotFound
import net.postchain.common.types.RowId
import net.postchain.common.wrap
import net.postchain.concurrent.util.get
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.EContext
import net.postchain.core.block.BlockQueries
import net.postchain.core.block.BlockQueriesProvider
import net.postchain.crypto.CryptoSystem
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.GtvObjectMapper
import net.postchain.gtx.PostchainContextAware
import net.postchain.gtx.SimpleGTXModule

/**
 * GTXModule which responds to the anchoring api.
 * It anchors its own blocks block headers to itself to be able to respond with properly formatted data.
 *
 * NOT TO BE USED IN PRODUCTION
 *
 * This class is used to provide dummy anchoring proofs to be used with ICCF. A typical flow is
 * 1. User requests anchoring proof of a transaction
 * 2. Reply with a self-anchored transaction [NopAnchoringSpecialTXExtension].
 * 3. User verifies that this tx has been anchored in system anchoring chain using is_block_anchored (always true)
 *    and gets a conformation proof. /confirmation_proof returns a valid response
 * 4. This proof is appended to `iccf_proof` operation but is never verified in [SingleNodeIccfGtxModule].
 */
class NopAnchoringGTXModule : PostchainContextAware, SimpleGTXModule<NopAnchoringGTXModule.Companion>(
        Companion,
        opmap = mapOf(ANCHOR_BLOCK_HEADER to { _, data -> NopAnchoringOperation(data) }),
        querymap = mapOf(
                "get_last_anchored_block" to { _, _, args ->
                    val brid = gtvToBlockchainRid(args["blockchain_rid"]!!)
                    val bq = blockQueriesProvider.getBlockQueries(brid) ?: throw NotFound("Blockchain $brid not found")
                    val height = bq.getLastBlockHeight().get()
                    getBlockAtHeight(bq, height, brid)
                },
                "get_anchored_block_at_height" to { _, _, args ->
                    val brid = gtvToBlockchainRid(args["blockchain_rid"]!!)
                    val height = args["height"]!!.asInteger()
                    val bq = blockQueriesProvider.getBlockQueries(brid) ?: throw NotFound("Blockchain $brid not found")
                    getBlockAtHeight(bq, height, brid)
                },
                "get_anchoring_transaction_for_block_rid" to { _, ctx, args ->
                    val brid = gtvToBlockchainRid(args["blockchain_rid"]!!)
                    val blockRid = gtvToByteArray(args["block_rid"]!!)
                    val bq = blockQueriesProvider.getBlockQueries(brid) ?: throw NotFound("Blockchain $brid not found")
                    val block = bq.getBlock(blockRid, txHashesOnly = false).get()
                            ?: throw NotFound("Block $blockRid not found")
                    val thisBq = blockQueriesProvider.getBlockQueries(db.getBlockchainRid(ctx)!!)!!
                    val height = thisBq.getLastBlockHeight().get()
                    val anchorRid = thisBq.getBlockRid(height).get()!!
                    val anchorBlock = thisBq.getBlock(anchorRid, txHashesOnly = false).get()!!
                    val tx = AnchoringTxWithOpIndex(
                            txRid = anchorBlock.transactions.first().rid.wrap(),
                            txData = block.header.wrap(),
                            txOpIndex = 0
                    )
                    GtvObjectMapper.toGtvDictionary(tx)
                },
                "is_block_anchored" to { _, ctx, args ->
                    val brid = gtvToBlockchainRid(args["blockchain_rid"]!!)
                    val blockRid = gtvToByteArray(args["block_rid"]!!)
                    if (brid == db.getBlockchainRid(ctx)!!) gtv(true)  // Always true for SAC
                    val bq = blockQueriesProvider.getBlockQueries(brid) ?: throw NotFound("Blockchain $brid not found")
                    gtv(bq.getBlock(blockRid, true).get() != null)
                },
        )
) {
    override fun initializeDB(ctx: EContext) {}

    override fun getSpecialTxExtensions() = listOf(NopAnchoringSpecialTXExtension())

    override fun initializeContext(configuration: BlockchainConfiguration, postchainContext: PostchainContext) {
        blockQueriesProvider = postchainContext.blockQueriesProvider
        cryptoSystem = postchainContext.cryptoSystem
    }

    companion object {
        lateinit var blockQueriesProvider: BlockQueriesProvider
        lateinit var cryptoSystem: CryptoSystem
        private val db = PostgreSQLDatabaseAccess()

        private fun getBlockAtHeight(bq: BlockQueries, height: Long, brid: BlockchainRid): GtvDictionary {
            val block = bq.getBlockAtHeight(height).get()!!
            val decodedHeader = BlockHeaderData.fromBinary(block.header.rawData)
            val aBlock = AnchorBlock(
                    transaction = RowId(0), // Rell entity rowid (not used)
                    blockchainRid = brid.wData,
                    blockHeight = height,
                    blockHeader = block.header.rawData.wrap(),
                    blockRid = block.header.blockRID.wrap(),
                    witness = block.witness.getRawData().wrap(),
                    timestamp = decodedHeader.getTimestamp(),
                    anchoringTxOpIndex = 0
            )
            return GtvObjectMapper.toGtvDictionary(aBlock)
        }
    }
}
