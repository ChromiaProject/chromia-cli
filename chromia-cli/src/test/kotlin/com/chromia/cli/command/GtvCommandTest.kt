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
import net.postchain.gtv.parse.GtvParser
import org.junit.jupiter.api.Test
import java.math.BigInteger

class GtvCommandTest {
    private val sampleGtv = gtv(mapOf(
            "a" to gtv(listOf(gtv(1), gtv("foo bar"), GtvNull)),
            "b" to gtv(listOf(gtv("1234ABCD".hexStringToByteArray()), gtv(BigInteger("19223372036854775807"))))
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
        |  ]
        |]
        |""".trimMargin())
        assertThat(GtvParser.parse(res.stdout)).isEqualTo(sampleGtv)
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
        |  ]
        |}
        |""".trimMargin())
    }
}
