package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.util.pubkey
import com.chromia.cli.util.secretOption
import com.chromia.directory1.proposal_voter_set.proposeUpdateVoterSetOperation
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.OptionTransformContext
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.common.tx.TransactionStatus


fun metadataTextValidator(): OptionTransformContext.(String) -> Unit = {
    validateMetadataText(it)
}

fun OptionTransformContext.validateMetadataText(text: String) {
    require(text.length <= METADATA_LENGTH_MAX) { "value is too long, maximum allowed length is $METADATA_LENGTH_MAX" }
}

const val METADATA_LENGTH_MAX = 1_000


fun CliktCommand.proposalDescriptionOption(helpMessage: String = "Proposal description", default: () -> String = { "" }) = option("--description", help = helpMessage)
        .defaultLazy(value = default)
        .validate(metadataTextValidator())


class VotersetUpdateCommand : CliktCommand(
        name = "update",
        help = """
            Propose an update of a voter set's governor
            
            New governor must be an existing voter set.
        """.trimIndent()
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val secret by secretOption()

    private val voterSet by option(
            "-vs", "--voter-set",
            help = "Name of existing voter set to update"
    ).required()

    private val threshold by option("--threshold", help = "New threshold").long()
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
        secret?.let { settings.config.setSignerFromSecret(it.toPath()) }
        val clientConfig = settings.config.setApiUrls(networkTarget.url).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)

        val res = client.transactionBuilder()
                .proposeUpdateVoterSetOperation(
                        client.pubkey.data,
                        voterSet, threshold, null, newMember, removeMember, description
                )
                .addNop()
                .postAwaitConfirmation()

        if (res.status == TransactionStatus.REJECTED || res.status == TransactionStatus.UNKNOWN)
            throw PrintMessage("Failed to add proposal with reason ${res.rejectReason}", statusCode = 1)
        echo("Proposal for voter set $voterSet has been added")
    }
}
