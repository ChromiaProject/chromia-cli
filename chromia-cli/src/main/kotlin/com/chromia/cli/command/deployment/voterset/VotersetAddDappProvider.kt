package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.ft.*
import com.chromia.cli.util.*
import com.chromia.directory1.economy_chain.REGISTER_DAPP_PROVIDER
import com.chromia.directory1.economy_chain.registerDappProviderOperation
import com.chromia.directory1.economy_chain_in_directory_chain.getEconomyChainRid
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvString


class VotersetAddDappProviderCommand : ChromiaCommand(
        name = "add-dapp-provider",
        help = "Add a dapp provider to container"
) {
    private val clientProvider: PostchainClientProvider = PostchainClientProviderImpl()
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val keyPairSource by keyPairSourceOption()
    private val awaitConfirmation by option("--await", "-a", help = "Wait for transaction to be included in a block").flag("--no-await", default = true)
    private val newProviderPubKey by publicKeyOption(help = "The public key of the dApp provider to be added")
    private val containerId by containerIdOption(help = "Container Identifier to add dapp provider too").required()
    private val evmAuth by evmAuthOption()

    override fun run() {
        require(newProviderPubKey != null) { "Missing value for dApp provider public key" }
        settings.config.configureSigners(keyPairSource)
        val d1ClientConfig = settings.config.setApiUrls(networkTarget.url).setBrid(networkTarget.brid)
        val d1Client = networkTarget.createClient(d1ClientConfig)

        val economyClient = createEconomyChainClient(d1Client)
        val transactionBuilder = economyClient.transactionBuilder()

        initFtAuth(economyClient)
        val signerPubKey = evmAuth ?: (economyClient.config.signers.singleOrNull()?.pubKey?.data
                ?: throw PrintMessage("A single keypair is required to use FT authentication", statusCode = 1))

        val (accountId, authDescriptorId) = findFtAccountIdAndAuthDescriptorId(
                economyClient,
                null,
                signerPubKey,
                REGISTER_DAPP_PROVIDER,
                null)

        if (evmAuth != null) {
            val args = listOf(GtvString(containerId), GtvByteArray(newProviderPubKey!!.hexStringToByteArray()))
            addEvmAuthOperation(economyClient, transactionBuilder, REGISTER_DAPP_PROVIDER, args, evmAuth!!, accountId, authDescriptorId)
        } else {
            addFtAuthOperation(transactionBuilder, accountId, authDescriptorId)
        }

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
