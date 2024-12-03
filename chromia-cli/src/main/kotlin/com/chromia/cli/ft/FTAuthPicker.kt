package com.chromia.cli.ft

import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.mordant.input.InputEvent
import com.github.ajalt.mordant.input.InputReceiver
import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.info

class FTAuthPicker(override val terminal: Terminal, authDescriptorsCandidates: List<Ft4GetAccountAuthDescriptorsBySignerResult>) : InputReceiver<Unit> {
    private val indexedDescriptors = authDescriptorsCandidates.mapIndexed { index: Int, s: Ft4GetAccountAuthDescriptorsBySignerResult -> (index + 1).toString() to s }.toMap()
    var selectedDescriptor = authDescriptorsCandidates.first()

    override fun receiveEvent(event: InputEvent): InputReceiver.Status<Unit> {
        return if (event is KeyboardEvent) {
            if (event.isCtrlC) {
                throw PrintMessage("Aborting process", statusCode = 1)

            } else if (indexedDescriptors.containsKey(event.key)) {
                terminal.info("You selected Descriptor ${event.key}")
                selectedDescriptor = indexedDescriptors[event.key]!!
                InputReceiver.Status.Finished
            } else {
                terminal.info("${event.key} is not a valid pick from the table of auth descriptors")
                InputReceiver.Status.Continue
            }
        } else {
            InputReceiver.Status.Continue
        }
    }

    fun render() {
        terminal.println(
                terminal.theme.defaultTable {
                    captionTop("Auth descriptors")
                    header { row("Index", "Id", "flags", "Number of signatures needed", "keys") }
                    body {
                        indexedDescriptors.map { (key, authDescriptor) ->
                            row(
                                    key,
                                    authDescriptor.id,
                                    authDescriptor.getFlags(),
                                    authDescriptor.getNumberOfSigners(),
                                    authDescriptor.getKeysAsFormattedString())
                        }
                    }
                })
        terminal.println("Please select a valid auth descriptor from the table of auth descriptors above with the index column as key (Ctrl + c to abort the process)")
    }
}