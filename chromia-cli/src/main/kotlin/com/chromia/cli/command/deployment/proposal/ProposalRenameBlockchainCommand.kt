package com.chromia.cli.command.deployment.proposal

import com.chromia.build.tools.util.apiVersion
import com.chromia.build.tools.util.snakeCaseName
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.command.deployment.voterset.proposalDescriptionOption
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.tx.TransactionStatus
import com.chromia.directory1.proposal_blockchain.proposeBlockchainRenameOperation
import com.github.ajalt.clikt.parameters.options.option

class ProposalRenameBlockchainCommand : ChromiaCommand(
        name = "rename",
        help = """
            Create a proposal to rename a deployed blockchain
        """.trimIndent()
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val secret by secretOption()
    private val description by proposalDescriptionOption {
        "Renaming blockchain with BRID: ${networkTarget.brid} to '$name'"
    }

    private val name by option("-n", "--name", help = "New name of the blockchain").required()

    override fun run() {
        secret?.let { settings.config.setSignerFromSecret(it.toPath()) }
        val clientConfig = settings.config.setApiUrls(networkTarget.url).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)
        val apiVersion = client.apiVersion

        if (apiVersion < 61) {
            throw PrintMessage(
                    "Blockchain rename operation requires directory chain version 61, found version $apiVersion"
            )
        }

        val pubKey: ByteArray
        try {
            pubKey = client.config.pubkey.data
        } catch (e: NoSuchElementException) {
            throw CanNotFindPubkeyException()
        }

        val res = client.transactionBuilder()
                .proposeBlockchainRenameOperation(
                        pubKey,
                        networkTarget.brid,
                        snakeCaseName(name),
                        description
                )
                .addNop()
                .postAwaitConfirmation()

        if (res.status == TransactionStatus.REJECTED || res.status == TransactionStatus.UNKNOWN) {
            throw PrintMessage("Cannot add proposal for renaming blockchain reason ${res.rejectReason}", statusCode = 1)
        }

        echo("Blockchain rename proposition was added successfully")
    }
}