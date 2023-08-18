package com.chromia.cli.tools.blockchain

import java.net.ConnectException
import net.postchain.common.BlockchainRid
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request

class BridFinder(private val httpHandler: HttpHandler, private val url: String) {
    fun findBlockchainRid(id: Int): BlockchainRid {
        val request = Request(Method.GET, "$url/brid/iid_$id").header("Accept", ContentType.TEXT_PLAIN.value)

        try {
            val response = httpHandler(request)
            return BlockchainRid.buildFromHex(response.body.toString())
        } catch (e: ConnectException) {
            throw RuntimeException("Could not auto-detect brid from $url", e)
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("Could not auto-detect brid from $url")
        }
    }
}