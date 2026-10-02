package com.chromia.cli.test_utils

import com.chromia.build.tools.restapi.CachedModel
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.TestModel
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.common.BlockchainRid

// TestModel keeps transactions in non thread-safe collections, while deployments of several
// blockchains post their transactions in parallel, so concurrent posts could be lost.
class SynchronizedTxModel(private val model: CachedModel) : CachedModel by model {
    override fun postTransaction(tx: ByteArray) = synchronized(this) { model.postTransaction(tx) }

    override fun getStatus(txRID: TxRid): ApiStatus = synchronized(this) { model.getStatus(txRID) }
}

fun synchronizedDirectoryChainModel() = DirectoryChainModel(SynchronizedTxModel(TestModel(BlockchainRid.ZERO_RID)), 28)
