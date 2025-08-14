package com.chromia.cli.command.multisignature

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class MultiSignatureViewCommandTest {
    private val logger = TerminalRecorder(width = 1000, outputInteractive = false)
    private val testTerminal = Terminal(terminalInterface = logger)

    @Test
    fun printExistingTransaction(@TempDir tempDir: Path) {
        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex")!!.file)

        MultiSignatureViewCommand().context { terminal = testTerminal }.parse(listOf("--file", transactionFile.absolutePath))
        assertThat(logger.output()).isEqualTo("""    
            {
              "transactionRID": "A96D1B62A761ACFC9FBC25E1AF55C4DCA483653DD2A45CA95D525C4FD7911335",
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
                    "integer: 1742972037215"
                  ]
                }
              ],
              "signers": [
                "03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05",
                "027C95A328CF7F91EA670D31A527D1C3BA6D04EF72AB4DE034C9C79A74189ECB10"
              ],
              "signatures": [
                "0334F97591929260337B411FE1DA988C3736DA693558DDFE87A007442D1586D44B9EE7776CE19C483548DA1BB357F9387215F277F6C1D536461086BE9CE311EE",
                ""
              ]
            }
            
        """.trimIndent()
        )
    }

    @Test
    fun unsupportedTransactionFormat(@TempDir tempDir: Path) {
        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex_unsupported")!!.file)
        val res = assertThrows<PrintMessage> {
            MultiSignatureViewCommand().context { terminal = testTerminal }.parse(listOf("--file", transactionFile.absolutePath))
        }
        assertThat(res.message).isEqualTo("Transaction file is incompatible with current CLI version")
    }

    @Test
    fun printExistingTransactionNestedArrayOperation(@TempDir tempDir: Path) {
        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex_array")!!.file)

        MultiSignatureViewCommand().context { terminal = testTerminal }.parse(listOf("--file", transactionFile.absolutePath))
        assertThat(logger.output()).isEqualTo("""    
{
  "transactionRID": "C619215A032214EDC905607F7648B7A80F1C4A5823902425D0FB24E4DC723DDB",
  "blockchainRID": "C7D5D9E5222E8AF3F13FE973581CAA78C7824E10D23A247C3DA9A5F7AA9E417F",
  "operations": [
    {
      "operation": "ft4.ft_auth",
      "arguments": [
        "byteArray: 885A6648772AB9615CCD88D25201402F734B43EF20DA959623591B0DCA5098C5",
        "byteArray: E6D0B18FD6BEE8EBE8E514DF55B46E5977FC4B3E16569B9D5A3C62FE050124A3"
      ]
    },
    {
      "operation": "ft4.update_main_auth_descriptor",
      "arguments": [
        [
          "integer: 1",
          [
            [
              "string: A",
              "string: T"
            ],
            "integer: 2",
            [
              "byteArray: 03F5A2656BC6DB03A8F85C2FD3FD32D72515D1435BB0869A28FFB49185E23E25D6",
              "byteArray: 02AB13E8808E8AC7A8CAD65660A189FF3744063D6B48C4BC5236F44F98A333CD41",
              "byteArray: 03B7EF6C5948022298051E57521CE56075A16A883516240021BD337C8A6E7DDD13"
            ]
          ],
          "null"
        ]
      ]
    },
    {
      "operation": "nop",
      "arguments": [
        "integer: 1731495874274"
      ]
    }
  ],
  "signers": [
    "022672944E1D542487601145FF42B2953F32CC7DA1167EBD3D0087954816EDD146",
    "03F5A2656BC6DB03A8F85C2FD3FD32D72515D1435BB0869A28FFB49185E23E25D6",
    "02AB13E8808E8AC7A8CAD65660A189FF3744063D6B48C4BC5236F44F98A333CD41",
    "03B7EF6C5948022298051E57521CE56075A16A883516240021BD337C8A6E7DDD13"
  ],
  "signatures": [
    "5249C9FD556B216D4AC61AAEF876F1E672D2911BCC0DED491D96C31832F7BE271EBF9343E19CF8DC149599C532336ABE117BC96F5B1A073C4D78D5B02E4E8B5B",
    "",
    "",
    ""
  ]
}

        """.trimIndent()
        )

    }

    @Test
    fun printExistingTransactionNestedDictOperation(@TempDir tempDir: Path) {
        val transactionFile = File(this.javaClass.classLoader.getResource("call_op_transaction_hex_dict")!!.file)

        MultiSignatureViewCommand().context { terminal = testTerminal }.parse(listOf("--file", transactionFile.absolutePath))
        assertThat(logger.output()).isEqualTo("""
{
  "transactionRID": "D6E07ADF35A8C4EB433390CA3041551CFDC8D5A4C169874A803473DD317D5260",
  "blockchainRID": "D53C7945E33781124C7D4A412D722C8B02C90B3BBC1445568F95DC62B6162306",
  "operations": [
    {
      "operation": "carl",
      "arguments": [
        {
          "key1": "string: value1",
          "key2": "string: value2"
        }
      ]
    },
    {
      "operation": "nop",
      "arguments": [
        "integer: 1731510488739"
      ]
    }
  ],
  "signers": [
    "03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05",
    "68065664FFCCAFF9536494877EBB35B29AC89CA7634AD178432C8C92E31448CF",
    "03D3A6DD0150582C3313F88D8B138256373A763087FBF6228C89EFE44BE76A1256"
  ],
  "signatures": [
    "2145C09B8D4EC8E8F6C0BB8524D9306EE060B5CC351A5626606F3D2E4C16D6835D683D88FE04A75C3873B641F1276762F8FDA11E87CBD616792BF33783C6EA34",
    "",
    ""
  ]
}

        """.trimIndent()
        )

    }
}
