package com.chromia.cli.command.deployment

import com.chromia.api.ChromiaDeploymentApi
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.BlockchainDeploymentResult
import com.chromia.build.tools.model.writer.ChromiaYmlWriter
import com.chromia.build.tools.model.writer.DeploymentUpdate
import com.chromia.cli.util.printChromiaYmlDiff
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.YesNoPrompt
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl

class DeployCreateCommand(
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
) : AbstractDeploymentCommand(name = "create", help = "Deploy new blockchain instance", clientProvider) {
    private val confirm by option("-y", help = "Confirm that this will create a new deployment").flag()

    override fun preDeploymentVerification(compiledChains: Collection<BlockchainConfiguration>) {
        compiledChains.forEach { chain ->
            if (deployModel.chains.containsKey(chain.name)) throw PrintMessage("Blockchain '${chain.name}' is already defined in the configuration file: '${settings.modelFile}' under the deployment: '${networkTarget.network}'")
            if (!confirm) {
                if (terminal.terminalInfo.inputInteractive) {
                    if (YesNoPrompt("This will create a new deployment of ${chain.name} on network ${networkTarget.network}. Would you like to create a new deployment?",
                                    terminal, default = false
                            ).ask() != true) throw PrintMessage("Deployment was aborted")
                } else {
                    throw CliktError("Please specify -y option to force deployment")
                }
            }
        }
    }

    override fun performDeploymentOperation(configurations: List<BlockchainConfiguration>): List<BlockchainDeploymentResult> {
        return ChromiaDeploymentApi.create(::printer, deployModel, settings.config, configurations, !noCompression)
    }

    override fun afterDeployment(deployTxs: List<BlockchainDeploymentResult>) {
        val successfulDeployments = deployTxs.filter { it.success && it.blockchainRid != null }
        try {
            successfulDeployments
                .map { DeploymentUpdate(networkTarget.network, it.blockchain.name, it.blockchainRid!!) }
                .let { deploymentsUpdate ->
                    ChromiaYmlWriter.updateDeploymentNodes(settings.modelFile, deploymentsUpdate) {diff ->
                        terminal.printChromiaYmlDiff(settings.modelFile, diff)
                    }
                }
        } catch (e: Exception) {
            if (successfulDeployments.isNotEmpty()) {
                echo("""
                Failed to update chromia.yml file, because: ${e.message}.
                
                Add the following to your project settings file manually:
                deployments:
                  ${networkTarget.network}:
                    chains:
                      ${successfulDeployments.joinToString("\n                      ") { "${it.blockchain.name}: x\"${it.blockchainRid!!.toHex()}\"" }}
                """.trimIndent())
            }
        }
    }

    override fun explicitChainsToDeploy(): Collection<String> {
        return settings.model.blockchains.keys
    }

    private fun printer(isError: Boolean, message: String) {
        if (isError) {
            throw PrintMessage(message, 1)
        } else {
            echo(message)
        }
    }
}
