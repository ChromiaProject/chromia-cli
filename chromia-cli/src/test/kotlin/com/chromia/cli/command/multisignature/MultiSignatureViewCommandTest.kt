package com.chromia.cli.command.multisignature

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MultiSignatureViewCommandTest {
    private val logger = TerminalRecorder(width = 1000, outputInteractive = false)
    private val testTerminal = Terminal(terminalInterface = logger)

    @Test
    fun printExistingTransaction(@TempDir tempDir: Path) {
        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex")!!.file)

        MultiSignatureViewCommand().context { terminal = testTerminal }.parse(listOf("--file", transactionFile.absolutePath))
        assertThat(logger.output()).isEqualTo("""    
            {
              "blockchainRID": "0505050505050505050505050505050505050505050505050505050505050505",
              "operations": [
                {
                  "operation": "call_op",
                  "arguments": [
                    "string: call_op"
                  ]
                },
                {
                  "operation": "nop",
                  "arguments": [
                    "integer: 1729612840588"
                  ]
                }
              ],
              "signers": [
                "03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05",
                "027C95A328CF7F91EA670D31A527D1C3BA6D04EF72AB4DE034C9C79A74189ECB10"
              ],
              "signatures": [
                "3E77838DF8F7560EA714C67A657E5523FB7405694098B85C0902424968F071A96A26A983D4054C40496E9D4A0130764A977810531601685ACB73DE231A610EE9",
                ""
              ]
            }
            
        """.trimIndent()
        )

    }
}