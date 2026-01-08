package com.chromia.cli.command.deployment

import com.chromia.api.ChromiaDeploymentApi
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.BlockchainDeploymentResult
import com.chromia.cli.schema.BlockchainConfigSchemaParser
import com.chromia.cli.schema.SchemaComparator
import com.chromia.cli.schema.ReportGenerator
import com.chromia.directory1.cm_api.cmGetBlockchainApiUrls
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.mordant.terminal.YesNoPrompt
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.exception.ClientError
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import org.http4k.core.Status

class DeployUpdateCommand(
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
        ) : AbstractDeploymentCommand(name = "update", help = "Update configuration of a deployed blockchain", clientProvider) {
    private val height by option(help = "Deploy configuration at a specific height").long().validate {
        require(blockchain?.size == 1 || deployModel.chains.size == 1) { "When deploying to a specific height, only one blockchain can be updated at a time. use --blockchain flag to specify" }
    }
    private val verifyOnly by option("--verify-only", help = "Verifies blockchain config without sending update transaction").flag()
    private val skipVerification by option("--skip-verification", help = "Skip verification of blockchain config before sending update transaction").flag()

    override fun preDeploymentVerification(compiledChains: Collection<BlockchainConfiguration>) {
        if (skipVerification) {
            echo("Skipping verification of blockchain config")
            return
        }
        val messages = compiledChains.mapNotNull { chain -> verifyConfiguration(chain, client) }
        if (messages.isNotEmpty()) {
            throw PrintMessage(messages.joinToString("\n"), statusCode = 1)
        }
        if (verifyOnly) throw PrintMessage("Verification only, skipping sending updates", 0)
    }

    override fun performDeploymentOperation(configurations: List<BlockchainConfiguration>): List<BlockchainDeploymentResult> {
        return ChromiaDeploymentApi.update(::printer, deployModel, settings.config, configurations, !noCompression, height)
    }

    override fun afterDeployment(deployTxs: List<BlockchainDeploymentResult>) {
        for (tx in deployTxs) {
            echo("Blockchain ${tx.blockchain.name} ${if (tx.success) "was successfully updated" else "failed update"} on network ${networkTarget.network}")
        }
    }

    override fun explicitChainsToDeploy(): Collection<String> {
        val chains = settings.model.deployments[networkTarget.network]?.chains?.keys
        if (chains.isNullOrEmpty()) {
            throw PrintMessage("No chains found in deployment ${networkTarget.network}", 1)
        }
        return chains
    }

    private fun verifyConfiguration(chain: BlockchainConfiguration, directoryChainClient: PostchainClient): String? {
        val blockchainRid = deployModel.chains[chain.name]
                ?: throw PrintMessage("Blockchain ${chain.name} cannot be updated since it has not been deployed to network ${networkTarget.network}. Specify target blockchain rid in chromia.yml")

        val urls = directoryChainClient.cmGetBlockchainApiUrls(blockchainRid).toList()
        require(urls.isNotEmpty()) {
            "No urls found for blockchain with brid: '${blockchainRid}', from directory-chain with brid '${directoryChainClient.config.blockchainRid}'"
        }
        val endpoint = EndpointPool.default(urls)

        val nodeClient = clientProvider.createClient(directoryChainClient.config.copy(blockchainRid, endpoint))

        try {
            nodeClient.validateConfiguration(chain.config)
            echo("Blockchain ${chain.name} was successfully verified against deployed chain on network ${networkTarget.network}")
        } catch (e: ClientError) {
            return when (e.status) {
                Status.UNAUTHORIZED -> "Node rejected request. You might need to update your chr to latest version. Reason: ${e.errorMessage}"
                Status.FORBIDDEN -> "You do not have access to validate configuration against this blockchain. Make sure the configured keypair match the blockchain provider/owner and try again."
                else -> "Blockchain ${chain.name} cannot be updated on network ${networkTarget.network}. Reason: ${e.errorMessage}"
            }
        }

        validateSchema(chain, nodeClient)

        return null
    }

    private fun validateSchema(chain: BlockchainConfiguration, nodeClient: PostchainClient) {
        val newConfig = chain.config
        val deployedConfig = nodeClient.getConfiguration()

        val configSchemaParser = BlockchainConfigSchemaParser()
        val newSchema = configSchemaParser.parse(newConfig)
        val deployedSchema = configSchemaParser.parse(deployedConfig)

        val schemaComparator = SchemaComparator()
        val comparison = schemaComparator.compareSchemas(deployedSchema, newSchema)
        if (comparison.entityDifferences.isEmpty() && comparison.enumDifferences.isEmpty()) {
            echo("No schema changes detected for ${chain.name} on network ${networkTarget.network}")
            return
        }

        val reportGenerator = ReportGenerator()
        val schemaChangesReport = reportGenerator.getSchemaChangesReport(comparison, chain.name)
        echo(schemaChangesReport.report)

        if (schemaChangesReport.containsUnsafeChanges && !verifyOnly) {
            if (terminal.terminalInfo.inputInteractive) {
                if (YesNoPrompt("This update of ${chain.name} on network ${networkTarget.network} includes potentially dangerous database schema modifications that could result in data loss or corruption. " +
                                "Are you sure you want to proceed?",
                                terminal, default = false
                        ).ask() != true) throw PrintMessage("Deployment update was aborted")
            } else {
                throw CliktError("Please specify --skip-verification option to skip update verification")
            }
        }
    }

    private fun printer(isError: Boolean, message: String) {
        if (isError) {
            throw PrintMessage(message, 1)
        } else {
            echo(message)
        }
    }
}
