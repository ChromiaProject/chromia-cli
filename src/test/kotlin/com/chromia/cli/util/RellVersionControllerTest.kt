package com.chromia.cli.util

import assertk.assertThat
import assertk.assertions.contains
import net.postchain.client.request.Endpoint
import net.postchain.common.BlockchainRid
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RellVersionControllerTest {

    private val httpHandler = mock<HttpHandler>()

    @Test
    fun getTargetVersion200Status() {
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.ACCEPTED, "").body("0.12.0"))
        val res = RellVersionController(httpHandler).getTargetVersion(Endpoint("foo"), BlockchainRid(ByteArray(32)))
        assertThat(res).contains("0.12.0")
    }

    @Test
    fun getTargetVersion404Status() {
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.NOT_FOUND, ""))
        val res = assertThrows<RuntimeException> { RellVersionController(httpHandler).getTargetVersion(Endpoint("foo"), BlockchainRid(ByteArray(32))) }.message!!
        assertThat(res).contains("Can not find blockchain with blockchainRID: 0000000000000000000000000000000000000000000000000000000000000000")
    }

    @Test
    fun getTargetVersion502Status() {
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.BAD_GATEWAY, ""))
        val res = assertThrows<RuntimeException> { RellVersionController(httpHandler).getTargetVersion(Endpoint("foo"), BlockchainRid(ByteArray(32))) }.message!!
        assertThat(res).contains("Unknown status 502 Bad Gateway for request foo/query/0000000000000000000000000000000000000000000000000000000000000000?type=rell.get_rell_version")
    }
}