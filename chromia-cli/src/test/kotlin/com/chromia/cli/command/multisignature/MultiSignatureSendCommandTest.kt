package com.chromia.cli.command.multisignature

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.KeyStoreBuilder
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.TestModel
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.parse
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.common.tx.TransactionStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class MultiSignatureSendCommandTest {
    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        settingsFile = testDir.resolve("chromia.yml").toFile()
    }

    @Test
    fun partiallySignedTransactionGetsSignedWithClientConfigFromSecretFile(@TempDir tempDir: Path) {
        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex")!!.file)
        val secretFile = tempDir.resolve(".secret").toFile()
        secretFile.writeText("""
                pubkey=${MultiSignatureCreateCommandTest.secondSignerPubkey},
                privkey=${MultiSignatureCreateCommandTest.secondSignerPrivkey},
            """.trimIndent()
        )

        RestApiInstance.withModel(TestModel(MultiSignatureCreateCommandTest.testBrid)) {
            MultiSignatureSendCommand().parse(listOf(
                    "--blockchain-rid", MultiSignatureCreateCommandTest.testBrid.toString(),
                    "--api-url", RestApiInstance.apiUrl,
                    "--file", transactionFile.absolutePath,
                    "--secret", secretFile.absolutePath
            ))
        }
    }

    @Test
    fun partiallySignedTransactionGetsSignedWithClientConfigKeyId(@TempDir tempDir: Path) {
        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex")!!.file)
        KeyStoreBuilder().createFiles(tempDir, MultiSignatureCreateCommandTest.secondSignerKeyPair)

        EnvironmentVariables("CHROMIA_HOME", tempDir.absolutePathString()).execute {
            RestApiInstance.withModel(TestModel(MultiSignatureCreateCommandTest.testBrid)) {
                MultiSignatureSendCommand().parse(listOf(
                        "--blockchain-rid", MultiSignatureCreateCommandTest.testBrid.toString(),
                        "--api-url", RestApiInstance.apiUrl,
                        "--file", transactionFile.absolutePath,
                ))
            }
        }
    }

    @Test
    fun fullySignedTransactionGetsSent(@TempDir tempDir: Path) {
        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex_signed")!!.file)
        val secretFile = tempDir.resolve(".secret").toFile()
        secretFile.writeText(
                """
                    pubkey=${MultiSignatureCreateCommandTest.secondSignerPubkey},
                    privkey=${MultiSignatureCreateCommandTest.secondSignerPrivkey},
                """.trimIndent()
        )

        RestApiInstance.withModel(TestModel(MultiSignatureCreateCommandTest.testBrid)) {
            MultiSignatureSendCommand().parse(listOf(
                    "--blockchain-rid", MultiSignatureCreateCommandTest.testBrid.toString(),
                    "--api-url", RestApiInstance.apiUrl,
                    "--file", transactionFile.absolutePath,
                    "--secret", secretFile.absolutePath
            ))
        }
    }

    @Test
    fun unsupportedTransactionFormat(@TempDir tempDir: Path) {
        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex_signed_unsupported")!!.file)
        val secretFile = tempDir.resolve(".secret").toFile()
        secretFile.writeText(
                """
                    pubkey=${MultiSignatureCreateCommandTest.secondSignerPubkey},
                    privkey=${MultiSignatureCreateCommandTest.secondSignerPrivkey},
                """.trimIndent()
        )

        val res = assertThrows<PrintMessage> {
            RestApiInstance.withModel(TestModel(MultiSignatureCreateCommandTest.testBrid)) {
                MultiSignatureSendCommand().parse(listOf(
                        "--blockchain-rid", MultiSignatureCreateCommandTest.testBrid.toString(),
                        "--api-url", RestApiInstance.apiUrl,
                        "--file", transactionFile.absolutePath,
                        "--secret", secretFile.absolutePath
                ))
            }
        }
        assertThat(res.message).isEqualTo("Transaction file is incompatible with current CLI version")
    }
    @Test
    fun differentTargetBridThanWhatIsConfiguredInTransaction(@TempDir tempDir: Path) {
        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex_signed")!!.file)
        val secretFile = tempDir.resolve(".secret").toFile()
        secretFile.writeText(
                """
                    pubkey=${MultiSignatureCreateCommandTest.secondSignerPubkey},
                    privkey=${MultiSignatureCreateCommandTest.secondSignerPrivkey},
                """.trimIndent()
        )

        val wrongTargetBrid = BlockchainRid.buildRepeat(6).toHex()
        val res = assertThrows<PrintMessage> {
            RestApiInstance.withModel(TestModel(MultiSignatureCreateCommandTest.testBrid)) {
                MultiSignatureSendCommand().parse(listOf(
                        "--blockchain-rid", wrongTargetBrid,
                        "--api-url", RestApiInstance.apiUrl,
                        "--file", transactionFile.absolutePath,
                        "--secret", secretFile.absolutePath
                ))
            }
        }
        assertThat(res.message).isEqualTo("Transaction Failed with code 404: Can't find blockchain with blockchainRID: $wrongTargetBrid")
    }

    @Test
    fun testNoBlockchainsConfiguredError() {
        settingsFile.writeText(
            """
            deployments:
              test-network:
                url:
                  - "http://localhost:7740"
            """.trimIndent()
        )

        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex")!!.file)

        val res = assertThrows<CliktError> {
            MultiSignatureSendCommand().parse(
                    listOf(
                            "--settings", settingsFile.absolutePath,
                            "--network", "test-network",
                            "--file", transactionFile.absolutePath
                    )
            )
        }
        assertThat(res.message!!).isEqualTo("""
                |No blockchains configured for deployment 'test-network'
                |Resolution options:
                |- Add blockchain configurations to your deployment
                |- Choose a different deployment target using --network
            """.trimMargin())
    }

    @Test
    fun testMultipleBlockchainsAvailableError() {
        settingsFile.writeText(
            """
           deployments:
              test_network:
                brid: x"0000000000000000000000000000000000000000000000000000000000000002"
                url:
                  - "http://localhost:7740"
                chains:
                  blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
                  blockchain2: x"0000000000000000000000000000000000000000000000000000000000000003"
            """.trimIndent()
        )

        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex")!!.file)

        val res = assertThrows<CliktError> {
            MultiSignatureSendCommand().parse(
                    listOf(
                            "--settings", settingsFile.absolutePath,
                            "--network", "test_network",
                            "--file", transactionFile.absolutePath
                    )
            )
        }
        assertThat(res.message!!).isEqualTo("""
                     Multiple blockchains available in deployment 'test_network'
                     Available chains: blockchain1, blockchain2
                     Resolution:
                     - Specify the target blockchain using: --blockchain <name>
                         - Example: --blockchain blockchain1
                     """.trimIndent()
        )
    }
}