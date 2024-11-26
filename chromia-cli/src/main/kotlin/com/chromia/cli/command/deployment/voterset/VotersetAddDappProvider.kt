package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.ft.createFTAuthenticator
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.util.containerIdOption
import com.chromia.cli.util.publicKeyOption
import com.chromia.cli.util.secretOption
import com.chromia.directory1.economy_chain.REGISTER_DAPP_PROVIDER
import com.chromia.directory1.economy_chain.registerDappProviderOperation
import com.chromia.directory1.economy_chain_in_directory_chain.getEconomyChainRid
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus


class VotersetAddDappProviderCommand : ChromiaCommand(
        name = "add-dapp-provider",
        help = "Add a dapp provider to container"
) {
    private val clientProvider: PostchainClientProvider = PostchainClientProviderImpl()
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val secret by secretOption()
    private val awaitConfirmation by option("--await", "-a", help = "Wait for transaction to be included in a block").flag("--no-await", default = true)
    private val newProviderPubKey by publicKeyOption(help = "The public key of the dApp provider to be added")
    private val containerId by containerIdOption(help = "Container Identifier to add dapp provider too").required()

    override fun run() {
        require(newProviderPubKey != null) { "Missing value for dApp provider public key" }
        secret?.let { settings.config.setSignerFromSecret(it.toPath()) }
        val d1ClientConfig = settings.config.setApiUrls(networkTarget.url).setBrid(networkTarget.brid)
        val d1Client = networkTarget.createClient(d1ClientConfig)

        val economyClient = createEconomyChainClient(d1Client)
        val transactionBuilder = economyClient.transactionBuilder()

        val authenticator = createFTAuthenticator(economyClient, terminal)
        val signerPubkey = (economyClient.config.signers.singleOrNull()?.pubKey
                ?: throw PrintMessage("A single keypair is required to use FT authentication", statusCode = 1))
        authenticator.addAuthenticationOperation(transactionBuilder, REGISTER_DAPP_PROVIDER, signerPubkey)

        val res = transactionBuilder
                .registerDappProviderOperation(containerId, newProviderPubKey!!.hexStringToByteArray())
                .addNop()
                .run {
                    if (awaitConfirmation) postAwaitConfirmation() else post()
                }

        if (res.status == TransactionStatus.REJECTED || res.status == TransactionStatus.UNKNOWN) {
            throw PrintMessage("Transaction to add dapp provider failed with reason: ${res.rejectReason}", statusCode = 1)
        }
        echo("Transaction with rid ${res.txRid.rid} to add dapp provider was posted ${res.status}${res.rejectReason?.let { ": $it" } ?: ""}")
    }

    private fun createEconomyChainClient(d1Client: PostchainClient): PostchainClient {
        val economyChainBrid = d1Client.getEconomyChainRid()
        require(economyChainBrid != null) { "Failed to get economy chain brid from management chain" }
        return clientProvider.createClient(d1Client.config.copy(blockchainRid = BlockchainRid(economyChainBrid)))
    }
}
