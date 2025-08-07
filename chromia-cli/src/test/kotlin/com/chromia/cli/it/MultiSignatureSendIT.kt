package com.chromia.cli.it

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.chromia.build.tools.TestDataBuilder
import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.testData
import com.chromia.cli.command.multisignature.MultiSignatureCreateCommand
import com.chromia.cli.command.multisignature.MultiSignatureSignCommand
import com.chromia.cli.command.multisignature.MultiSignatureTxData
import com.github.ajalt.clikt.core.parse
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.absolutePathString
import net.postchain.common.BlockchainRid
import net.postchain.common.toHex
import net.postchain.gtx.Gtx
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MultiSignatureSendIT {
    @Test
    fun sendMultiSignedTransaction(@TempDir tempDir: Path) {

        val secondSignerPubkey = "0389A330A3549B54CFFEEC475729E8176DD8AD8EA206AB4591A27D7A4554B8EC28"
        val secondSignerPrivkey = "88F71D48430A63CE2C8E21E8D0CFF200D75E81E259C34A74D9483622F8B62086"
        val testBrid = BlockchainRid.buildRepeat(5)

        testData(tempDir) {
            secret { secretFile(tempDir) }
        }

        val signersFile = tempDir.resolve("signers").toFile()
        signersFile.writeText(
                """
                    pubkey1=${secondSignerPubkey},
                """.trimIndent()
        )

        val settingsFile = tempDir.resolve("chromia.yml").toFile()
        val secret = tempDir.resolve(".secret").toFile()
        val opName = "call_op"

        MultiSignatureCreateCommand().parse(listOf(
                "--settings", settingsFile.absolutePath,
                "--signers-file", signersFile.absolutePath,
                "--blockchain-rid", testBrid.toString(),
                "--secret", secret.absolutePath,
                "--target", tempDir.absolutePathString(),
                "call_op", opName
        ))


        val createdTransactionFile = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }
        val txData = createdTransactionFile?.readText()?.let {
            MultiSignatureTxData.decode(it)
        }
        val createdTransactionGtx = Gtx.decode(txData!!.transaction)

        assertThat(createdTransactionGtx.gtxBody.blockchainRid).isEqualTo(testBrid)
        assertThat(createdTransactionGtx.gtxBody.operations.map { it.opName }).containsAll(opName, "nop")
        assertThat(createdTransactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(createdTransactionGtx.gtxBody.signers.last().toHex()).isEqualTo(secondSignerPubkey)
        assertThat(createdTransactionGtx.signatures.size).isEqualTo(2)
        //Can't assert on expected signature because nop transaction changes signature
        assertThat(createdTransactionGtx.signatures.first().toHex().length).isEqualTo(ByteArray(64).toHex().length)
        assertThat(createdTransactionGtx.signatures.last()).isEmpty()


        val secretForSecondSigner = tempDir.resolve(".secret_signer_2").toFile()
        secretForSecondSigner.writeText(
                """
                    pubkey=${secondSignerPubkey},
                    privkey=${secondSignerPrivkey},
                """.trimIndent()
        )

        MultiSignatureSignCommand().parse(listOf(
                "--file", createdTransactionFile.absolutePath,
                "--secret", secretForSecondSigner.absolutePath,
                "--target", tempDir.toFile().absolutePath
        ))
        createdTransactionFile.delete()

        val createdSignedTransactionFile = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }
        val signedTransaction = createdSignedTransactionFile?.readText()?.let {
            MultiSignatureTxData.decode(it)
        }
        val signedTransactionGtx = Gtx.decode(signedTransaction!!.transaction)

        assertThat(signedTransactionGtx.gtxBody.blockchainRid).isEqualTo(testBrid)
        assertThat(signedTransactionGtx.gtxBody.operations.map { it.opName }).containsAll(opName, "nop")
        assertThat(signedTransactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(signedTransactionGtx.gtxBody.signers.last().toHex()).isEqualTo(secondSignerPubkey)
        assertThat(signedTransactionGtx.signatures.size).isEqualTo(2)
        assertThat(signedTransactionGtx.signatures.first().toHex()).isEqualTo(createdTransactionGtx.signatures.first().toHex())
        //Can't assert on expected signature because nop transaction changes signature
        assertThat(signedTransactionGtx.signatures.last().toHex().length).isEqualTo(ByteArray(64).toHex().length)


        val txRecorderModel = TxRecorderModel(testBrid)
        withModel(
                Directory1Model(BlockchainRid.ZERO_RID, mapOf(testBrid to listOf(RestApiInstance.apiUrl))),
                txRecorderModel
        ) {
            TestProcess.Builder("multi-signature", "send",
                    "--blockchain-rid", testBrid.toString(),
                    "--api-url", RestApiInstance.apiUrl,
                    "--file", createdSignedTransactionFile.absolutePath,
                    "--secret", secretForSecondSigner.absolutePath,
                    "--no-await"
            )
                    .setWorkingDir(tempDir.toFile())
                    .awaitCompletion(false)
                    .start { process ->
                        process.waitUntil("was posted WAITING: OK", Duration.ofSeconds(10))
                    }
        }


        val sentTxGtx = Gtx.decode(txRecorderModel.txList.single())
        assertThat(sentTxGtx.gtxBody.blockchainRid).isEqualTo(testBrid)
        assertThat(sentTxGtx.gtxBody.operations.map { it.opName }).containsAll(opName, "nop")
        assertThat(sentTxGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(sentTxGtx.gtxBody.signers.last().toHex()).isEqualTo(secondSignerPubkey)
        assertThat(sentTxGtx.signatures.size).isEqualTo(2)
        assertThat(sentTxGtx.signatures.first().toHex()).isEqualTo(signedTransactionGtx.signatures.first().toHex())
        assertThat(sentTxGtx.signatures.last().toHex()).isEqualTo(signedTransactionGtx.signatures.last().toHex())
    }

    @Test
    fun `should infer blockchain when only one blockchain exists under deployments`(@TempDir tempDir: Path) {
        val secondSignerPubkey = "0389A330A3549B54CFFEEC475729E8176DD8AD8EA206AB4591A27D7A4554B8EC28"
        val secondSignerPrivkey = "88F71D48430A63CE2C8E21E8D0CFF200D75E81E259C34A74D9483622F8B62086"
        val testBrid = BlockchainRid.buildRepeat(5)

        testData(tempDir) {
            config {
                deployments("""
                    deployments:
                      test:
                        url: "${RestApiInstance.apiUrl}"
                        brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                        container: testcontainer
                        chains:
                          hello: x"${testBrid.toHex()}"
                        """.trimIndent()
                )
            }
            secret { secretFile(tempDir) }
        }

        val signersFile = tempDir.resolve("signers").toFile()
        signersFile.writeText("pubkey1=${secondSignerPubkey},")

        val settingsFile = tempDir.resolve("chromia.yml").toFile()
        val secret = tempDir.resolve(".secret").toFile()
        val opName = "call_op"

        MultiSignatureCreateCommand().parse(listOf(
                "--settings", settingsFile.absolutePath,
                "--signers-file", signersFile.absolutePath,
                "--blockchain-rid", testBrid.toString(),
                "--secret", secret.absolutePath,
                "--target", tempDir.absolutePathString(),
                "call_op", opName
        ))

        val createdTransactionFile = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }!!
        val secretForSecondSigner = tempDir.resolve(".secret_signer_2").toFile()
        secretForSecondSigner.writeText("""
            pubkey=${secondSignerPubkey},
            privkey=${secondSignerPrivkey},
        """.trimIndent())

        MultiSignatureSignCommand().parse(listOf(
                "--file", createdTransactionFile.absolutePath,
                "--secret", secretForSecondSigner.absolutePath,
                "--target", tempDir.toFile().absolutePath
        ))
        createdTransactionFile.delete()

        val signedTransactionFile = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }!!

        TestProcess.Builder("multi-signature", "send", "--file", signedTransactionFile.absolutePath, "--no-await", "--network", "test")
                .setWorkingDir(tempDir.toFile())
                .exitCode(0)
                .awaitCompletion(false)
                .start()
    }

    @Test
    fun `should fail to send when blockchain cannot be inferred from deployment without chains configuration`(@TempDir tempDir: Path) {
        val secondSignerPubkey = "0389A330A3549B54CFFEEC475729E8176DD8AD8EA206AB4591A27D7A4554B8EC28"
        val secondSignerPrivkey = "88F71D48430A63CE2C8E21E8D0CFF200D75E81E259C34A74D9483622F8B62086"
        val testBrid = BlockchainRid.buildRepeat(5)

        testData(tempDir) {
            config {
                deployments("""
                    deployments:
                      test:
                        url: "${RestApiInstance.apiUrl}"
                        brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                        container: testcontainer
                        """.trimIndent()
                )
            }
            secret { secretFile(tempDir) }
        }

        val signersFile = tempDir.resolve("signers").toFile()
        signersFile.writeText("pubkey1=${secondSignerPubkey},")

        val settingsFile = tempDir.resolve("chromia.yml").toFile()
        val secret = tempDir.resolve(".secret").toFile()
        val opName = "call_op"

        MultiSignatureCreateCommand().parse(listOf(
                "--settings", settingsFile.absolutePath,
                "--signers-file", signersFile.absolutePath,
                "--blockchain-rid", testBrid.toString(),
                "--secret", secret.absolutePath,
                "--target", tempDir.absolutePathString(),
                "call_op", opName
        ))

        val createdTransactionFile = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }!!
        val secretForSecondSigner = tempDir.resolve(".secret_signer_2").toFile()
        secretForSecondSigner.writeText("""
            pubkey=${secondSignerPubkey},
            privkey=${secondSignerPrivkey},
        """.trimIndent())

        MultiSignatureSignCommand().parse(listOf(
                "--file", createdTransactionFile.absolutePath,
                "--secret", secretForSecondSigner.absolutePath,
                "--target", tempDir.toFile().absolutePath
        ))
        createdTransactionFile.delete()

        val signedTransactionFile = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }!!

        TestProcess.Builder("multi-signature", "send", "--file", signedTransactionFile.absolutePath, "--no-await", "--network", "test")
                .setWorkingDir(tempDir.toFile())
                .exitCode(3)
                .partialOutput("No blockchain specified and no default blockchain found in deployment configuration")
                .start()
    }
}
