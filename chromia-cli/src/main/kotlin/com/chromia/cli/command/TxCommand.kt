package com.chromia.cli.command

import com.chromia.cli.ft.FTAuth
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.blockchain.BridFetcher
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.LocalDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.secretOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.TxRid
import net.postchain.client.defaultHttpHandler
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvString
import net.postchain.gtv.parse.GtvParser


class TxCommand : CliktCommand(help = """
    Make a transaction towards a node.
    
    Supports both specifying the target node using url and brid/id or from a deployment which is specified in the chromia.yml
    This will post the transaction asynchronously unless `--await` is specified, in which it will wait until transaction has been included in a block.
    
    FT4 compatibility:
    To make a transaction towards a dapp that uses ft-authentication, use the `--ft-auth` flag. 
    This only works if the signer keypair is connected to a ft-account with the correct authentication rules. Use `--ft-account-id` to explicitly state which account to use if keypair is connected to more than one account.
    Node: This does *not* work with evm-authentication
    
    ICCF:
    To verify a transaction using ICCF, specify the tx-rid to verify using `--iccf-tx` and which chain id the transaction was processed.
    The command will both construct and insert a `iccf_proof` operation prior to the user operation but will also add the the transaction as a `gtx_transaction` as first argument to the user operation.
""".trimIndent()) {

    private val settings by optionalChromiaModelConfigOption()
    private val secret by secretOption()
    private val explicitTarget by LocalDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()
    private val awaitConfirmation by option("--await", "-a", help = "Wait for transaction to be included in a block").flag()
    private val nop by option("-nop", help = "Adds a nop to the transaction").flag()
    private val ftAuthOptions by object : OptionGroup("FT compatible dapps options") {
        val ftAuth by option(help = "Adds ft4.ft_auth operation for FT-compatible dapps").flag()
        val ftAccountId by option(help = "Explicitly specify which account to use")
    }
    private val iccfTx by option(help = "Constructs a ICCF-proof for this tx-rid and inserts iccf_proof operation to the transaction. This will also add the tx as a gtx_transaction as first argument to the operation").validate { it.hexStringToWrappedByteArray() }
    private val iccfSource by option(help = "Chain id for the chain which the tx to be confirmed has taken place").int()

    private val opName by argument(help = "name of the operation to execute.")

    private val args by argument(help = "arguments to pass to the operation.", helpTags = mapOf(
            "integer" to "123",
            "big_integer" to "1234L",
            "string" to "foo, \"bar\"",
            "bytearray" to "will be encoded using the rell notation x\"<myByteArray>\" and will initially be interpreted as a hex-string.",
            "array" to "[foo,123]",
            "dict" to """["key1":value1,"key2":value2]"""
    ))
            .multiple()
            .transformAll { args ->
                args.map {
                    try {
                        GtvParser.parse(it)
                    } catch (e: IllegalArgumentException) {
                        GtvString(it)
                    }
                }
            }

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val postchainClientConfig = settings.config.setApiUrls(target.url).setBrid(target.brid)
        secret?.let { postchainClientConfig.setSignerFromSecret(it.toPath()) }
        val client = target.createClient(postchainClientConfig)
        val transactionBuilder = client.transactionBuilder()
        val args = if (iccfTx != null) {
            require(iccfSource != null) { "Chain id for iccf transaction must be specified" }
            val sourceBrid = bridFetcher(client.config).fetchBlockchainRid(iccfSource!!)
            val sourceClient = target.createClient(postchainClientConfig.setBrid(sourceBrid))
            val proof = sourceClient.confirmationProof(TxRid(iccfTx!!))
            val txHash = GtvDecoder.decodeGtv(proof)["hash"]!!
            val tx = sourceClient.getTransaction(TxRid(iccfTx!!))
            transactionBuilder.addOperation("iccf_proof", GtvFactory.gtv(sourceBrid), txHash, GtvFactory.gtv(proof))
            listOf(GtvFactory.decodeGtv(tx)) + args
        } else args
        if (ftAuthOptions.ftAuth) {
            val authenticator = FTAuth.createFTAuthenticator(client, terminal)
            val signerPubkey = (postchainClientConfig.signers.singleOrNull()?.pubKey
                    ?: throw PrintMessage("A single keypair is required to use FT authentication", statusCode = 1))
            authenticator.addAuthenticationOperation(transactionBuilder, opName, signerPubkey, ftAuthOptions.ftAccountId)
        }

        val res = transactionBuilder
                .addOperation(opName, *args.toTypedArray())
                .run {
                    if (nop) addNop()
                    if (awaitConfirmation) postAwaitConfirmation() else post()
                }
        if (res.status == TransactionStatus.REJECTED || res.status == TransactionStatus.UNKNOWN)
            throw PrintMessage("Transaction Failed with code ${res.httpStatusCode}: ${res.rejectReason}", statusCode = 1)
        echo("transaction with rid ${res.txRid.rid} was posted ${res.status}${res.rejectReason?.let { ": $it" } ?: ""}")
    }

    private fun bridFetcher(clientConfig: PostchainClientConfig) = BridFetcher(defaultHttpHandler(clientConfig), clientConfig.endpointPool.first().url)
}
