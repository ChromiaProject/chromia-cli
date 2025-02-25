package com.chromia.cli.command.deployment

import com.chromia.cli.base.formatter.json
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.util.*
import com.chromia.directory1.economy_chain.LeaseData
import com.chromia.directory1.economy_chain.getLeaseByContainerName
import com.chromia.directory1.economy_chain.getLeasesByAccount
import com.chromia.directory1.economy_chain_in_directory_chain.getEconomyChainRid
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex

class DeployInspectLeaseCommand : ChromiaCommand(name = "lease-info", help = """
    Information about a leases of for a given owner    
    $EXPERIMENTAL_COMMAND    
""".trimIndent()
) {
    override val hiddenFromHelp: Boolean
        get() = true

    private val clientProvider: PostchainClientProvider = PostchainClientProviderImpl()
    private val settings by optionalChromiaModelConfigOption()
    private val explicitTarget by ExplicitRemoteSystemOption { settings.config }.cooccurring()
    private val deploymentTarget by RemoteSystemOption {
        (settings.model ?: ChromiaModel.default()) to settings.config
    }.cooccurring()
    private val ownerOfLeaseAccountId by accountIdOption()
    private val containerId by containerIdOption()
    private val outputFormat by tableOutputFormat()

    override fun run() {
        val (accountId, container, leaseData) = getLeaseData()

        when (outputFormat ?: if (terminal.terminalInfo.outputInteractive) TableOutputFormat.table else TableOutputFormat.JSON) {
            TableOutputFormat.table -> {
                if (accountId != null) echo("Getting active leases for account id: $accountId")
                if (container != null) echo("Getting lease information for container: $container")
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

            TableOutputFormat.JSON ->
                echo(json(mapOf(
                        "account id" to accountId,
                        "container" to container,
                        "leases" to leaseData.map {
                            mapOf(
                                    "Cluster" to it.clusterName,
                                    "Container" to it.containerName,
                                    "Container_units" to it.containerUnits,
                                    "Extra_storage" to it.extraStorageGib,
                                    "Expire_time" to it.expireTimeMillis,
                                    "Expired" to it.expired,
                                    "Auto_renew" to it.autoRenew
                            )
                        })))
        }
    }

    private fun getLeaseData(): Triple<String?, String?, List<LeaseData>> {
        val target = deploymentTarget ?: explicitTarget
        require(target != null) { "Must specify network target from config or set it explicitly" }
        val container = containerId ?: target.containerId
        require(ownerOfLeaseAccountId != null || container != null) { "Option account id or container name needs to be specified." }
        val client = createEconomyChainClient(target)

        return if (ownerOfLeaseAccountId != null) {
            val leaseData = client.getLeasesByAccount(ownerOfLeaseAccountId!!)
            if (leaseData.isEmpty()) throw PrintMessage("No active leases for user: $ownerOfLeaseAccountId", 0)
            Triple(ownerOfLeaseAccountId!!.toHex(), null, leaseData)

        } else {
            val leaseData = client.getLeaseByContainerName(container!!)
                    ?: throw PrintMessage("Container $container not found", 0)
            Triple(null, container, listOf(leaseData))
        }
    }

    private fun createEconomyChainClient(target: SystemOption): PostchainClient {
        val d1Client = target.client
        val economyChainBrid = d1Client.getEconomyChainRid()
                ?: throw PrintMessage("Couldn't get blockchain RID for economy chain", 1)

        return clientProvider.createClient(d1Client.config.copy(blockchainRid = BlockchainRid(economyChainBrid)))
    }
}
