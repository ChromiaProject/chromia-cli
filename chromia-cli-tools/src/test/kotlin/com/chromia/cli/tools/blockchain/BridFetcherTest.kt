package com.chromia.cli.tools.blockchain

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import net.postchain.common.BlockchainRid
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import java.net.ConnectException
import kotlin.test.assertFailsWith

class BridFetcherTest {

    val blockchainRid = BlockchainRid.buildRepeat(1)

    @Test
    fun success() {
        val sut = BridFetcher({ _ -> Response(Status.OK).body(blockchainRid.toHex()) }, "http://localhost:0")
        assertThat(sut.fetchBlockchainRid(1)).isEqualTo(blockchainRid)
    }

    @Test
    fun `non-successful response`() {
        val sut = BridFetcher({ _ -> Response(Status.BAD_REQUEST).body("the error message") }, "http://localhost:0")
        val e = assertFailsWith<RuntimeException> { sut.fetchBlockchainRid(1) }
        assertThat(e.message!!).contains("Bad Request")
    }

    @Test
    fun `cannot connect`() {
        val sut = BridFetcher({ _ -> throw ConnectException("can't connect") }, "http://localhost:0")
        val e = assertFailsWith<RuntimeException> { sut.fetchBlockchainRid(1) }
        assertThat(e.message!!).contains("can't connect")
    }

    @Test
    fun `invalid response`() {
        val sut = BridFetcher({ _ -> Response(Status.OK).body("123") }, "http://localhost:0")
        val e = assertFailsWith<RuntimeException> { sut.fetchBlockchainRid(1) }
        assertThat(e.message!!).contains("Invalid hex string")
    }
}
