package com.chromia.cli.ft

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.directory1.lib.ft4.core.accounts.AuthType
import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.mordant.input.InputReceiver
import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.crypto.PubKey
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FTAuthPickerTest {
    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(terminalInterface = logger, width = 500)
    private val pubkey = PubKey("2".repeat(64).hexStringToWrappedByteArray())
    private val descriptors = listOf(
            Ft4GetAccountAuthDescriptorsBySignerResult(
                    id = "5".repeat(64).hexStringToWrappedByteArray(),
                    args = GtvFactory.gtv(GtvFactory.gtv(GtvFactory.gtv("A")), GtvFactory.gtv(pubkey.data)),
                    created = System.currentTimeMillis(),
                    authType = AuthType.S,
                    rules = GtvNull,
                    accountId = "1".repeat(64).hexStringToWrappedByteArray()
            ),
            Ft4GetAccountAuthDescriptorsBySignerResult(
                    id = "6".repeat(64).hexStringToWrappedByteArray(),
                    args = GtvFactory.gtv(GtvFactory.gtv(GtvFactory.gtv("A")), GtvFactory.gtv(2), GtvFactory.gtv(
                            GtvFactory.gtv(pubkey.data),
                            GtvFactory.gtv("4".repeat(64).hexStringToWrappedByteArray()),
                    )),
                    created = System.currentTimeMillis(),
                    authType = AuthType.M,
                    rules = GtvNull,
                    accountId = "1".repeat(64).hexStringToWrappedByteArray()
            )
    )


    @Test
    fun receiveEventContinueOnFaultEventTest() {
        val picker = FTAuthPicker(testTerminal, descriptors)
        assertThat(picker.receiveEvent(KeyboardEvent("A"))).isEqualTo(InputReceiver.Status.Continue)
        assertThat(picker.receiveEvent(KeyboardEvent("q"))).isEqualTo(InputReceiver.Status.Continue)
    }

    @Test
    fun receiveEventCtrlCStopsTest() {
        val picker = FTAuthPicker(testTerminal, descriptors)
        val res = assertThrows<PrintMessage> {
            picker.receiveEvent(KeyboardEvent("c", ctrl = true))
        }
        assertThat(res.message).isEqualTo("Aborting process")
    }

    @Test
    fun receiveEventWithCorrectIndexTest() {
        val picker = FTAuthPicker(testTerminal, descriptors)
        assertThat(picker.selectedDescriptor.id).isEqualTo(descriptors.first().id)
        assertThat(picker.receiveEvent(KeyboardEvent("2"))).isEqualTo(InputReceiver.Status.Finished)
        assertThat(picker.selectedDescriptor.id).isEqualTo(descriptors.last().id)
    }

    @Test
    fun rendererShowsTable() {
        val picker = FTAuthPicker(testTerminal, descriptors)
        picker.render()
        assertThat(logger.output().replace("\u001B[22m", "").replace("\u001B[2m", "")).isEqualTo("""
                                                                                    Auth descriptors                                                                                    
╭───────┬──────────────────────────────────────────────────────────────────┬───────┬─────────────────────────────┬─────────────────────────────────────────────────────────────────────╮
│ Index │ Id                                                               │ flags │ Number of signatures needed │ keys                                                                │
├───────┼──────────────────────────────────────────────────────────────────┼───────┼─────────────────────────────┼─────────────────────────────────────────────────────────────────────┤
│ 1     │ 5555555555555555555555555555555555555555555555555555555555555555 │ ["A"] │ 1                           │ x"2222222222222222222222222222222222222222222222222222222222222222" │
├───────┼──────────────────────────────────────────────────────────────────┼───────┼─────────────────────────────┼─────────────────────────────────────────────────────────────────────┤
│ 2     │ 6666666666666666666666666666666666666666666666666666666666666666 │ ["A"] │ 2                           │ x"2222222222222222222222222222222222222222222222222222222222222222" │
│       │                                                                  │       │                             │ x"4444444444444444444444444444444444444444444444444444444444444444" │
╰───────┴──────────────────────────────────────────────────────────────────┴───────┴─────────────────────────────┴─────────────────────────────────────────────────────────────────────╯
Please select a valid auth descriptor from the table of auth descriptors above with the index column as key (Ctrl + c to abort the process)

        """.trimIndent())
    }
}