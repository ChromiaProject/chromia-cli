package com.chromia.build.tools.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.testData
import com.chromia.cli.model.parseModel
import net.postchain.common.BlockchainRid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class DeploymentModelTest {
    @Test
    fun `can parse hex string of length 64 as brid`(@TempDir dir: Path) {
        testData(dir) {
            config {
                deployments("""
                deployments: 
                    foo:
                      url: "http://foo.com"
                      brid: 615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5
                            """.trimIndent())
            }
        }
        val model = parseModel(dir.resolve("chromia.yml").toFile())
        assertThat(model.deployments["foo"]?.blockchainRid).isEqualTo(BlockchainRid.buildFromHex("615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"))
    }

    @Test
    fun `can parse bytearray as brid`(@TempDir dir: Path) {
        testData(dir) {
            config {
                deployments("""
                deployments: 
                    foo:
                      url: "http://foo.com"
                      brid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                     """.trimIndent())
            }
        }
        val model = parseModel(dir.resolve("chromia.yml").toFile())
        assertThat(model.deployments["foo"]?.blockchainRid).isEqualTo(BlockchainRid.buildFromHex("615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"))
    }

    @Test
    fun `can parse both formats for brid chains in deployed chains`(@TempDir dir: Path) {
        testData(dir) {
            config {
                deployments("""
                deployments:
                    foo:
                        url: "http://foo.com"
                        brid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                        chains:
                            bc1: 615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5
                            bc2: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                """.trimIndent())
            }
        }
        val model = parseModel(dir.resolve("chromia.yml").toFile())
        assertThat(model.deployments["foo"]!!.chains.keys.size).isEqualTo(2)
    }
}