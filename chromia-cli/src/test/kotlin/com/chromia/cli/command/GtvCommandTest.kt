package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.cli.command.tools.GtvCommand
import com.github.ajalt.clikt.testing.test
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtv.merkle.GtvMerkleHashCalculatorV1
import net.postchain.gtv.merkle.makeMerkleHashCalculator
import net.postchain.gtv.merkleHash
import net.postchain.gtv.parse.GtvParser
import org.junit.jupiter.api.Test
import java.math.BigInteger

class GtvCommandTest {
    private val sampleGtv = gtv(mapOf(
            "a" to gtv(listOf(gtv(1), gtv("foo bar"), GtvNull)),
            "b" to gtv(listOf(gtv("1234ABCD".hexStringToByteArray()), gtv(BigInteger("19223372036854775807")))),
            "c" to gtv(listOf(gtv(listOf(gtv("foo")))))
    ))

    @Test
    fun `hex input on command line`() {
        val hex = GtvEncoder.encodeGtv(sampleGtv).toHex()
        val res = GtvCommand().test(listOf("--hex", hex))
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.stdout).isEqualTo("""[
        |  "a": [
        |    1,
        |    "foo bar",
        |    null
        |  ],
        |  "b": [
        |    x"1234ABCD",
        |    19223372036854775807L
        |  ],
        |  "c": [
        |    [
        |      "foo"
        |    ]
        |  ]
        |]
        |""".trimMargin())
        assertThat(GtvParser.parse(res.stdout)).isEqualTo(sampleGtv)
    }

    @Test
    fun `Merkle hash version 1`() {
        val hex = GtvEncoder.encodeGtv(sampleGtv).toHex()
        val res = GtvCommand().test(listOf("--hex", hex, "--hash", "1"))
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.stdout).isEqualTo(sampleGtv.merkleHash(makeMerkleHashCalculator(1)).toHex() + '\n')
        assertThat(res.stdout).isEqualTo("68DBFE35A3E94D9B310515CE02346146C2F20381A212E91682D7F987D710D4A5\n")
    }

    @Test
    fun `Merkle hash version 2`() {
        val hex = GtvEncoder.encodeGtv(sampleGtv).toHex()
        val res = GtvCommand().test(listOf("--hex", hex, "--hash", "2"))
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.stdout).isEqualTo(sampleGtv.merkleHash(makeMerkleHashCalculator(2)).toHex() + '\n')
        assertThat(res.stdout).isEqualTo("E9713A29784BFB58D08CDC1347886E73710237DD692128BFC4BD597E97EE3F6B\n")
    }

    @Test
    fun `hex input on interactive stdin`() {
        val res = GtvCommand().test(listOf("-f", "json"),
                stdin = GtvEncoder.encodeGtv(sampleGtv).toHex(),
                inputInteractive = true)
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.stdout).isEqualTo("""{
        |  "a": [
        |    1,
        |    "foo bar",
        |    null
        |  ],
        |  "b": [
        |    "1234ABCD",
        |    19223372036854775807
        |  ],
        |  "c": [
        |    [
        |      "foo"
        |    ]
        |  ]
        |}
        |""".trimMargin())
    }
}
