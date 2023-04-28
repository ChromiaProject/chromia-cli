package com.chromia.cli.util

import com.github.ajalt.clikt.core.PrintMessage
import net.postchain.common.BlockchainRid
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class BridFinder(private val url: String) {
    fun findBlockchainRid(id: Int): BlockchainRid {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
                .uri(URI.create("${url}/brid/iid_${id}"))
                .build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            return BlockchainRid.buildFromHex(response.body())
        } catch (e: ConnectException) {
            throw PrintMessage("Could not auto-detect brid from $url")
        }
    }
}