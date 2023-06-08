package com.chromia.cli.versionfinder

import net.postchain.client.request.Endpoint
import net.postchain.common.BlockchainRid
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status

class Http4kRellVersionFinder(private val httpHandler: HttpHandler) : RellVersionFinderInterface {

    override fun getTargetVersion(endpoint: Endpoint, brid: BlockchainRid): String {
        val request = Request(Method.GET, "${endpoint.url.trimEnd().replace(Regex("/$"), "")}/query/${brid.toHex()}?type=rell.get_rell_version")
                .header("Accept", ContentType.APPLICATION_JSON.value)

        val result = httpHandler(request)
        return when (result.status) {
            Status.ACCEPTED -> result.body.toString()
            Status.NOT_FOUND -> throw RuntimeException("Can not find blockchain with blockchainRID: $brid")
            else -> {
                throw RuntimeException("Unknown status ${result.status} for request ${request.uri}")
            }
        }
    }
}