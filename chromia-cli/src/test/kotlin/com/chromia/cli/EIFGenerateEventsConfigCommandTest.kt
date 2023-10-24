package com.chromia.cli
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.cli.util.testData
import com.github.ajalt.clikt.core.BadParameterValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class EIFGenerateEventsConfigCommandTest {

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    @BeforeEach
    fun setup() {
        testData(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()

    }

    @Test
    fun `Test extracting Token Bridge events with default options`() {
        val testTokenAbi = javaClass.getResource("/EIF/TokenBridge.json").path
        GenerateEventsConfigCommand().parse(
                listOf(
                        "--settings",
                        settingsFile.absolutePath,
                        "--abi",
                        testTokenAbi,
                        "--events",
                        "DepositedERC20",
                )
        )
        val outputFile = testDir.resolve("${testDir.toAbsolutePath()}/build/events.yaml").toFile()
        assertEquals(expectedGtvYaml, outputFile.readText())
    }

    @Test
    fun `Test file format validation test`() {
        val testTokenAbi = javaClass.getResource("/EIF/TokenBridge.json").path
        val outputFile = testDir.resolve("events.xml").toFile()

        val error = assertThrows<BadParameterValue> {
            GenerateEventsConfigCommand().parse(
                    listOf(
                            "--settings",
                            settingsFile.absolutePath,
                            "--abi",
                            testTokenAbi,
                            "--events",
                            "DepositedERC20",
                            "--target",
                            outputFile.absolutePath
                    )
            )
        }
        assertThat(error.message).isEqualTo("Unexpected format of target file found: events.xml, expected type: yaml. " +
                                                    "Change suffix or use --format option")
    }

    @Test
    fun `Test extracting ERC20 events as gtv from abi`(@TempDir tempDir: Path) {

        val testTokenAbi = javaClass.getResource("/EIF/TestToken.json").path
        val outputFile = tempDir.resolve("events.xml").toFile()
        GenerateEventsConfigCommand().parse(
                listOf(
                        "--settings",
                        settingsFile.absolutePath,
                        "--abi",
                        testTokenAbi,
                        "--events",
                        "Transfer,Approval",
                        "--target",
                        outputFile.absolutePath,
                        "--format",
                        "xml"
                )
        )
        val expectedGtvXml = javaClass.getResource("/EIF/erc20_abi_to_gtv.xml").readText()

        assertEquals(expectedGtvXml, outputFile.readText())
    }

    @Test
    fun `Test extracting Token Bridge events as yaml from abi`(@TempDir tempDir: Path) {
        val testTokenAbi = javaClass.getResource("/EIF/TokenBridge.json").path
        val outputFile = tempDir.resolve("events.yaml").toFile()
        GenerateEventsConfigCommand().parse(
                listOf(
                        "--settings",
                        settingsFile.absolutePath,
                        "--abi",
                        testTokenAbi,
                        "--events",
                        "DepositedERC20",
                        "--target",
                        outputFile.absolutePath,
                        "--format",
                        "yaml"
                )
        )
        assertEquals(expectedGtvYaml, outputFile.readText())
    }


    val expectedGtvYaml = """
            ---
              - anonymous: 0
                inputs:
                  - indexed: 1
                    internalType: address
                    name: sender
                    type: address
                  - indexed: 1
                    internalType: contract IERC20Upgradeable
                    name: token
                    type: address
                  - indexed: 1
                    internalType: bytes32
                    name: ft3_account_id
                    type: bytes32
                  - indexed: 0
                    internalType: uint256
                    name: networkId
                    type: uint256
                  - indexed: 0
                    internalType: uint256
                    name: amount
                    type: uint256
                  - indexed: 0
                    internalType: string
                    name: name
                    type: string
                  - indexed: 0
                    internalType: string
                    name: symbol
                    type: string
                  - indexed: 0
                    internalType: uint8
                    name: decimals
                    type: uint8
                name: DepositedERC20
                type: event

        """.trimIndent()
}
