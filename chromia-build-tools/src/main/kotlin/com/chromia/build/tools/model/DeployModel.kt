package com.chromia.cli.model

import com.chromia.build.tools.model.ensureBrid
import com.chromia.build.tools.model.ensureType
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvString
import net.postchain.gtv.listMapAndPrimitivesToGtv

data class DeploymentModel(
        //TODO make this public instead of private to remove extra getter
        private val brid: BlockchainRid,
        val container: String?, // Container id
        private val url: Gtv,
        val chains: Map<String, BlockchainRid> = mapOf()
) {
    val blockchainRid: BlockchainRid = brid
    val urls: List<String>
        get() {
            return when (url) {
                is GtvString -> listOf(url.asString())
                is GtvArray -> url.asArray().map { it.asString() }
                else -> throw IllegalArgumentException("deployment url must be either a single string or an array")
            }
        }

    companion object {
        fun load(data: Map<String, Any>, additionalProperty: String) = DeploymentModel(
                brid = ensureBrid(data["brid"]!!, "deployments", additionalProperty, "brid"),
                container = ensureType<String?>(data["container"], "deployments", additionalProperty, "container"),
                url = listMapAndPrimitivesToGtv(data["url"]),
                chains = ensureType<Map<String, Any>?>(data["chains"], "deployments", additionalProperty, "chains")
                        ?.mapValues {
                            ensureBrid(it.value, "deployments", additionalProperty, "chains", it.key)
                        } ?: mapOf(),
        )
    }
}
