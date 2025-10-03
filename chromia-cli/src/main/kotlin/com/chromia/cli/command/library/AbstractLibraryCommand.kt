package com.chromia.cli.command.library

import com.chromia.api.ChromiaCompileApi
import com.chromia.api.filterBlockchains
import com.chromia.build.tools.lib.DirectoryHashCalculator
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.ft.addFtAuthOperation
import com.chromia.cli.tools.ft.findFtAccountIdAndAuthDescriptorId
import com.chromia.cli.tools.ft.initFtAuth
import com.chromia.cli.util.BuildCliEnv
import com.chromia.cli.util.LibraryChainNetworkUtils.CHROMIA_MAINNET
import com.chromia.cli.util.LibraryChainNetworkUtils.libraryPredefinedNetworks
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

abstract class AbstractLibraryCommand(
    name: String? = null,
    help: String,
    hideKeyPairSourceHelpMessage: Boolean = false
) : ChromiaCommand(name, help) {

    protected val settings by optionalChromiaModelConfigOption()

    private val keyPairSource by keyPairSourceOption(hideKeyPairSourceHelpMessage)

    protected val remoteTarget by RemoteTargetOptions()

    protected val client by lazy {
        createConfiguredClient(remoteTarget.url, remoteTarget.brid)
    }

    protected val pubkey by lazy {
        client.config.signers
            .firstOrNull()
            ?.pubKey?.data
            ?: throw PrintMessage("Signer is needed to proceed with this transaction", statusCode = 1)
    }

    protected val txBuilder by lazy {
        client.transactionBuilder()
    }

    fun createConfiguredClient(url: String? = null, brid: BlockchainRid? = null): PostchainClient {
        val inputUrl = url ?: remoteTarget.url ?: CHROMIA_MAINNET

        val networkConfig = inputUrl.let { libraryPredefinedNetworks[it]?.invoke() }

        val targetUrl = networkConfig?.url ?: inputUrl
        val targetBrid = brid ?: remoteTarget.brid
            ?: networkConfig?.brid
            ?: throw PrintMessage("Brid of library_chain is required")

        settings.config.configureSigners(keyPairSource)

        val postchainConfig = PostchainClientConfig.defaultConfig
            .copy(
                signers = settings.config.signers,
                endpointPool = EndpointPool.singleUrl(targetUrl),
                blockchainRid = targetBrid
            )
        return PostchainClientProviderImpl().createClient(postchainConfig)
    }

    protected fun authorizeFtAuthOperation(
        operationName: String,
        optionalAccountId: String? = null
    ): Pair<ByteArray, ByteArray> {
        initFtAuth(client)

        val (accountId, authDescriptorId) = findFtAccountIdAndAuthDescriptorId(
            client,
            optionalAccountId,
            pubkey,
            operationName,
            null
        )

        addFtAuthOperation(txBuilder, accountId, authDescriptorId)

        return accountId to authDescriptorId
    }

    class RemoteTargetOptions : OptionGroup("Library chain deployment options") {
        val url by option(
            "--url",
            help = "Url where library-chain is deployed. Ex: testnet, localhost, https://custom-network.chromia.dev:7740"
        )
        val brid by option(
            "--brid",
            "-b",
            help = "Brid (hex string) of library-chain"
        ).convert {
            BlockchainRid.buildFromHex(it)
        }
    }

    fun collectFiles(libDirectory: Path): Map<String, ByteArray> {
        echo("Uploading files from: $libDirectory")

        val (rellFiles, nonRellFiles) = Files.walk(libDirectory)
            .asSequence()
            .filter { it.isRegularFile() }
            .partition { it.extension == "rell" }

        val rellFilesMap = rellFiles
            .associateWith { it.readBytes() }
            .mapKeys { (file, _) ->
                file.relativeTo(libDirectory).toString()
            }

        nonRellFiles.forEach { file ->
            val relativePath = file.relativeTo(libDirectory).toString()
            echo("Skipping non-rell file -> [$relativePath]")
        }

        return rellFilesMap
    }

    fun calculateRid(libDir: Path): ByteArray {
        val calculator = DirectoryHashCalculator(libDir)
        val rid = calculator.compute(libDir, DirectoryHashCalculator.RidStrategy.LIST)
        return rid.data
    }

    protected fun validateLibraryCode(libraryName: String) {
        val model = settings.model
            ?: throw PrintMessage("No chromia.yml configuration found", statusCode = 1)

        model.blockchains[libraryName]
            ?.takeIf { it.type == BlockchainModel.Type.LIBRARY }
            ?: throw PrintMessage("Library '$libraryName' not found or not configured as library type", statusCode = 1)

        runCatching {
            val cliEnv = BuildCliEnv(this, hideLibWarnings = true)
            val compiledChains = ChromiaCompileApi.build(
                cliEnv,
                model.filterBlockchains(listOf(libraryName))
            )

            if (compiledChains.isEmpty()) {
                throw PrintMessage(
                    "No blockchain configurations were compiled for library: $libraryName",
                    statusCode = 1
                )
            }

            compiledChains.find { it.name == libraryName }
                ?: throw PrintMessage(
                    "Library '$libraryName' was not found in compiled configurations",
                    statusCode = 1
                )
        }.onFailure { error ->
            when (error) {
                is PrintMessage -> {
                    throw error
                }
                else -> throw PrintMessage(
                    "Compilation failed for library '$libraryName': ${error.message}",
                    statusCode = 1
                )
            }
        }
    }
}
