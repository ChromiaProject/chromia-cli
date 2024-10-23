package com.chromia.cli.command.multisignature

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.chromia.build.tools.TestDataBuilder
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.restapi.withClusterManagement
import com.chromia.build.tools.testData
import com.chromia.cli.util.TestClusterManagement
import com.chromia.directory1.lib.ft4.core.accounts.AuthType
import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.chromia.directory1.lib.ft4.external.accounts.GET_ACCOUNTS_BY_SIGNER
import com.chromia.directory1.lib.ft4.external.accounts.GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER
import com.chromia.directory1.lib.ft4.external.auth.GET_AUTH_FLAGS
import com.chromia.directory1.lib.ft4.utils.PagedResult
import com.chromia.directory1.lib.ft4.version.GET_VERSION
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.parse
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import net.postchain.api.rest.controller.Model
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.common.hexStringToByteArray
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.toHex
import net.postchain.common.wrap
import net.postchain.crypto.KeyPair
import net.postchain.crypto.PubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtv.mapper.GtvObjectMapper
import net.postchain.gtx.Gtx
import net.postchain.gtx.GtxQuery
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class Ft4Model(val model: Model, val version: String, val responses: Map<String, Gtv> = mapOf()) : Model by model {

    constructor(version: String, responses: Map<String, Gtv>) : this(TestModel(), version, responses)

    val ft4AccountId = "4".repeat(64).hexStringToWrappedByteArray()
    val ftAuthDescriptorId = "4".repeat(64)
    override fun query(query: GtxQuery): Gtv {

        val pagedResult = PagedResult(null, data = listOf(gtv(mapOf("id" to gtv(ft4AccountId)))))

        return when (query.name) {
            GET_VERSION -> gtv(version)
            GET_ACCOUNTS_BY_SIGNER -> GtvObjectMapper.toGtvDictionary(pagedResult)
            GET_AUTH_FLAGS -> gtv(gtv("A"))
            GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER -> getAccountAuthDescriptorBySigner(query.args)
            else -> responses[query.name] ?: throw UserMistake("Query ${query.name} not found in Ft4Model")
        }
    }

    private fun getAccountAuthDescriptorBySigner(args: Gtv): Gtv {
        val accountId = args.asDict().get("account_id") ?: throw PrintMessage("AcountId not defined in Gtv")
        val signer = args.asDict().get("signer") ?: throw PrintMessage("Signer not defined in Gtv")
        val authDescriptor = Ft4GetAccountAuthDescriptorsBySignerResult(
                id = ftAuthDescriptorId.hexStringToWrappedByteArray(),
                accountId = accountId.asByteArray().wrap(),
                authType = AuthType.valueOf("S"),
                args = gtv(gtv(gtv("A")), gtv(signer.asByteArray().wrap())),
                rules = GtvNull,
                created = System.currentTimeMillis(),
        )
        return gtv(GtvObjectMapper.toGtvDictionary(authDescriptor))
    }
}

class MultiSignatureCreateCommandTest {

    @Test
    fun createInitialTransactionWithSecretFile(@TempDir tempDir: Path) {
        testData(tempDir) {
            secret { secretFile(tempDir) }
        }

        val signersFile = tempDir.resolve("signers").toFile()
        signersFile.writeText(
                """
                    pubkey1=$secondSignerPubkey,
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

        val transaction = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }?.readText()?.hexStringToByteArray()
        val transactionGtx = Gtx.decode(transaction!!)

        assertThat(transactionGtx.gtxBody.blockchainRid).isEqualTo(testBrid)
        assertThat(transactionGtx.gtxBody.operations.map { it.opName }).containsAll(opName, "nop")
        assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(transactionGtx.gtxBody.signers.last().toHex()).isEqualTo(secondSignerPubkey)
        assertThat(transactionGtx.signatures.size).isEqualTo(2)
        //Can't assert on expected signature because nop transaction changes signature
        assertThat(transactionGtx.signatures.first().toHex().length).isEqualTo(ByteArray(64).toHex().length)
        assertThat(transactionGtx.signatures.last()).isEmpty()
    }

    @Test
    fun createInitialTransactionWithKeyPairFromConfig(@TempDir tempDir: Path) {
        testData(tempDir) {
            keyStore()
        }

        val signersFile = tempDir.resolve("signers").toFile()
        signersFile.writeText(
                """
                    pubkey1=$secondSignerPubkey,
                """.trimIndent()
        )

        val settingsFile = tempDir.resolve("chromia.yml").toFile()
        val opName = "call_op"

        EnvironmentVariables("CHROMIA_HOME", tempDir.absolutePathString()).execute {
            MultiSignatureCreateCommand().parse(listOf(
                    "--settings", settingsFile.absolutePath,
                    "--signers-file", signersFile.absolutePath,
                    "--blockchain-rid", testBrid.toString(),
                    "--target", tempDir.absolutePathString(),
                    "call_op", opName
            ))
        }


        val transaction = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }?.readText()?.hexStringToByteArray()
        val transactionGtx = Gtx.decode(transaction!!)

        assertThat(transactionGtx.gtxBody.blockchainRid).isEqualTo(testBrid)
        assertThat(transactionGtx.gtxBody.operations.map { it.opName }).containsAll(opName, "nop")
        assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(transactionGtx.gtxBody.signers.last().toHex()).isEqualTo(secondSignerPubkey)
        assertThat(transactionGtx.signatures.size).isEqualTo(2)
        //Can't assert on expected signature because nop transaction changes signature
        assertThat(transactionGtx.signatures.first().toHex().length).isEqualTo(ByteArray(64).toHex().length)
        assertThat(transactionGtx.signatures.last()).isEmpty()
    }

    @Test
    fun createInitialTransactionWithFt4(@TempDir tempDir: Path) {
        testData(tempDir) {
            secret { secretFile(tempDir) }
        }
        val secondSignerPubKey = PubKey("2".repeat(64).hexStringToWrappedByteArray())
        val signersFile = tempDir.resolve("signers").toFile()
        signersFile.writeText(
                """
                    pubkey1=${secondSignerPubKey.data.toHex()},
                """.trimIndent()
        )
        val settingsFile = tempDir.resolve("chromia.yml").toFile()
        val secret = tempDir.resolve(".secret").toFile()
        val opName = "call_op"

        val ft4Model = Ft4Model(
                "0.4.0",
                mapOf()
        )

        withModel(ft4Model) {
            MultiSignatureCreateCommand().parse(listOf(
                    "--settings", settingsFile.absolutePath,
                    "--signers-file", signersFile.absolutePath,
                    "--blockchain-rid", ft4Model.blockchainRid.toHex(),
                    "--api-url", RestApiInstance.apiUrl,
                    "--secret", secret.absolutePath,
                    "--target", tempDir.absolutePathString(),
                    "--ft-auth",
                    "--auth-descriptor-id", ft4Model.ftAuthDescriptorId,
                    "call_op", opName
            ))
        }

        val transaction = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }?.readText()?.hexStringToByteArray()
        val transactionGtx = Gtx.decode(transaction!!)
        val ft4Operations = transactionGtx.gtxBody.operations.filter { it.opName == "ft4.ft_auth" }

        assertThat(transactionGtx.gtxBody.blockchainRid.toHex()).isEqualTo(ft4Model.blockchainRid.toHex())
        assertThat(transactionGtx.gtxBody.operations.map { it.opName }).containsExactlyInAnyOrder("ft4.ft_auth", "call_op", "nop")

        assertThat(ft4Operations.first().args.first().asByteArray().toHex()).isEqualTo(ft4Model.ft4AccountId.toHex())
        assertThat(ft4Operations.first().args.last().asByteArray().toHex()).isEqualTo(ft4Model.ftAuthDescriptorId)

        assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(transactionGtx.gtxBody.signers.last().toHex()).isEqualTo(secondSignerPubKey.data.toHex())
        assertThat(transactionGtx.signatures.size).isEqualTo(2)
        //Can't assert on expected signature because nop transaction changes signature
        assertThat(transactionGtx.signatures.first().toHex().length).isEqualTo(ByteArray(64).toHex().length)
        assertThat(transactionGtx.signatures.last()).isEmpty()
    }

    @Test
    fun throwsWhenAuthDescriptorIdIsNotSetWhenUsingFtAuth(@TempDir tempDir: Path) {
        testData(tempDir) {
            secret { secretFile(tempDir) }
        }
        val secondSignerPubKey = PubKey("2".repeat(64).hexStringToWrappedByteArray())
        val signersFile = tempDir.resolve("signers").toFile()
        signersFile.writeText(
                """
                    pubkey1=${secondSignerPubKey.data.toHex()},
                """.trimIndent()
        )
        val settingsFile = tempDir.resolve("chromia.yml").toFile()
        val secret = tempDir.resolve(".secret").toFile()
        val opName = "call_op"

        val ft4Model = Ft4Model(
                "0.4.0",
                mapOf()
        )

        withModel(ft4Model) {
            val res = assertThrows<IllegalArgumentException> {
                MultiSignatureCreateCommand().parse(listOf(
                        "--settings", settingsFile.absolutePath,
                        "--signers-file", signersFile.absolutePath,
                        "--blockchain-rid", ft4Model.blockchainRid.toHex(),
                        "--api-url", RestApiInstance.apiUrl,
                        "--secret", secret.absolutePath,
                        "--target", tempDir.absolutePathString(),
                        "--ft-auth",
                        "call_op", opName
                ))
            }
            assertThat(res.message).isEqualTo("Must specify auth descriptor id when using ft auth for multi signature")
        }
    }

    @Test
    fun shouldThrowIfNoInitialSignerIsFound(@TempDir tempDir: Path) {
        testData(tempDir)
        val signersFile = tempDir.resolve("signers").toFile()
        signersFile.writeText(
                """
                    pubkey1=$secondSignerPubkey,
                """.trimIndent()
        )
        val settingsFile = tempDir.resolve("chromia.yml").toFile()
        val opName = "call_op"


        val res = assertThrows<IllegalArgumentException> {
            MultiSignatureCreateCommand().parse(listOf(
                    "--settings", settingsFile.absolutePath,
                    "--signers-file", signersFile.absolutePath,
                    "--blockchain-rid", testBrid.toString(),
                    "--target", tempDir.absolutePathString(),
                    "call_op", opName
            ))
        }
        assertThat(res.message).isEqualTo("No initial signer found. Either set one in your configuration or specify path to secret file")
    }

    @Test
    fun createTransactionFromDeploymentSetting(@TempDir tempDir: Path) {
        val model = DirectoryChainModel().withClusterManagement(TestClusterManagement())
        withModel(model) {

            val deploymentBrid = "1111111111111111111111111111111111111111111111111111111111111111"
            testData(tempDir) {
                config {
                    blockchains("""
                    blockchains:
                      foo:
                        module: main
                """.trimIndent())
                    deployments("""
                    deployments:
                        test:
                          url: "http://localhost:7745"
                          brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                          container: test_container
                          chains:
                            foo: x"$deploymentBrid"
                """.trimIndent())
                }
                secret {
                    secretFile(tempDir)
                }
            }
            val signersFile = tempDir.resolve("signers").toFile()
            signersFile.writeText(
                    """
                    pubkey1=$secondSignerPubkey,
                """.trimIndent()
            )

            val settingsFile = tempDir.resolve("chromia.yml").toFile()
            val secret = tempDir.resolve(".secret").toFile()
            val opName = "call_op"

            MultiSignatureCreateCommand().parse(listOf(
                    "--settings", settingsFile.absolutePath,
                    "--blockchain", "foo",
                    "--network", "test",
                    "--secret", secret.absolutePath,
                    "--signers-file", signersFile.absolutePath,
                    "--target", tempDir.absolutePathString(),
                    "call_op", opName
            ))

            val transaction = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }?.readText()?.hexStringToByteArray()
            val transactionGtx = Gtx.decode(transaction!!)

            assertThat(transactionGtx.gtxBody.blockchainRid.toHex()).isEqualTo(deploymentBrid)
            assertThat(transactionGtx.gtxBody.operations.map { it.opName }).containsAll(opName, "nop")
            assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
            assertThat(transactionGtx.gtxBody.signers.last().toHex()).isEqualTo(secondSignerPubkey)
            assertThat(transactionGtx.signatures.size).isEqualTo(2)
            //Can't assert on expected signature because nop transaction changes signature
            assertThat(transactionGtx.signatures.first().toHex().length).isEqualTo(ByteArray(64).toHex().length)
            assertThat(transactionGtx.signatures.last()).isEmpty()
        }


    }

    @Test
    fun testHandlingWithFaultySignerFile(@TempDir tempDir: Path) {
        testData(tempDir) {
            secret { secretFile(tempDir) }
        }
        val signersFile = tempDir.resolve("signers").toFile()
        signersFile.writeText(
                """
                    pubkey1=a,
                """.trimIndent()
        )
        val settingsFile = tempDir.resolve("chromia.yml").toFile()
        val secret = tempDir.resolve(".secret").toFile()
        val opName = "call_op"


        val res = assertThrows<PrintMessage> {
            MultiSignatureCreateCommand().parse(listOf(
                    "--settings", settingsFile.absolutePath,
                    "--signers-file", signersFile.absolutePath,
                    "--blockchain-rid", testBrid.toString(),
                    "--target", tempDir.absolutePathString(),
                    "--secret", secret.absolutePath,
                    "call_op", opName
            ))
        }
        assertThat(res.message).isEqualTo("Failed to add signer for value: a, reason: Invalid hex string: length is not an even number. Please verify that your signers file is defined correctly")
    }

    @Test
    fun testHandlingOfDuplicateSignerPubkey(@TempDir tempDir: Path) {
        testData(tempDir) {
            secret { secretFile(tempDir) }
        }
        val signersFile = tempDir.resolve("signers").toFile()
        signersFile.writeText(
                """
                    pubkey1=$secondSignerPubkey,
                    pubkey2=$secondSignerPubkey,
                """.trimIndent()
        )
        val settingsFile = tempDir.resolve("chromia.yml").toFile()
        val secret = tempDir.resolve(".secret").toFile()
        val opName = "call_op"

        MultiSignatureCreateCommand().parse(listOf(
                "--settings", settingsFile.absolutePath,
                "--signers-file", signersFile.absolutePath,
                "--blockchain-rid", testBrid.toString(),
                "--target", tempDir.absolutePathString(),
                "--secret", secret.absolutePath,
                "call_op", opName
        ))

        val transaction = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }?.readText()?.hexStringToByteArray()
        val transactionGtx = Gtx.decode(transaction!!)
        assertThat(transactionGtx.gtxBody.signers.size).isEqualTo(2)
        assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(transactionGtx.gtxBody.signers.last().toHex()).isEqualTo(secondSignerPubkey)
        //Can't assert on expected signature because nop transaction changes signature
        assertThat(transactionGtx.signatures.size).isEqualTo(2)
        assertThat(transactionGtx.signatures.first().toHex().length).isEqualTo(ByteArray(64).toHex().length)
        assertThat(transactionGtx.signatures.last()).isEmpty()
    }

    @Test
    fun testHandlingWhenSignerIsSameAsInitialSigner(@TempDir tempDir: Path) {
        testData(tempDir) {
            secret { secretFile(tempDir) }
        }
        val signersFile = tempDir.resolve("signers").toFile()
        signersFile.writeText(
                """
                    pubkey1=${TestDataBuilder.keyPair.pubKey},
                """.trimIndent()
        )
        val settingsFile = tempDir.resolve("chromia.yml").toFile()
        val secret = tempDir.resolve(".secret").toFile()
        val opName = "call_op"

        MultiSignatureCreateCommand().parse(listOf(
                "--settings", settingsFile.absolutePath,
                "--signers-file", signersFile.absolutePath,
                "--blockchain-rid", testBrid.toString(),
                "--target", tempDir.absolutePathString(),
                "--secret", secret.absolutePath,
                "call_op", opName
        ))

        val transaction = tempDir.toFile().listFiles()?.find { it.name.startsWith(opName) }?.readText()?.hexStringToByteArray()
        val transactionGtx = Gtx.decode(transaction!!)
        assertThat(transactionGtx.gtxBody.signers.size).isEqualTo(1)
        assertThat(transactionGtx.gtxBody.signers.first().toHex()).isEqualTo(TestDataBuilder.keyPair.pubKey.data.toHex())
        assertThat(transactionGtx.signatures.size).isEqualTo(1)
        //Can't assert on expected signature because nop transaction changes signature
        assertThat(transactionGtx.signatures.first().toHex().length).isEqualTo(ByteArray(64).toHex().length)
    }

    companion object {
        val secondSignerPubkey = "027C95A328CF7F91EA670D31A527D1C3BA6D04EF72AB4DE034C9C79A74189ECB10"
        val secondSignerPrivkey = "DC166B6A1F80C7BC653CB795F969BD20EFF31CABED1AE9A6767871249C830B29"
        val secondSignerKeyPair = KeyPair.of(secondSignerPubkey, secondSignerPrivkey)
        val testBrid = BlockchainRid.buildRepeat(5)
        val opName = "call_op"
    }
}
