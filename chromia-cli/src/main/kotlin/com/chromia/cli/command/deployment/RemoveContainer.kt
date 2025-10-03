package com.chromia.cli.command.deployment

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.ft.addFtAuthenticationOperation
import com.chromia.cli.tools.ft.initFtAuth
import com.chromia.cli.util.ExplicitDeploymentOption
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.util.containerIdOption
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.directory1.economy_chain_in_directory_chain.getEconomyChainRid
import com.chromia.directory1.economy_chain_remove_container.REMOVE_CONTAINER
import com.chromia.directory1.economy_chain_remove_container.removeContainerOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.common.BlockchainRid
import net.postchain.common.tx.TransactionStatus


class RemoveContainerCommand : ChromiaCommand(
        name = "remove-container",
        help = "Remove a container and its associated lease without refund"
) {
    override val hiddenFromHelp: Boolean
        get() = true

    private val clientProvider: PostchainClientProvider = PostchainClientProviderImpl()
    private val settings by optionalChromiaModelConfigOption()
    private val keyPairSource by keyPairSourceOption()
    private val explicitTarget by ExplicitDeploymentOption({ settings.config })
    private val awaitConfirmation by option("--await", "-a", help = "Wait for transaction to be included in a block").flag("--no-await", default = true)
    private val containerId by containerIdOption(help = "Container Identifier to add dapp provider too").required()

    override fun run() {
        val postchainClientConfig = settings.config.setApiUrls(explicitTarget.urls).setBrid(explicitTarget.brid)
        postchainClientConfig.configureSigners(keyPairSource)
        val client = explicitTarget.createClient(postchainClientConfig)

        val economyClient = createEconomyChainClient(client)
        val transactionBuilder = economyClient.transactionBuilder()

        initFtAuth(economyClient)
        val signerPubkey = (economyClient.config.signers.singleOrNull()?.pubKey
                ?: throw PrintMessage("A single keypair is required to use FT authentication", statusCode = 1))
        addFtAuthenticationOperation(economyClient, transactionBuilder, REMOVE_CONTAINER, signerPubkey.data)

        val res = transactionBuilder
                .removeContainerOperation(containerId)
                .addNop()
                .run {
                    if (awaitConfirmation) postAwaitConfirmation(txListener()) else post()
                }

        if (res.status == TransactionStatus.REJECTED || res.status == TransactionStatus.UNKNOWN) {
            throw PrintMessage("Transaction to remove container failed with reason: ${res.rejectReason}", statusCode = 1)
        }
        echo("Transaction with rid ${res.txRid.rid} to remove container was posted ${res.status}${res.rejectReason?.let { ": $it" } ?: ""}")
    }

    private fun createEconomyChainClient(d1Client: PostchainClient): PostchainClient {
        val economyChainBrid = d1Client.getEconomyChainRid()
        require(economyChainBrid != null) { "Failed to get economy chain brid from management chain" }
        return clientProvider.createClient(d1Client.config.copy(blockchainRid = BlockchainRid(economyChainBrid)))
    }
}
