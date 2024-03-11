package com.chromia.cli.model

import com.chromia.build.tools.model.ensureType
import net.postchain.gtv.listMapAndPrimitivesToGtv
import net.postchain.common.BlockchainRid
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap
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
    val urls: List<String>
        get() {
            return when (url) {
                is GtvString -> listOf(url.asString())
                is GtvArray -> url.asArray().map { it.asString() }
                else -> throw IllegalArgumentException("deployment url must be either a single string or an array")
            }
        }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun load(data: Map<String, Any>, additionalProperty: String) = DeploymentModel(
                brid = ensureType<ByteArray>(data["brid"], "deployments", additionalProperty, "brid").wrap(),
                container = ensureType<String?>(data["container"], "deployments", additionalProperty, "container"),
                url = listMapAndPrimitivesToGtv(data["url"]),
                chains = ensureType<Map<String, ByteArray>?>(data["chains"], "deployments", additionalProperty, "chains")
                        ?.mapValues {
                            BlockchainRid(it.value)
                        } ?: mapOf(),
        )
    }
}
