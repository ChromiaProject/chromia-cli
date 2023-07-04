package com.chromia.cli.model

import net.postchain.common.BlockchainRid
import net.postchain.common.types.WrappedByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvString

data class DeploymentModel(
        private val brid: WrappedByteArray,
        val container: String?, // Container id
        private val url: Gtv,
        val chains: Map<String, BlockchainRid> = mapOf()
) {
    val blockchainRid: BlockchainRid = BlockchainRid(brid)
    val urls: List<String> get() {
        return when (url) {
            is GtvString -> listOf(url.asString())
            is GtvArray -> url.asArray().map { it.asString() }
            else -> throw IllegalArgumentException("deployment url must be either a single string or an array")
        }
    }
}
