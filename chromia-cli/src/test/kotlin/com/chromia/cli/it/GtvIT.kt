package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.time.Duration

class GtvIT {
    private val sampleGtv = gtv(mapOf(
            "a" to gtv(listOf(gtv(1), gtv("foo bar"), GtvNull)),
            "b" to gtv(listOf(gtv("1234ABCD".hexStringToByteArray()), gtv(BigInteger("19223372036854775807"))))
    ))

    @Test
    fun formatHelp() {
        TestProcess.Builder("tools", "gtv", "--help")
                .exitCode(0)
                .timeout(Duration.ofSeconds(10))
                .startCondition("Decode and convert GTV data")
                .start()
    }

    @Test
    fun `binary input on non-interactive stdin`() {
        TestProcess.Builder("tools", "gtv", "-f", "xml")
                .binaryInput(GtvEncoder.encodeGtv(sampleGtv))
                .exitCode(0)
                .wholeOutput("""
                <dict>
                    <entry key="a">
                        <array>
                            <int>1</int>
                            <string>foo bar</string>
                            <null xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                        </array>
                    </entry>
                    <entry key="b">
                        <array>
                            <bytea>1234ABCD</bytea>
                            <bigint>19223372036854775807</bigint>
                        </array>
                    </entry>
                </dict>""".trimIndent())
                .start()
    }
}
