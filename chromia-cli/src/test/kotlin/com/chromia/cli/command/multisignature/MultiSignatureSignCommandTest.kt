package com.chromia.cli.command.multisignature

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import com.chromia.build.tools.KeyStoreBuilder
import com.chromia.build.tools.TestDataBuilder
import com.chromia.build.tools.testData
import com.github.ajalt.clikt.core.parse
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.gtx.Gtx
import org.junit.jupiter.api.Test
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

        val transaction = tempDir.toFile().listFiles()?.find { it.name.startsWith(MultiSignatureCreateCommandTest.opName) }?.readText()?.hexStringToByteArray()
        val transactionGtx = Gtx.decode(transaction!!)

        assertThat(transactionGtx.gtxBody.blockchainRid).isEqualTo(MultiSignatureCreateCommandTest.testBrid)
        assertThat(transactionGtx.gtxBody.operations.map { it.opName }).containsAll(MultiSignatureCreateCommandTest.opName, "nop")
        assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(transactionGtx.gtxBody.signers.last().toHex()).isEqualTo(MultiSignatureCreateCommandTest.secondSignerPubkey)
        assertThat(transactionGtx.signatures.first().toHex()).isEqualTo("3E77838DF8F7560EA714C67A657E5523FB7405694098B85C0902424968F071A96A26A983D4054C40496E9D4A0130764A977810531601685ACB73DE231A610EE9")
        assertThat(transactionGtx.signatures.last().toHex()).isEqualTo("BDA048E34109986F731A9F1EFD46EA41B6EBFBED60A4DCF1BF24AE6D7D4A7FCB6BA1A5A78C9DEAE27DA6D5D4B15B15AD8E2269A3F733A95EE64A5573BDBF1A7B")
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

        val transaction = tempDir.toFile().listFiles()?.find { it.name.startsWith(MultiSignatureCreateCommandTest.opName) }?.readText()?.hexStringToByteArray()
        val transactionGtx = Gtx.decode(transaction!!)

        assertThat(transactionGtx.gtxBody.blockchainRid).isEqualTo(MultiSignatureCreateCommandTest.testBrid)
        assertThat(transactionGtx.gtxBody.operations.map { it.opName }).containsAll(MultiSignatureCreateCommandTest.opName, "nop")
        assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(transactionGtx.gtxBody.signers.last().toHex()).isEqualTo(MultiSignatureCreateCommandTest.secondSignerPubkey)
        assertThat(transactionGtx.signatures.first().toHex()).isEqualTo("3E77838DF8F7560EA714C67A657E5523FB7405694098B85C0902424968F071A96A26A983D4054C40496E9D4A0130764A977810531601685ACB73DE231A610EE9")
        assertThat(transactionGtx.signatures.last().toHex()).isEqualTo("BDA048E34109986F731A9F1EFD46EA41B6EBFBED60A4DCF1BF24AE6D7D4A7FCB6BA1A5A78C9DEAE27DA6D5D4B15B15AD8E2269A3F733A95EE64A5573BDBF1A7B")
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
