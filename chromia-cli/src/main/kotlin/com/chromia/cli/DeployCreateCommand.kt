package com.chromia.cli

import com.chromia.build.tools.compile.ChromiaCompileResult
import com.chromia.cli.compatibility.BlockchainOperations
import com.chromia.cli.util.CliktClusterManagement
import com.chromia.cli.util.ClusterManagementFactory
import com.chromia.cli.util.apiVersion
import com.chromia.cli.util.pubkey
import com.chromia.directory1.proposal_blockchain.findBlockchainRid
import com.chromia.directory1.version.apiVersion
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.YesNoPrompt
import net.postchain.base.gtv.GtvToBlockchainRidFactory
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.core.TxRid
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.cm.cm_api.ClusterManagementImpl
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.crypto.sha256Digest
import org.http4k.core.HttpHandler

class DeployCreateCommand(
        private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { DeployInfoCommand.httpHandlerFactory(it) },
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
) : AbstractDeploymentCommand(name = "create", help = "Deploy blockchain into container", clientProvider) {
    private val confirm by option("-y", help = "Confirm that this will create a new deployment").flag()

    override fun beforeDeployment(compiledChains: Collection<ChromiaCompileResult>, client: PostchainClient) {

        validateRellVersion(client, httpHandlerFactory)

        compiledChains.forEach { chain ->
            if (deployModel.chains.containsKey(chain.name)) throw PrintMessage("Blockchain ${chain.name} is already deployed to network $target")
            if (!confirm && YesNoPrompt("This will create a new deployment of ${chain.name} on network $target. Would you like to create a new deployment?",
                            terminal, default = false
                    ).ask() != true) throw PrintMessage("Deployment was aborted")
        }
    }

    override fun afterDeployment(client: PostchainClient, deployTxs: List<Pair<ChromiaCompileResult, TxRid>>) {
        val apiVersion = client.apiVersion()

        val deployedChains = buildList {
            for ((chain, tx) in deployTxs) {
                val maybeBcRid = if (apiVersion >= 8) {
                    client.findBlockchainRid(tx.rid.hexStringToByteArray())?.let { BlockchainRid(it) }
                } else {
                    GtvToBlockchainRidFactory.calculateBlockchainRid(chain.config, ::sha256Digest)
                }
                if (maybeBcRid != null) {
                    echo("Deployment of blockchain ${chain.name} was successful")
                    add(chain.name to maybeBcRid)
                } else {
                    echo("Deployment of blockchain ${chain.name} was proposed, tx-rid: ${tx.rid}")
                }
            }
        }

        echo("""
            Add the following to your project settings file:
            deployments:
              $target:
                chains:
                  ${deployedChains.joinToString("\n      ") { "${it.first}: x\"${it.second.toHex()}\"" }}
            """.trimIndent())
    }

    override fun TransactionBuilder.addDeploymentOperation(client: PostchainQuery, clientConfig: PostchainClientConfig, configHolder: ChromiaCompileResult) {
        BlockchainOperations(client.apiVersion, this)
                .newBlockchainOperation(clientConfig.pubkey.data, configHolder.configByteArray, configHolder.name, deployModel.container!!)
    }

    companion object : ClusterManagementFactory {
        override fun buildClusterManagement(client: PostchainQuery) = CliktClusterManagement(ClusterManagementImpl(client))
    }
}
