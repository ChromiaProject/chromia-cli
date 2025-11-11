package com.chromia.cli.command.multisignature

import com.chromia.build.tools.multisignature.MultiSignatureTxData
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.multisignature.parseTransactionFile
import com.chromia.cli.util.ExplicitDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.exception.UserMistake
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtx.signTransaction

class MultiSignatureSendCommand : ChromiaCommand(name = "send", help = "Send a fully signed transaction") {

    private val settings by optionalChromiaModelConfigOption()
    private val keyPairSource by keyPairSourceOption()
    private val explicitTarget by ExplicitDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()
    private val awaitConfirmation by option("--await", "-a", help = "Wait for transaction to be included in a block").flag("--no-await", default = true)
    private val transactionFile by option("-f", "--file", help = "Path to file of transaction")
            .file(canBeDir = false, mustExist = true, mustBeReadable = true)
            .required()

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val postchainClientConfig = settings.config.setApiUrls(target.urls).setBrid(target.brid)
        postchainClientConfig.configureSigners(keyPairSource)

        val transactionBuilder = target.createClient(postchainClientConfig).transactionBuilder()
        val txData = MultiSignatureTxData.parseTransactionFile(transactionFile)

        val signedTransaction = try {
            signTransaction(txData.transaction, txData.txRid, postchainClientConfig.signers)
        } catch (e: UserMistake) {
            if (e.message == "Signature for this signer already exists") {
                txData.transaction
            } else {
                throw PrintMessage("Failed to sign transaction, cause: ${e.message}", 1)
            }
        }

        val res = if (awaitConfirmation) {
            transactionBuilder.postTransactionAwaitConfirmation(signedTransaction)
        } else {
            transactionBuilder.postTransaction(signedTransaction)
        }
        if (res.status == TransactionStatus.REJECTED || res.status == TransactionStatus.UNKNOWN) {
            echo("Reject reason ${res.rejectReason}")
            throw PrintMessage("Transaction Failed with code ${res.httpStatusCode}: ${res.rejectReason}", statusCode = 1)
        }
        echo("transaction with rid ${res.txRid.rid} was posted ${res.status}${res.rejectReason?.let { ": $it" } ?: ""}")
    }
}
