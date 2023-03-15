package com.chromia.cli.util

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.mapFailure
import net.postchain.common.BlockchainRid
import net.postchain.ebft.NodeState
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson.auto
import org.http4k.lens.asResult

class NodeStatusChecker(private val blockchainRid: BlockchainRid, private val httpHandler: HttpHandler) {

    fun checkStatus(url: String): NodeStatus {
        val request = Request(Method.GET, "$url/node/${blockchainRid.toHex()}/my_status")
        return try {
            val result = httpHandler(request)
            statusLens(result).mapFailure { errorLens(result) }.get()
        } catch (e: Exception) {
            NodeStatus.Error("Node Unreachable")
        }
    }

    companion object {
        val statusLens = Body.auto<NodeStatus.Status>().toLens().asResult()
        val errorLens = Body.auto<NodeStatus.Error>().toLens()
    }

    sealed class NodeStatus {
        class Status(val state: NodeState, val height: Long, val serial: Long, val round: Long, val revolting: Boolean, val blockRid: String?) : NodeStatus()
        class Error(val error: String) : NodeStatus()

    }
}
