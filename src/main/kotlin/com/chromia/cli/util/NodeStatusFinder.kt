package com.chromia.cli.util

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.request.Endpoint
import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement
import net.postchain.ebft.NodeState
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson.auto
import org.http4k.lens.asResult

class NodeStatusFinder(private val httpHandler: HttpHandler, // TODO: this should be removed when PostchainClient provides an official API for `my_status`
                       clientProvider: PostchainClientProvider,
                       templateConfig: PostchainClientConfig,
                       clusterManagement: ClusterManagement,
                       private val verbose: Boolean) {

    private val heightFinder = HeightFinder(clientProvider, templateConfig, clusterManagement)

    fun headers() = if (verbose) verboseHeaders else headers

    fun findStatus(endpoint: Endpoint, blockchainRid: BlockchainRid): StatusResult {
        if (!verbose) {
            return StatusResult.Ok(endpoint.url, heightFinder.findHeight(endpoint, blockchainRid))
        }

        //TODO remove trailing slash fix when it is no longer possible to register with it.
        val request = Request(Method.GET, "${endpoint.url.trimEnd().replace(Regex("/$"), "")}/node/${blockchainRid.toHex()}/my_status")
                .header("Accept", ContentType.APPLICATION_JSON.value)
        return try {
            val result = httpHandler(request)
            statusLens(result).map { StatusResult.VerboseOk(endpoint.url, it) }.mapFailure { StatusResult.VerboseError(endpoint.url, errorLens(result)) }.get()
        } catch (e: Exception) {
            StatusResult.VerboseError(endpoint.url, NodeStatus.Error("Node Unreachable"))
        }
    }

    companion object {
        val statusLens = Body.auto<NodeStatus.Status>().toLens().asResult()
        val errorLens = Body.auto<NodeStatus.Error>().toLens()

        private val headers = listOf("Node url", "Height", "Status")
        private val verboseHeaders = listOf("Node Url", "Height", "State", "Round", "Revolting", "Status")
    }

    sealed class NodeStatus {
        class Status(val state: NodeState, val height: Long, val serial: Long, val round: Long, val revolting: Boolean, val blockRid: String?) : NodeStatus()
        class Error(val error: String) : NodeStatus()

    }

    abstract sealed class StatusResult() {
        abstract fun values(): Array<String>


        class Ok(val url: String, val ok: HeightFinder.HeightResult) : StatusResult() {
            override fun values() = arrayOf(url, ok.height.toString(), ok.status)
        }

        class VerboseOk(val url: String, val status: NodeStatus.Status) : StatusResult() {
            override fun values() = arrayOf(url, status.height.toString(), status.state.name, status.round.toString(), status.revolting.toString(), "OK")
        }

        class VerboseError(val url: String, val status: NodeStatus.Error) : StatusResult() {
            override fun values() = arrayOf(url, "", "", "", "", status.error)
        }

    }
}
