package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.util.thresholdOption
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.util.pubkey
import com.chromia.directory1.proposal_voter_set.proposeUpdateVoterSetOperation
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.common.tx.TransactionStatus


fun metadataTextValidator(): OptionTransformContext.(String) -> Unit = {
    validateMetadataText(it)
}

fun OptionTransformContext.validateMetadataText(text: String) {
    require(text.length <= METADATA_LENGTH_MAX) { "value is too long, maximum allowed length is $METADATA_LENGTH_MAX, current length is ${text.length}." }
}

const val METADATA_LENGTH_MAX = 1_000


fun CliktCommand.proposalDescriptionOption(helpMessage: String = "Proposal description", default: () -> String = { "" }) = option("--description", help = helpMessage)
        .defaultLazy(value = default)
        .validate(metadataTextValidator())


class VotersetUpdateCommand : ChromiaCommand(
        name = "update",
        help = """
            Propose an update of a voter set's members list
        """.trimIndent()
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val keyPairSource by keyPairSourceOption()

    private val voterSet by option(
            "-vs", "--voter-set",
            help = "Name of existing voter set to update"
    ).required()

    private val threshold by thresholdOption()
    private val newMember by option("--add-member", help = "Provider pubkey(s) to add to voter set. Separate keys with ','")
            .convert { it.hexStringToByteArray() }
            .split(",")
            .default(listOf())
    private val removeMember by option("--remove-member", help = "Provider pubkey(s) to remove from voter set. Separate keys with ','")
            .convert { it.hexStringToByteArray() }
            .split(",")
            .default(listOf())

    private val description by proposalDescriptionOption {
        "Update voter set $voterSet - threshold: $threshold, " +
                "add members: ${newMember.map { it.toHex() }.toTypedArray().contentToString()}, " +
                "remove members: ${removeMember.map { it.toHex() }.toTypedArray().contentToString()}"
    }

    override fun run() {
        settings.config.configureSigners(keyPairSource)
        val clientConfig = settings.config.setApiUrls(networkTarget.urls).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)

        val res = client.transactionBuilder()
                .proposeUpdateVoterSetOperation(
                        client.pubkey.data,
                        voterSet, threshold, null, newMember, removeMember, description
                )
                .addNop()
                .postAwaitConfirmation(txListener())

        if (res.status == TransactionStatus.REJECTED || res.status == TransactionStatus.UNKNOWN)
            throw PrintMessage("Failed to add proposal with reason ${res.rejectReason}", statusCode = 1)
        echo("Proposal for voter set $voterSet has been added")
    }
}
