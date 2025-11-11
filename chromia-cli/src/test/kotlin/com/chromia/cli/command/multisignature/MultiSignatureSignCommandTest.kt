package com.chromia.cli.command.multisignature

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import com.chromia.build.tools.KeyStoreBuilder
import com.chromia.build.tools.TestDataBuilder
import com.chromia.build.tools.multisignature.MultiSignatureTxData
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.parse
import net.postchain.common.toHex
import net.postchain.gtx.Gtx
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class MultiSignatureSignCommandTest {

    private val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex")!!.file)


    @Test
    fun signTransactionWithSecretFile(@TempDir tempDir: Path) {
        val secret = tempDir.resolve(".secret").toFile()
        secret.writeText(
                """
                    pubkey=${MultiSignatureCreateCommandTest.secondSignerPubkey},
                    privkey=${MultiSignatureCreateCommandTest.secondSignerPrivkey},
                """.trimIndent()
        )

        MultiSignatureSignCommand().parse(listOf(
                "--file", transactionFile.absolutePath,
                "--secret", secret.absolutePath,
                "--target", tempDir.toFile().absolutePath
        ))

        val txData = tempDir.toFile().listFiles()?.find { it.name.startsWith(MultiSignatureCreateCommandTest.opName) }?.readText()?.let {
            MultiSignatureTxData.decode(it)
        }
        assertThat(txData!!.txRid).isNotEmpty()

        val transactionGtx = Gtx.decode(txData.transaction)

        assertThat(transactionGtx.gtxBody.blockchainRid).isEqualTo(MultiSignatureCreateCommandTest.testBrid)
        assertThat(transactionGtx.gtxBody.operations.map { it.opName }).containsAll(MultiSignatureCreateCommandTest.opName, "nop")
        assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(transactionGtx.gtxBody.signers.last().toHex()).isEqualTo(MultiSignatureCreateCommandTest.secondSignerPubkey)
        assertThat(transactionGtx.signatures.first().toHex()).isEqualTo("0334F97591929260337B411FE1DA988C3736DA693558DDFE87A007442D1586D44B9EE7776CE19C483548DA1BB357F9387215F277F6C1D536461086BE9CE311EE")
        assertThat(transactionGtx.signatures.last().toHex()).isEqualTo("2E109CB60EA43895363400DC17EB477AFDCA24EC23C48228F07A52889DFC8D8439CDD4B9FC2F76CD0D6544342BBE85A1B684B52760521613D6DCBD698498C1BB")
    }

    @Test
    fun unsupportedTransactionFormat(@TempDir tempDir: Path) {
        val secret = tempDir.resolve(".secret").toFile()
        val unsupportedTransactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex_unsupported")!!.file)
        secret.writeText(
                """
                    pubkey=${MultiSignatureCreateCommandTest.secondSignerPubkey},
                    privkey=${MultiSignatureCreateCommandTest.secondSignerPrivkey},
                """.trimIndent()
        )

        val res = assertThrows<PrintMessage> {
            MultiSignatureSignCommand().parse(listOf(
                    "--file", unsupportedTransactionFile.absolutePath,
                    "--secret", secret.absolutePath,
                    "--target", tempDir.toFile().absolutePath
            ))
        }
        assertThat(res.message).isEqualTo("Transaction file is incompatible with current CLI version")
    }

    @Test
    fun signTransactionWithKeyPairFromConfig(@TempDir tempDir: Path) {

        KeyStoreBuilder().createFiles(tempDir, MultiSignatureCreateCommandTest.secondSignerKeyPair)

        EnvironmentVariables("CHROMIA_HOME", tempDir.absolutePathString()).execute {
            MultiSignatureSignCommand().parse(listOf(
                    "--file", transactionFile.absolutePath,
                    "--target", tempDir.toFile().absolutePath
            ))
        }

        val txData = tempDir.toFile().listFiles()?.find { it.name.startsWith(MultiSignatureCreateCommandTest.opName) }?.readText()?.let {
            MultiSignatureTxData.decode(it)
        }
        assertThat(txData!!.txRid).isNotEmpty()

        val transactionGtx = Gtx.decode(txData.transaction)

        assertThat(transactionGtx.gtxBody.blockchainRid).isEqualTo(MultiSignatureCreateCommandTest.testBrid)
        assertThat(transactionGtx.gtxBody.operations.map { it.opName }).containsAll(MultiSignatureCreateCommandTest.opName, "nop")
        assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(transactionGtx.gtxBody.signers.last().toHex()).isEqualTo(MultiSignatureCreateCommandTest.secondSignerPubkey)
        assertThat(transactionGtx.signatures.first().toHex()).isEqualTo("0334F97591929260337B411FE1DA988C3736DA693558DDFE87A007442D1586D44B9EE7776CE19C483548DA1BB357F9387215F277F6C1D536461086BE9CE311EE")
        assertThat(transactionGtx.signatures.last().toHex()).isEqualTo("2E109CB60EA43895363400DC17EB477AFDCA24EC23C48228F07A52889DFC8D8439CDD4B9FC2F76CD0D6544342BBE85A1B684B52760521613D6DCBD698498C1BB")
    }

    @Test
    fun signTransactionWithKeyPairFromConfigAndRenameOutputFile(@TempDir tempDir: Path) {

        KeyStoreBuilder().createFiles(tempDir, MultiSignatureCreateCommandTest.secondSignerKeyPair)
        EnvironmentVariables("CHROMIA_HOME", tempDir.absolutePathString()).execute {
            MultiSignatureSignCommand().parse(listOf(
                    "--file", transactionFile.absolutePath,
                    "--target", tempDir.toFile().absolutePath,
                    "--file-name", "newTestFile"
            ))
        }
        assertThat(tempDir.resolve("newTestFile")).exists()
    }
}
