package com.chromia.cli.util

import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_API_URLS
import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_CLUSTER
import net.postchain.client.core.PostchainQuery
import net.postchain.client.exception.ClientError
import net.postchain.client.request.Endpoint
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import org.http4k.core.Status

open class ClusterManagementStub(
        vararg val apiUrls: String
) : PostchainQuery {
    override fun query(name: String, args: Gtv): Gtv {
        return when (name) {
            CM_GET_BLOCKCHAIN_API_URLS -> gtv(apiUrls.map { gtv(it) })
            CM_GET_BLOCKCHAIN_CLUSTER -> {
                val blockchainRid = BlockchainRid(args["brid"]!!.asByteArray())
                return if (!blockchainRid.toHex().endsWith("4")) {
                    gtv("my_cluster")
                } else {
                    throw ClientError("", Status(404, null), "", Endpoint(""))
                }
            }

            else -> TODO("Not yet implemented")
        }
    }
}
