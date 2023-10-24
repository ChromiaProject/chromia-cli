package com.chromia.cli

import com.chromia.cli.util.testData
import com.github.ajalt.clikt.testing.test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class EIFEIFGenerateEventsConfigCommandTest {

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        testData(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()

    }

    @Test
    fun `Test file format validation test`() {
        val testTokenAbi = javaClass.getResource("/EIF/TokenBridge.json")!!.path
        val outputFile = testDir.resolve("events.xml").toFile()
        val rsp = EIFGenerateEventsConfigCommand().test(
                "--abi $testTokenAbi --events=DepositedERC20 --target ${outputFile.absolutePath}")
        assertEquals(rsp.statusCode, 1)
        assertThat(rsp.stderr).contains("Error: invalid value for --target: Unexpected format of target file found: events.xml, expected type: yaml. Change suffix or use --format option")
    }

    @Test
    fun `Test extracting ERC20 events as gtv from abi`(@TempDir tempDir: Path) {
        val testTokenAbi = javaClass.getResource("/EIF/TestToken.json")!!.path
        val outputFile = tempDir.resolve("events.xml").toFile()
        EIFGenerateEventsConfigCommand().test("--abi $testTokenAbi --events Transfer,Approval --target ${outputFile.absolutePath} --format xml")
        val expectedGtvXml = javaClass.getResource("/EIF/erc20_abi_to_gtv.xml")!!.readText()
        assertEquals(expectedGtvXml, outputFile.readText())
    }

    @Test
    fun `Test extracting Token Bridge events as yaml from abi`(@TempDir tempDir: Path) {
        val testTokenAbi = javaClass.getResource("/EIF/TokenBridge.json")!!.path
        val outputFile = tempDir.resolve("events.yaml").toFile()
        EIFGenerateEventsConfigCommand().test("--abi $testTokenAbi --events DepositedERC20 --target ${outputFile.absolutePath} --format yaml")
        assertEquals(expectedGtvYamlDepositedERC20Event, outputFile.readText())
    }

    @Test
    fun `Test extracting from multiple abi sources in directory`(@TempDir tempDir: Path) {
        val testAbiDirSources = javaClass.getResource("/EIF/")!!.path
        val outputFile = tempDir.resolve("events.yaml").toFile()
        EIFGenerateEventsConfigCommand().test("--abi $testAbiDirSources --events 'DepositedERC20,Transfer' --target ${outputFile.absolutePath} --format yaml")
        assertEquals(expectedGtvYamlDepositedERC20AndTransferEvent, outputFile.readText())
    }

    @Test
    fun `Test should throw if not all events are found`(@TempDir tempDir: Path) {
        val testAbiDirSources = javaClass.getResource("/EIF/TestToken.json")!!.path
        val outputFile = tempDir.resolve("events.yaml").toFile()
        val rsp = EIFGenerateEventsConfigCommand().test("--abi $testAbiDirSources --events 'NonExistingEvent,NonExistingEvent2' --target ${outputFile.absolutePath} --format yaml")
        assertThat(rsp.stdout).contains("Events: [NonExistingEvent, NonExistingEvent2] not found in abi source: $testAbiDirSources. Generation of events was aborted")
    }

    val expectedGtvYamlDepositedERC20AndTransferEvent = """
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
              - anonymous: 0
                inputs:
                  - indexed: 1
                    internalType: address
                    name: from
                    type: address
                  - indexed: 1
                    internalType: address
                    name: to
                    type: address
                  - indexed: 0
                    internalType: uint256
                    name: value
                    type: uint256
                name: Transfer
                type: event

    """.trimIndent()

    val expectedGtvYamlDepositedERC20Event = """
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
