package com.chromia.cli.command.multisignature

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import com.chromia.build.tools.KeyStoreBuilder
import com.chromia.build.tools.TestDataBuilder
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.parse
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import net.postchain.common.toHex
import net.postchain.gtx.Gtx
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

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
        val transactionGtx = Gtx.decode(txData!!.transaction)

        assertThat(transactionGtx.gtxBody.blockchainRid).isEqualTo(MultiSignatureCreateCommandTest.testBrid)
        assertThat(transactionGtx.gtxBody.operations.map { it.opName }).containsAll(MultiSignatureCreateCommandTest.opName, "nop")
        assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(transactionGtx.gtxBody.signers.last().toHex()).isEqualTo(MultiSignatureCreateCommandTest.secondSignerPubkey)
        assertThat(transactionGtx.signatures.first().toHex()).isEqualTo("F06B4C8E4C22126C69A347191A5DC78311F1A94947D19C5C733D0839D7011E9B66F6A0C086CCC38CD899F56958D2B8CB4563735CFB68DBC6F74966D3FC143C01")
        assertThat(transactionGtx.signatures.last().toHex()).isEqualTo("15E5B6972F256F167AB043D3910EBBF3BCA043C51611ECD09CFE2C135BEA17C158C78D0EB71CA12639A2151B7FF925083C100D383BF30671D857CF5057F3EA41")
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
        val transactionGtx = Gtx.decode(txData!!.transaction)

        assertThat(transactionGtx.gtxBody.blockchainRid).isEqualTo(MultiSignatureCreateCommandTest.testBrid)
        assertThat(transactionGtx.gtxBody.operations.map { it.opName }).containsAll(MultiSignatureCreateCommandTest.opName, "nop")
        assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(transactionGtx.gtxBody.signers.last().toHex()).isEqualTo(MultiSignatureCreateCommandTest.secondSignerPubkey)
        assertThat(transactionGtx.signatures.first().toHex()).isEqualTo("F06B4C8E4C22126C69A347191A5DC78311F1A94947D19C5C733D0839D7011E9B66F6A0C086CCC38CD899F56958D2B8CB4563735CFB68DBC6F74966D3FC143C01")
        assertThat(transactionGtx.signatures.last().toHex()).isEqualTo("15E5B6972F256F167AB043D3910EBBF3BCA043C51611ECD09CFE2C135BEA17C158C78D0EB71CA12639A2151B7FF925083C100D383BF30671D857CF5057F3EA41")
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
