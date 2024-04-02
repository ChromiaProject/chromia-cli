package com.chromia.cli.command.deployment

import com.chromia.build.tools.compile.ChromiaCompileResult
import com.chromia.cli.compatibility.BlockchainOperations
import com.chromia.cli.util.CliktClusterManagement
import com.chromia.cli.util.ClusterManagementFactory
import com.chromia.cli.util.HeightFinder
import com.chromia.cli.util.apiVersion
import com.chromia.cli.util.pubkey
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.core.TxRid
import net.postchain.client.defaultHttpHandler
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.cm.cm_api.ClusterManagementImpl
import net.postchain.gtv.gtvml.GtvMLEncoder
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status

class DeployUpdateCommand(
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
        private val clusterManagementFactory: ClusterManagementFactory = Companion,
        private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { Companion.httpHandlerFactory(it) }

) : AbstractDeploymentCommand(name = "update", help = "Update configuration of a deployed blockchain", clientProvider) {
    private val height by option(help = "Deploy configuration at a specific height").long().validate {
        require(blockchain?.size == 1 || deployModel.chains.size == 1) { "When deploying to a specific height, only one blockchain can be updated at a time. use --blockchain flag to specify" }
    }
    private val verifyOnly by option("--verify-only", help = "Verifies blockchain config without sending update transaction").flag()
    private val skipVerification by option("--skip-verification", help = "Skip verification of blockchain config before sending update transaction").flag()

    override fun beforeDeployment(compiledChains: Collection<ChromiaCompileResult>, client: PostchainClient) {
        validateRellVersion(client, httpHandlerFactory)
        if (skipVerification) {
            echo("Skipping verification of blockchain config")
            return
        }
        compiledChains.forEach { chain -> verifyConfiguration(chain, client) }
    }

    override fun afterDeployment(client: PostchainClient, deployTxs: List<Pair<ChromiaCompileResult, TxRid>>) {
        for ((chain, _) in deployTxs) {
            echo("Blockchain ${chain.name} was successfully updated on network $target")
        }
    }

    private fun verifyConfiguration(chain: ChromiaCompileResult, client: PostchainClient) {
        val blockchainRid = deployModel.chains[chain.name]
                ?: throw PrintMessage("Blockchain ${chain.name} cannot be updated since it has not been deployed to network $target. Specify target blockchain rid in chromia.yml")

        val compiledConfig = GtvMLEncoder.encodeXMLGtv(configToDeploy(chain).config)
        val httpHandler = httpHandlerFactory(client.config)
        val clusterManagement = clusterManagementFactory.buildClusterManagement(client)

        val endpoint = EndpointPool.default(clusterManagement.getBlockchainApiUrls(blockchainRid).toList())
        val request = Request(Method.POST, "${endpoint.first().url.trimEnd().replace(Regex("/$"), "")}/config/${blockchainRid.toHex()}")
                .body(compiledConfig)

        val result = httpHandler(request)
        when (result.status) {
            Status.OK -> {
                echo("Blockchain ${chain.name} was successfully verified against deployed chain on network $target")
            }

            Status.BAD_REQUEST, Status.NOT_FOUND -> {
                echo(result.body.toString())
                throw PrintMessage("Blockchain ${chain.name} cannot be updated on network $target. Code is not compatible with deployed version", 1)
            }

            else -> {
                echo("Unexpected status code: ${result.status.code} \nBody: ${result.body} ")
                throw PrintMessage("Blockchain ${chain.name} can not be updated on network. Unexpected status code: ${result.status.code} \n" +
                        "Body: ${result.body}")
            }
        }
        if (verifyOnly) throw PrintMessage("Verification only, skipping sending updates", 0)
    }

    override fun TransactionBuilder.addDeploymentOperation(client: PostchainQuery, clientConfig: PostchainClientConfig, configHolder: ChromiaCompileResult) {
        val clusterManagement = clusterManagementFactory.buildClusterManagement(client)
        val heightChecker by lazy { HeightFinder(clientProvider, clientConfig, clusterManagement) }
        val blockchainRid = deployModel.chains[configHolder.name]!!
        BlockchainOperations(client.apiVersion, this, heightChecker)
                .proposeConfiguration(clientConfig.pubkey.data, blockchainRid, configHolder.configByteArray, clusterManagement.getClusterOfBlockchain(blockchainRid), height, true)
    }

    companion object : ClusterManagementFactory {
        override fun buildClusterManagement(client: PostchainQuery) = CliktClusterManagement(ClusterManagementImpl(client))

        fun httpHandlerFactory(config: PostchainClientConfig) = defaultHttpHandler(config)
    }
}
