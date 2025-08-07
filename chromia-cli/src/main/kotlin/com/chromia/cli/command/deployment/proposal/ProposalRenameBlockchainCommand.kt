package com.chromia.cli.command.deployment.proposal

import com.chromia.build.tools.util.apiVersion
import com.chromia.build.tools.util.snakeCaseName
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.command.deployment.voterset.proposalDescriptionOption
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.blockchainRidOption
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.pubkey
import com.chromia.directory1.proposal_blockchain.proposeBlockchainRenameOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.BlockchainRid
import net.postchain.common.tx.TransactionStatus

class ProposalRenameBlockchainCommand : ChromiaCommand(
        name = "rename",
        help = """
            Create a proposal to rename a deployed blockchain
        """.trimIndent()
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val keyPairSource by keyPairSourceOption()
    private val description by proposalDescriptionOption {
        "Renaming blockchain with BRID: ${bridOfBlockchainToRename} to '$newName'"
    }

    private val newName by option("-n", "--new-name", "--name", help = "New name of the blockchain").required()

    private val bridOfBlockchainToRename by blockchainRidOption("Brid for blockchain to rename").required()

    override fun run() {
        settings.config.configureSigners(keyPairSource)
        val clientConfig = settings.config.setApiUrls(networkTarget.urls).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)
        val apiVersion = client.apiVersion
        val brid = BlockchainRid.buildFromHex(bridOfBlockchainToRename)

        if (apiVersion < 61) {
            throw PrintMessage(
                    "Blockchain rename operation requires directory chain version 61, found version $apiVersion"
            )
        }

        val pubKey: ByteArray
        try {
            pubKey = client.config.pubkey.data
        } catch (_: NoSuchElementException) {
            throw CanNotFindPubkeyException()
        }

        val res = client.transactionBuilder()
                .proposeBlockchainRenameOperation(
                        pubKey,
                        brid,
                        snakeCaseName(newName),
                        description
                )
                .addNop()
                .postAwaitConfirmation(txListener())

        if (res.status == TransactionStatus.REJECTED || res.status == TransactionStatus.UNKNOWN) {
            throw PrintMessage("Cannot add proposal for renaming blockchain reason ${res.rejectReason}", statusCode = 1)
        }

        echo("Blockchain rename proposition was added successfully")
    }
}