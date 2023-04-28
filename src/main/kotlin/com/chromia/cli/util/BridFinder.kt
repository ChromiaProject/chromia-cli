package com.chromia.cli.util

import com.github.ajalt.clikt.core.PrintMessage
import net.postchain.common.BlockchainRid
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson.auto
import java.net.ConnectException

class BridFinder(private val httpHandler: HttpHandler, private val url: String) {
    fun findBlockchainRid(id: Int): BlockchainRid {
        val request = Request(Method.GET, "$url/brid/iid_$id").header("Accept", ContentType.TEXT_PLAIN.value)

        try {
            val response = httpHandler(request)
            return BlockchainRid.buildFromHex(response.body.toString())
        } catch (e: ConnectException) {
            throw PrintMessage("Could not auto-detect brid from $url")
        }
    }
}