package com.chromia.cli.ft

import com.chromia.directory1.lib.ft4.core.accounts.AuthType
import com.chromia.directory1.lib.ft4.core.auth.Signature
import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.chromia.directory1.lib.ft4.external.accounts.getAccountAuthDescriptorsBySigner
import com.chromia.directory1.lib.ft4.external.accounts.getAccountsBySigner
import com.chromia.directory1.lib.ft4.external.accounts.getAuthDescriptorCounter
import com.chromia.directory1.lib.ft4.external.auth.evmAuthOperation
import com.chromia.directory1.lib.ft4.external.auth.ftAuthOperation
import com.chromia.directory1.lib.ft4.external.auth.getAuthFlags
import com.chromia.directory1.lib.ft4.external.auth.getAuthMessageTemplate
import com.chromia.directory1.lib.ft4.utils.PagedResult
import com.chromia.directory1.lib.ft4.version.getVersion
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.input.interactiveSelectList
import com.github.ajalt.mordant.rendering.TextStyles.Companion.hyperlink
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainQuery
import net.postchain.client.exception.ClientError
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.hexStringToByteArray
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.toHex
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap
import net.postchain.crypto.sha256Digest
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculatorV1
import net.postchain.gtv.merkleHash
import org.apache.commons.text.StringEscapeUtils
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.webJars
import org.http4k.server.Netty
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.server.asServer
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

// TODO move to chromia-cli-tools

fun CoreCliktCommand.initFtAuth(client: PostchainQuery) {
    val version = try {
        client.getVersion()
    } catch (e: ClientError) {
        throw PrintMessage("Dapp is not FT4 compatible: ${e.errorMessage}", statusCode = 1)
    }
    // 0.0.* -> 0.3.*
    if (version.matches(Regex("^0\\.[0-3]\\.(0|[1-9]\\d*).*"))) {
        throw PrintMessage("Versions before release 0.4.0 are not supported, current FT4 version $version is to old", statusCode = 1)
    }
}

fun CoreCliktCommand.addFtAuthenticationOperation(client: PostchainQuery, transactionBuilder: TransactionBuilder, opName: String, signer: ByteArray, optionalAccountId: String? = null, optionalAuthDescriptorId: String? = null) {
    val (accountId, authDescriptorId) = findFtAccountIdAndAuthDescriptorId(client, optionalAccountId, signer, opName, optionalAuthDescriptorId)
    addFtAuthOperation(transactionBuilder, accountId, authDescriptorId)
}

fun CoreCliktCommand.findFtAccountIdAndAuthDescriptorId(client: PostchainQuery, optionalAccountId: String?, signer: ByteArray, opName: String, optionalAuthDescriptorId: String?): Pair<ByteArray, ByteArray> {
    val accountId = optionalAccountId?.hexStringToByteArray() ?: findAccountId(client, signer)
    return accountId to findValidAuthDescriptorIdForOperation(client, opName, accountId, signer, optionalAuthDescriptorId).data
}

private fun findAuthDescriptors(descriptors: List<Ft4GetAccountAuthDescriptorsBySignerResult>, optionalAuthDescriptorId: String?, signer: ByteArray): List<Ft4GetAccountAuthDescriptorsBySignerResult> {
    return descriptors.filter { descriptor ->
        when {
            !optionalAuthDescriptorId.isNullOrEmpty() -> {
                descriptor.id == optionalAuthDescriptorId.hexStringToWrappedByteArray()
            }

            descriptor.authType == AuthType.S -> {
                descriptor.getSingleKey().wrap() == signer.wrap()
            }

            descriptor.authType == AuthType.M -> {
                descriptor.getMultiKeys().any { oneSigner ->
                    oneSigner.asByteArray().wrap() == signer.wrap()
                }
            }

            else -> {
                throw PrintMessage("Authtype: ${descriptor.authType} is not supported in FTAuthenticator")
            }
        }
    }
}

private fun CoreCliktCommand.findValidAuthDescriptorIdForOperation(client: PostchainQuery, opName: String, accountId: ByteArray, signer: ByteArray, optionalAuthDescriptorId: String?): WrappedByteArray {
    val flags = client.getAuthFlags(opName)
    val authDescriptors = client.getAccountAuthDescriptorsBySigner(accountId, signer = signer)

    val authDescriptorsCandidates = findAuthDescriptors(authDescriptors, optionalAuthDescriptorId, signer)
    val authDescriptor = if (authDescriptorsCandidates.isEmpty()) {
        throw PrintMessage("No valid account descriptor found. User not authorized for operation $opName", statusCode = 1)
    } else if (authDescriptorsCandidates.size == 1) {
        authDescriptorsCandidates.first()
    } else if (terminal.terminalInfo.inputInteractive) {
        val candidateMap = authDescriptorsCandidates.associateBy { it.id.toHex() }
        (terminal.interactiveSelectList(
                entries = authDescriptorsCandidates.map {
                    """id: ${it.id}
                                |flags: ${it.getFlags()}
                                |signatures needed: ${it.getNumberOfSigners()} 
                                |keys: ${it.getKeysAsFormattedString()}
                                |""".trimMargin()
                },
                title = "Please select a valid auth descriptor"
        )
                ?: throw Abort()).let { candidateMap[it.substring(4, it.indexOf("\n"))]!! }
    } else {
        authDescriptorsCandidates.first()
    }

    if (!isValid(flags, authDescriptor)) {
        throw PrintMessage("No valid account descriptor found. Operation $opName requires the flag(s): $flags, while the flag(s) of the auth descriptor is: ${authDescriptor.getFlags()}", statusCode = 1)
    }
    return authDescriptor.id
}

private fun isValid(requiredFlags: List<String>, authDescriptor: Ft4GetAccountAuthDescriptorsBySignerResult): Boolean {
    val flags = authDescriptor.getFlags()
    return flags.containsAll(requiredFlags)
}

private fun CoreCliktCommand.findAccountId(client: PostchainQuery, signer: ByteArray): ByteArray {
    val accounts = client.getAccountsBySigner(signer, 100, null).getAccountIds()
    return accountPicker(accounts, signer)
}

private fun PagedResult.getAccountIds() = this.data.map { it.asDict()["id"]!!.asByteArray() }

private fun CoreCliktCommand.accountPicker(accounts: List<ByteArray>, signer: ByteArray): ByteArray {
    return accounts.let {
        if (it.isEmpty()) throw PrintMessage("No FT4 Account found for signer: ${signer.toHex()}", statusCode = 1)
        if (it.size == 1) it.first()
        else if (terminal.terminalInfo.inputInteractive) {
            terminal.interactiveSelectList(
                    entries = it.map { ac -> ac.toHex() }.toSet(),
                    title = "More than one account found, which one should we use?"
            )
                    ?.hexStringToByteArray()
                    ?: throw Abort()
        } else {
            throw PrintMessage("More than one account found, please specify which one to use with --ft-account-id option",
                    statusCode = 1)
        }
    }
}

fun CoreCliktCommand.addFtAuthOperation(transactionBuilder: TransactionBuilder, accountId: ByteArray, authDescriptorId: ByteArray) {
    transactionBuilder.ftAuthOperation(accountId, authDescriptorId)
}

fun CoreCliktCommand.addEvmAuthOperation(client: PostchainClient, transactionBuilder: TransactionBuilder,
                                         opName: String, opArgs: List<Gtv>, evmAddress: ByteArray,
                                         accountId: ByteArray, authDescriptorId: ByteArray,
                                         launchWebBrowser: Boolean = true, urlNotifier: (String) -> Unit = {}) {
    val encodedSignature = fetchEvmSignature(
            client, opName, opArgs, evmAddress,
            accountId, authDescriptorId,
            launchWebBrowser, urlNotifier)
    transactionBuilder.evmAuthOperation(accountId, authDescriptorId, listOf(encodedSignature))
}

fun CoreCliktCommand.fetchEvmSignature(client: PostchainClient,
                                       opName: String, opArgs: List<Gtv>, evmAddress: ByteArray,
                                       accountId: ByteArray, authDescriptorId: ByteArray,
                                       launchWebBrowser: Boolean = true, urlNotifier: (String) -> Unit = {}): Signature {
    val authMessageTemplate = client.getAuthMessageTemplate(opName, gtv(opArgs))
    val counter = client.getAuthDescriptorCounter(accountId, authDescriptorId)
    if (counter == null) throw CliktError("Invalid auth descriptor counter. Was the auth descriptor too close to expiration?")
    // TODO [use-new-algo] use new hash version here
    val nonce = gtv(listOf(
            gtv(client.config.blockchainRid),
            gtv(opName),
            gtv(opArgs),
            gtv(counter),
    )).merkleHash(GtvMerkleHashCalculatorV1(::sha256Digest))
    val authMessage = authMessageTemplate
            .replace("{blockchain_rid}", client.config.blockchainRid.toHex().uppercase())
            .replace("{nonce}", nonce.toHex().uppercase())
            .replace("{account_id}", accountId.toHex().uppercase())
            .replace("{auth_descriptor_id}", authDescriptorId.toHex().uppercase())

    val html = this::class.java.getResource("/com/chromia/cli/evm_auth/index.html")!!.readText()
            .replace("{{address}}", "0x${evmAddress.toHex()}")
            .replace("{{message}}", StringEscapeUtils.escapeEcmaScript(authMessage))
    val signatureFuture = CompletableFuture<String>()
    val server = routes(
            "/" bind GET to { Response(OK).header("Content-Type", "text/html").body(html) },
            "/signature" bind POST to { request ->
                signatureFuture.complete(request.bodyString())
                Response(OK)
            },
            "/error" bind POST to { request ->
                signatureFuture.completeExceptionally(CliktError(request.bodyString()))
                Response(OK)
            },
            webJars()
    ).asServer(Netty(port = 0)).start()
    val url = "http://localhost:${server.port()}"
    if (launchWebBrowser) {
        openWebLink(url)
    }
    urlNotifier(url)
    val rawSignature = try {
        signatureFuture.get()
    } catch (e: ExecutionException) {
        throw (e.cause ?: e)
    } finally {
        server.stop()
    }
    val signature = Gson().fromJson(rawSignature, JsonObject::class.java)
    return Signature(
            r = signature.get("r").asString.drop(2).hexStringToByteArray().wrap(),
            s = signature.get("s").asString.drop(2).hexStringToByteArray().wrap(),
            v = signature.get("v").asLong
    )
}

fun CoreCliktCommand.openWebLink(url: String) {
    val os = System.getProperty("os.name")
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else if (os.contains("mac")) {
            Runtime.getRuntime().exec(arrayOf("open", url))
        } else if (os.contains("nix") || os.contains("nux")) {
            Runtime.getRuntime().exec(arrayOf("xdg-open", url))
        } else {
            terminalWebLink(url)
        }
    } catch (_: IOException) {
        terminalWebLink(url)
    }
}

fun CoreCliktCommand.terminalWebLink(url: String) {
    echo("Open ${hyperlink(url)(url)} in your web browser to continue")
}
