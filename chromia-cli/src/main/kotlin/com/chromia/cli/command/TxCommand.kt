package com.chromia.cli.command

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.ft.addEvmAuthOperation
import com.chromia.cli.tools.ft.addFtAuthOperation
import com.chromia.cli.tools.ft.findFtAccountIdAndAuthDescriptorId
import com.chromia.cli.tools.ft.initFtAuth
import com.chromia.cli.util.LocalDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.evmAuthOption
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import net.postchain.client.core.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.GtvFactory.decodeGtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvString
import net.postchain.gtv.parse.GtvParser


class TxCommand : ChromiaCommand(help = """
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
    
    Examples:
    ```
    # operation primitive_args(arg1: integer, arg2: name, arg3: text, arg4: byte_array, arg5: my_enum)
    chr tx primitive_args 123 Alice "My Neighbor" 'x"AB12"' 0
    # operation dict_arg(arg: map<text, integer>)
    chr tx dict_arg '["key": 12]'
    # operation map_arg(arg: map<my_enum, text>)
    chr tx map_arg '[[0, "first"],[1, "second"]]'
    # operation struct_arg(arg: my_struct)
    chr tx struct_arg '[12, "structs are arrays", x"AB"]'
    ```
""".trimIndent()) {

    private val settings by optionalChromiaModelConfigOption()
    private val keyPairSource by keyPairSourceOption()
    private val explicitTarget by LocalDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()
    private val awaitConfirmation by option("--await", "-a", help = "Wait for transaction to be included in a block").flag("--no-await", default = true)
    private val nop by option("-nop", help = "Adds a nop to the transaction").flag()
    private val ftAuthOptions by object : OptionGroup("FT compatible dapps options") {
        val ftAuth by option(help = "Adds ft4.ft_auth operation for FT-compatible dapps").flag()
        val ftAccountId by option(help = "Explicitly specify which account to use")
        val evmAuth by evmAuthOption()
    }
    private val iccfTx by option(help = "Constructs a ICCF-proof for this tx-rid and inserts iccf_proof operation to the transaction. This will also add the tx as a gtx_transaction as first argument to the operation").validate { it.hexStringToWrappedByteArray() }
    private val iccfSource by option(help = "Blockchain RID for the chain which the tx to be confirmed has taken place").convert {
        BlockchainRid.buildFromHex(it)
    }

    private val opName by argument(help = "Name of the operation to execute.")

    private val args by argument(help = "Types and their format as arguments to pass to the operation.", helpTags = mapOf(
            "integer" to "123",
            "big_integer" to "1234L",
            "decimal" to "\"1.2\"",
            "text" to "foo, \"bar\"",
            "byte_array" to "'x\"<myByteArray>\"'",
            "list" to "'[\"foo\",123]'",
            "map<text, ...>" to "'[\"text_key\":value1,\"text_key2\":value2]'",
            "map<non_text_key_type, ...>" to "'[[non_text_key, value1], [non_text_key, value2]]'",
            "struct" to "'[\"foo\"]'"
    )
    )
            .multiple()
            .transformAll { args ->
                args.map {
                    try {
                        GtvParser.parse(it)
                    } catch (_: IllegalArgumentException) {
                        GtvString(it)
                    }
                }
            }

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val postchainClientConfig = settings.config.setApiUrls(target.urls).setBrid(target.brid)
        postchainClientConfig.configureSigners(keyPairSource)
        val client = target.createClient(postchainClientConfig)
        val transactionBuilder = client.transactionBuilder()
        val args = if (iccfTx != null) {
            require(iccfSource != null) { "Chain id for iccf transaction must be specified" }
            val sourceClient = target.createClient(postchainClientConfig.setBrid(iccfSource!!))
            val proof = sourceClient.confirmationProof(TxRid(iccfTx!!))
            val txHash = GtvDecoder.decodeGtv(proof)["hash"]!!
            val tx = sourceClient.getTransaction(TxRid(iccfTx!!))
            transactionBuilder.addOperation("iccf_proof", gtv(iccfSource!!), txHash, gtv(proof))
            listOf(decodeGtv(tx)) + args
        } else args

        if (ftAuthOptions.ftAuth || ftAuthOptions.evmAuth != null) {
            val signer = ftAuthOptions.evmAuth
                    ?: (postchainClientConfig.signers.singleOrNull()?.pubKey?.data
                            ?: throw PrintMessage("A single keypair is required to use FT authentication", statusCode = 1))

            initFtAuth(client)

            val (accountId, authDescriptorId) = findFtAccountIdAndAuthDescriptorId(
                    client,
                    ftAuthOptions.ftAccountId,
                    signer,
                    opName,
                    null)

            if (ftAuthOptions.evmAuth != null) {
                addEvmAuthOperation(client, transactionBuilder, opName, args, ftAuthOptions.evmAuth!!, accountId, authDescriptorId)
            } else {
                addFtAuthOperation(transactionBuilder, accountId, authDescriptorId)
            }
        }

        val res = transactionBuilder.addOperation(opName, *args.toTypedArray()).run {
            if (nop) addNop()
            if (awaitConfirmation) postAwaitConfirmation() else post()
        }
        if (res.status == TransactionStatus.REJECTED || res.status == TransactionStatus.UNKNOWN) throw PrintMessage("Transaction Failed with code ${res.httpStatusCode}: ${res.rejectReason}", statusCode = 1)
        echo("transaction with rid ${res.txRid.rid} was posted ${res.status}${res.rejectReason?.let { ": $it" } ?: ""}")
    }
}
