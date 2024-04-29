package com.chromia.cli.command.deployment

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.util.ExplicitRemoteSystemOption
import com.chromia.cli.util.RemoteSystemOption
import com.chromia.cli.util.SystemOption
import com.chromia.cli.util.containerIdOption
import com.chromia.cli.util.publicKeyOption
import com.chromia.directory1.economy_chain.LeaseData
import com.chromia.directory1.economy_chain.getLeaseByContainerName
import com.chromia.directory1.economy_chain.getLeasesByAccount
import com.chromia.directory1.economy_chain_in_directory_chain.getEconomyChainRid
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray

class DeployInspectLeaseCommand(val clientProvider: PostchainClientProvider = PostchainClientProviderImpl()) : CliktCommand(
        name = "lease-info",
        help = "Information about a leases of for a given owner",
        hidden = true
) {
    private val settings by optionalChromiaModelConfigOption()
    private val explicitTarget by ExplicitRemoteSystemOption().cooccurring()
    private val deploymentTarget by RemoteSystemOption { settings.model ?: ChromiaModel() }.cooccurring()
    private val ownerOfLeasePubkey by publicKeyOption()
    private val containerId by containerIdOption()

    override fun run() {
        val leaseData = getLeaseData()

        echo(defaultTable {
            header {
                row("Cluster", "Container", "Container Units (SCU)", "Extra storage (gib)", "Expire Time (millis)", "Expired", "Auto Renewal")
            }
            body {
                leaseData.map {
                    row(it.clusterName, it.containerName, it.containerUnits, it.extraStorageGib, it.expireTimeMillis, it.expired, it.autoRenew)
                }
            }
        })
    }

    private fun getLeaseData(): List<LeaseData> {
        val target = deploymentTarget ?: explicitTarget
        require(target != null) { "Must specify network target from config or set it explicitly" }
        val container = containerId ?: target.containerId
        val client = createEconomyChainClient(target)

        if (ownerOfLeasePubkey != null) {
            echo("Getting active leases for user with public key: $ownerOfLeasePubkey")
            val leaseData = client.getLeasesByAccount(ownerOfLeasePubkey!!.hexStringToByteArray())
            if (leaseData.isEmpty()) throw PrintMessage("No active leases for user: $ownerOfLeasePubkey", 0)
            return leaseData

        } else if (container != null) {
            echo("Getting lease information for container: $container")
            val leaseData = client.getLeaseByContainerName(container)
                    ?: throw PrintMessage("Container $container not found", 0)
            return listOf(leaseData)

        } else {
            throw PrintMessage("Option pubkey or container name needs to be specified.", 0)
        }
    }

    private fun createEconomyChainClient(target: SystemOption): PostchainClient {
        val chromiaClientConfig = settings.config.setApiUrls(target.url).setBrid(target.brid)
        val d1Client = chromiaClientConfig.client(clientProvider)
        val economyChainBrid = d1Client.getEconomyChainRid()
                ?: throw PrintMessage("Couldn't get blockchainrid for economy chain", 1)

        return clientProvider.createClient(d1Client.config.copy(blockchainRid = BlockchainRid(economyChainBrid)))
    }
}
