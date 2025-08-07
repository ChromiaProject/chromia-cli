package com.chromia.cli.command.deployment.proposal

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.util.pubkey
import com.chromia.directory1.proposal.revokeProposalOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.tx.TransactionStatus
import net.postchain.common.types.RowId


class ProposalRevokeCommand : ChromiaCommand(
        name = "revoke",
        help = "Revoke/remove a proposal submitted by you"
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val idx by proposalIndexOption().required()
    private val keyPairSource by keyPairSourceOption()

    override fun run() {
        settings.config.configureSigners(keyPairSource)
        val clientConfig = settings.config.setApiUrls(networkTarget.urls).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)

        val pubkey: ByteArray
        try {
            pubkey = client.pubkey.data
        } catch (_: NoSuchElementException) {
            throw CanNotFindPubkeyException()
        }

        val res = client.transactionBuilder()
                .revokeProposalOperation(pubkey, RowId(idx))
                .addNop()
                .postAwaitConfirmation(txListener())

        if (res.status == TransactionStatus.REJECTED || res.status == TransactionStatus.UNKNOWN)
            throw PrintMessage("Proposal action failed with reason ${res.rejectReason}", statusCode = 1)
        echo("Proposal $idx was successfully revoked")
    }
}
