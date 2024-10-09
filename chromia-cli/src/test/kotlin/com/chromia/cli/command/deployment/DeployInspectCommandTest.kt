package com.chromia.cli.command.deployment

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.cli.util.BlockchainAnalyzer
import com.chromia.cli.util.ModuleArgsAnalyzer
import com.chromia.cli.util.RellEntity
import com.chromia.cli.util.RellModule
import com.chromia.cli.util.RellObject
import com.chromia.cli.util.RellOperation
import com.chromia.cli.util.RellParameter
import com.chromia.cli.util.RellQuery
import com.chromia.cli.util.RellStructure
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import org.junit.jupiter.api.Test

class DeployInspectCommandTest {
    private val nonInteractiveLogger = TerminalRecorder(width = 1000, outputInteractive = false)
    private val nonInteractiveTerminal = Terminal(terminalInterface = nonInteractiveLogger)
    private val interactiveLogger = TerminalRecorder(width = 1000, outputInteractive = true)
    private val interactiveTerminal = Terminal(terminalInterface = interactiveLogger)

    private val testInputBlockchain = mapOf(
            "foo" to RellModule("foo",
                    queries = mapOf("q" to RellQuery("q.1", listOf(RellParameter("p1", gtv("integer"))), gtv("gtv"))),
                    operations = mapOf("op" to RellOperation("op.1", listOf(RellParameter("op1", gtv("integer"))))),
                    entities = mapOf("ent" to RellEntity("ent.1", gtv(listOf(gtv(mapOf("name" to gtv("attr1"), "type" to gtv("text"), "mutable" to gtv(false))))))),
                    objects = mapOf("obj" to RellObject("obj.1", gtv(listOf(gtv(mapOf("name" to gtv("attr1"), "type" to gtv("text"), "mutable" to gtv(false))))))),
                    structures = mapOf("bogus" to RellStructure(gtv(listOf())))),
            "bar" to RellModule("bar",
                    queries = mapOf("q" to RellQuery("q.2", listOf(RellParameter("p1", gtv("text"))), gtv("decimal"))),
                    operations = emptyMap(),
                    entities = emptyMap(),
                    objects = emptyMap(),
                    structures = mapOf("module_args" to RellStructure(gtv(listOf(
                            gtv(mapOf("name" to gtv("arg1"), "type" to gtv("text"), "mutable" to gtv(false))),
                            gtv(mapOf("name" to gtv("arg2"), "type" to gtv("integer"), "mutable" to gtv(false))),
                    ))))),
    )

    @Test
    fun moduleList() {
        testCommand(testInputBlockchain, null, listOf("-brid", BlockchainRid.ZERO_RID.toHex(), "--list-modules"))
        assertThat(nonInteractiveLogger.output()).isEqualTo("""
            [
              "foo",
              "bar"
            ]
            
        """.trimIndent())
    }

    @Test
    fun all() {
        testCommand(testInputBlockchain, null, listOf("-brid", BlockchainRid.ZERO_RID.toHex()))
        assertThat(nonInteractiveLogger.output()).isEqualTo("""
           {
             "queries": [
               {
                 "mount_name": "q.1",
                 "return_type": "\"gtv\"",
                 "parameters": {
                   "p1": "\"integer\""
                 }
               },
               {
                 "mount_name": "q.2",
                 "return_type": "\"decimal\"",
                 "parameters": {
                   "p1": "\"text\""
                 }
               }
             ],
             "operations": [
               {
                 "mount_name": "op.1",
                 "parameters": [
                   {
                     "name": "op1",
                     "type": "\"integer\""
                   }
                 ]
               }
             ],
             "entities": [
               {
                 "mount_name": "ent.1",
                 "attributes": [
                   {
                     "name": "attr1",
                     "type": "\"text\"",
                     "mutable": false
                   }
                 ]
               }
             ],
             "objects": [
               {
                 "mount_name": "obj.1",
                 "attributes": [
                   {
                     "name": "attr1",
                     "type": "\"text\"",
                     "mutable": false
                   }
                 ]
               }
             ]
           }
           
           """.trimIndent())
        assertThat(interactiveLogger.output()).contains("q.1")
    }

    @Test
    fun singleFound() {
        testCommand(testInputBlockchain, null, listOf("-brid", BlockchainRid.ZERO_RID.toHex(), "--signature", "q.2"))
        assertThat(nonInteractiveLogger.output()).isEqualTo("""
           {
             "mount_name": "q.2",
             "return_type": "\"decimal\"",
             "parameters": {
               "p1": "\"text\""
             }
           }
           
           """.trimIndent())
        assertThat(interactiveLogger.output()).contains("q.2")
    }

    @Test
    fun singleNotFound() {
        testCommand(testInputBlockchain, null, listOf("-brid", BlockchainRid.ZERO_RID.toHex(), "--signature", "bogus"))
        assertThat(nonInteractiveLogger.output()).isEqualTo("""
           {}
           
           """.trimIndent())
    }

    @Test
    fun moduleArgsWithoutValues() {
        testCommand(testInputBlockchain, null, listOf("-brid", BlockchainRid.ZERO_RID.toHex(), "--module-args"))
        assertThat(nonInteractiveLogger.output()).isEqualTo("""
           [
             {
               "module_name": "bar",
               "module_args": [
                 {
                   "name": "arg1",
                   "type": "\"text\""
                 },
                 {
                   "name": "arg2",
                   "type": "\"integer\""
                 }
               ]
             }
           ]
           
           """.trimIndent())
        assertThat(interactiveLogger.output()).contains("arg1")
    }

    @Test
    fun moduleArgsWithValues() {
        testCommand(
                testInputBlockchain,
                mapOf("bar" to mapOf("arg1" to gtv("foobar"), "arg2" to gtv(17))),
                listOf("-brid", BlockchainRid.ZERO_RID.toHex(), "--module-args")
        )
        assertThat(nonInteractiveLogger.output()).isEqualTo("""
           [
             {
               "module_name": "bar",
               "module_args": [
                 {
                   "name": "arg1",
                   "type": "\"text\"",
                   "value": "\"foobar\""
                 },
                 {
                   "name": "arg2",
                   "type": "\"integer\"",
                   "value": "17"
                 }
               ]
             }
           ]
           
           """.trimIndent())
        assertThat(interactiveLogger.output()).contains("arg1")
    }

    private fun testCommand(blockchain: Map<String, RellModule>, moduleArgs: Map<String, Map<String, Gtv>>?, args: List<String>) {
        DeployInspectCommand(
                { BlockchainAnalyzer { blockchain } },
                { ModuleArgsAnalyzer { moduleArgs } },
        )
                .context { terminal = nonInteractiveTerminal }
                .parse(args)

        DeployInspectCommand(
                { BlockchainAnalyzer { blockchain } },
                { ModuleArgsAnalyzer { moduleArgs } },
        )
                .context { terminal = interactiveTerminal }
                .parse(args)
    }
}
