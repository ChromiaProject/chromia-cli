package com.chromia.cli.command.deployment.proposal

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.util.pubkey
import com.chromia.directory1.proposal.voting.makeVoteOperation
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.switch
import net.postchain.common.tx.TransactionStatus

class ProposalVoteCommand : CliktCommand(
        name = "vote",
        help = """
            Vote on proposals linked to your public key
        """.trimIndent()
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }

    private val idx by proposalIndexOption().required()


    private val vote by option(help = "Vote action to cast").switch(
            "--accept" to true,
            "--reject" to false,
    ).required()


    override fun run() {
        val clientConfig = settings.config.setApiUrls(networkTarget.url).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)

        val res = client.transactionBuilder()
                .makeVoteOperation(client.pubkey.data, idx, vote)
                .postAwaitConfirmation()

        if (res.status == TransactionStatus.REJECTED || res.status == TransactionStatus.UNKNOWN)
            throw PrintMessage("Proposal action failed with reason ${res.rejectReason}", statusCode = 1)
        echo("Successfully voted on proposal $idx")
    }
}