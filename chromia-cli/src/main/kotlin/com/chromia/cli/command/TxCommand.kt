package com.chromia.cli.command

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.ft.addEvmAuthOperation
import com.chromia.cli.tools.ft.addFtAuthOperation
import com.chromia.cli.tools.ft.addFtRegisterAccountOperation
import com.chromia.cli.tools.ft.findFtAccountIdAndAuthDescriptorId
import com.chromia.cli.tools.util.timebOptions
import com.chromia.cli.util.ExplicitDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.evmAuthOption
import com.chromia.cli.util.initFtAuthVerbose
import com.chromia.cli.util.parseArgAsGtv
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import net.postchain.client.core.TxRid
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.d1.client.StandardChromiaClient
import java.time.Clock

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
    private val explicitTarget by ExplicitDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()
    private val awaitConfirmation by option("--await", "-a", help = "Wait for transaction to be included in a block").flag("--no-await", default = true)
    private val nop by option("-nop", help = "Adds a nop to the transaction").flag()
    private val timeb by timebOptions(Clock.systemUTC())
    private val ftAuthOptions by object : OptionGroup("FT compatible dapps options") {
        val ftAuth by option(help = "Adds ft4.ft_auth operation for FT-compatible dapps").flag()
        val ftAccountId by option(help = "Explicitly specify which account to use")
        val evmAuth by evmAuthOption()
        val ftRegisterAccount by option(help = "Adds ft4.register_account operation. To be used in combination with an account creation strategy.").flag()
    }
    private val iccfOptions by object : OptionGroup("ICCF options") {
        val iccfTx by option(help = "Constructs a ICCF-proof for this tx-rid and inserts iccf_proof operation to the transaction. This will also add the tx as a gtx_transaction as argument to the operation").validate { it.hexStringToWrappedByteArray() }
        val iccfSource by option(help = "Blockchain RID for the chain which the tx to be proven has taken place").convert {
            BlockchainRid.buildFromHex(it)
        }
        val sourceApiUrl by option(help = "Source api url (default target api url)")
        val iccfForceIntraNetwork by option(help = "Force usage of intra-network ICCF proof").flag()
        val iccfArgPos by option(help = "Which argument position to insert the gtx_transaction in the operation").int()
                .default(0)
                .validate { require(it >= 0) { "Cannot be negative" } }
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
            .transformAll { args -> args.map(::parseArgAsGtv) }

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val postchainClientConfig = settings.config.setApiUrls(target.urls).setBrid(target.brid)
        postchainClientConfig.configureSigners(keyPairSource)
        val client = target.createClient(postchainClientConfig)
        val newArgs = args.toMutableList()
        val transactionBuilder = client.transactionBuilder()

        if (iccfOptions.iccfTx != null) {
            require(iccfOptions.iccfSource != null) { "Source chain for iccf transaction must be specified" }

            val chromiaClient = if (iccfOptions.sourceApiUrl != null) {
                StandardChromiaClient(EndpointPool.singleUrl(iccfOptions.sourceApiUrl!!))
            } else {
                StandardChromiaClient(target.createDirectoryClient(postchainClientConfig).config)
            }

            val provenTx = chromiaClient.addIccfProof(
                    transactionBuilder,
                    TxRid(iccfOptions.iccfTx!!),
                    iccfOptions.iccfSource!!,
                    forceIntraNetworkIccfOperation = iccfOptions.iccfForceIntraNetwork
            )

            newArgs.add(iccfOptions.iccfArgPos, provenTx)
        }

        timeb?.let {
            transactionBuilder.addTimeBound(0, it)
        }

        if (ftAuthOptions.ftAuth || ftAuthOptions.evmAuth != null) {
            val signer = ftAuthOptions.evmAuth
                    ?: (postchainClientConfig.signers.singleOrNull()?.pubKey?.data
                            ?: throw PrintMessage("A single keypair is required to use FT authentication", statusCode = 1))

            initFtAuthVerbose(client, target.blockchain, target.brid.toHex())

            val (accountId, authDescriptorId) = findFtAccountIdAndAuthDescriptorId(
                    client,
                    ftAuthOptions.ftAccountId,
                    signer,
                    opName,
                    null)

            if (ftAuthOptions.evmAuth != null) {
                addEvmAuthOperation(client, transactionBuilder, opName, newArgs, ftAuthOptions.evmAuth!!, accountId, authDescriptorId)
            } else {
                addFtAuthOperation(transactionBuilder, accountId, authDescriptorId)
            }
        }

        val res = transactionBuilder.addOperation(opName, *newArgs.toTypedArray()).run {
            if (ftAuthOptions.ftRegisterAccount) addFtRegisterAccountOperation(this)
            if (nop) addNop()
            if (awaitConfirmation) postAwaitConfirmation(txListener()) else post()
        }
        when (res.status) {
            TransactionStatus.UNKNOWN ->
                echo("transaction with rid ${res.txRid.rid} was posted but has unknown status")

            TransactionStatus.WAITING ->
                echo("transaction with rid ${res.txRid.rid} was posted but is still pending")

            TransactionStatus.CONFIRMED ->
                echo("transaction with rid ${res.txRid.rid} was posted and confirmed")

            TransactionStatus.REJECTED ->
                throw PrintMessage("Transaction was rejected ${if (!awaitConfirmation || res.httpStatusCode == 400) "immediately" else "after polling"}: ${res.rejectReason}", statusCode = 1)
        }
    }
}
