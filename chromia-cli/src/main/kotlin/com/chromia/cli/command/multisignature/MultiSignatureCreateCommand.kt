package com.chromia.cli.command.multisignature

import com.chromia.build.tools.multisignature.MultiSignatureTxData
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.ft.findFtAccountIdWithAuthDescriptorId
import com.chromia.cli.tools.multisignature.saveTransactionToFile
import com.chromia.cli.tools.util.SUPPORTED_TIME_AT_FORMATS
import com.chromia.cli.tools.util.signersOption
import com.chromia.cli.tools.util.timeAtConverter
import com.chromia.cli.tools.util.timebOptions
import com.chromia.cli.util.ExplicitDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.initFtAuthVerbose
import com.chromia.cli.util.parseArgAsGtv
import com.chromia.lib.ft4.external.auth.FT_AUTH
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.data.Hash
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtx.GtxBuilder
import java.nio.file.Paths
import java.time.Clock

class MultiSignatureCreateCommand : ChromiaCommand(name = "create", help = "Creates a new transaction for multi signature and signs it with your key") {

    private val settings by optionalChromiaModelConfigOption()
    private val explicitTarget by ExplicitDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()

    private val ftAuthOptions by object : OptionGroup("FT compatible dapps options") {
        val ftAuth by option(help = "Adds ft4.ft_auth operation for FT-compatible dapps").flag()
        val ftAccountId by option(help = "Explicitly specify which account to use")
        val ftAuthDescriptorId by option("--auth-descriptor-id", "-id", help = "Explicitly specify which auth descriptor id to use")
    }
    private val keyPairSource by keyPairSourceOption()

    private val signers by signersOption().required()

    private val outputFolder by option("--target", help = "Path where file should be saved")
            .file()
            .default(Paths.get("").toAbsolutePath().toFile())

    private val timebFrom by option("--timeb-from", help = "Add timeb operation to make transaction fail if applied before the given time (UTC). $SUPPORTED_TIME_AT_FORMATS")
            .convert { timeAtConverter(it) }

    private val timebUntil by timebOptions(Clock.systemUTC())

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
            .transformAll { args -> args.map(::parseArgAsGtv) }

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val postchainClientConfig = settings.config.setApiUrls(target.urls).setBrid(target.brid)
        postchainClientConfig.configureSigners(keyPairSource)
        val client = target.createClient(postchainClientConfig)

        val initialSigner = postchainClientConfig.signers
        val signersWithoutInitialSigner = signers.filterNot { signer -> initialSigner.any { it.pubKey == signer } }

        echo("Creating transaction with signers: ${initialSigner.map { it.pubKey } + signersWithoutInitialSigner}")
        val transactionBuilder = GtxBuilder(
                client.config.blockchainRid,
                signers = initialSigner.map { it.pubKey.data } + signersWithoutInitialSigner.map { it.data },
                client.config.cryptoSystem,
                client.merkleHashCalculator
        )

        if (timebFrom != null || timebUntil != null) {
            transactionBuilder.addOperation("timeb", gtv(timebFrom ?: 0), timebUntil?.let { gtv(it) } ?: GtvNull)
        }

        if (ftAuthOptions.ftAuth) {
            require(ftAuthOptions.ftAuthDescriptorId != null) { "Must specify auth descriptor id when using ft auth for multi signature" }
            initFtAuthVerbose(client, target.blockchain, target.brid.toHex())

            val signerPubkey = (postchainClientConfig.signers.singleOrNull()?.pubKey
                    ?: throw PrintMessage("A single keypair is required to use FT authentication", statusCode = 1))
            val (accountId, authDescriptorId) = findFtAccountIdWithAuthDescriptorId(client, ftAuthOptions.ftAccountId?.hexStringToByteArray(),
                    signerPubkey.data, opName,
                    ftAuthOptions.ftAuthDescriptorId?.hexStringToByteArray()
            )
            transactionBuilder.addOperation(FT_AUTH, gtv(accountId), gtv(authDescriptorId))
        }

        val signatureBuilder = transactionBuilder.addOperation(opName, *args.toTypedArray())
                .addNop()
                .uncheckedSignBuilder()
        val txRid: Hash = signatureBuilder.txRid
        val transaction: ByteArray = signatureBuilder
                .apply {
                    initialSigner.forEach { sign(it) }
                }
                .buildGtx().encode()

        val file = MultiSignatureTxData(transaction, txRid).saveTransactionToFile(outputFolder, "${opName}_transaction")
        echo("Transaction is written as hex to file: ${file.absolutePath}")
    }

}
