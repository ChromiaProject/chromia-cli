package com.chromia.cli.ft

import com.chromia.directory1.lib.ft4.core.accounts.AuthType
import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.prompt
import net.postchain.client.core.PostchainQuery
import net.postchain.client.exception.ClientError
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.hexStringToByteArray
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.toHex
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap
import net.postchain.crypto.PubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.Name
import net.postchain.gtv.mapper.Nullable
import net.postchain.gtv.mapper.toList


interface FTAuth {
    fun addAuthenticationOperation(transactionBuilder: TransactionBuilder, opName: String, pubKey: PubKey, optionalAccountId: String? = null, optionalAuthDescriptorId: String? = null)

    companion object : FTAuthFactory {
        override fun createFTAuthenticator(client: PostchainQuery, terminal: Terminal): FTAuth {
            val version = try {
                client.query("ft4.get_version", gtv(mapOf())).asString()
            } catch (e: ClientError) {
                throw PrintMessage("Dapp is not FT compatible: ${e.errorMessage}", statusCode = 1)
            }
            return when {
                version == "0.1.0r" -> throw PrintMessage("FT version $version not supported", statusCode = 1)
                // 0.1.*
                version.matches(Regex("^0\\.1\\.(0|[1-9]\\d*).*")) -> FTAuthenticator(FTAuth01Query(client), terminal)
                // 0.2.* -> 0.3.*
                version.matches(Regex("^0\\.[2-3]\\.(0|[1-9]\\d*).*")) -> FTAuthenticator(FTAuth02Query(client), terminal)
                // > 0.4.0
                else -> FTAuthenticator(FTAuth04Query(client), terminal)
            }
        }
    }
}

interface FTAuthFactory {
    fun createFTAuthenticator(client: PostchainQuery, terminal: Terminal): FTAuth
}

class FTAuthenticator internal constructor(private val client: FTAuthQuery, private val terminal: Terminal) : FTAuth {

    override fun addAuthenticationOperation(transactionBuilder: TransactionBuilder, opName: String, pubKey: PubKey, optionalAccountId: String?, optionalAuthDescriptorId: String?) {
        val account = optionalAccountId?.hexStringToByteArray() ?: findAccountId(pubKey)
        val authDescriptorId = findValidAuthDescriptorIdForOperation(opName, account, pubKey, optionalAuthDescriptorId)

        transactionBuilder.addOperation("ft4.ft_auth", gtv(account), gtv(authDescriptorId))
    }

    private fun findValidAuthDescriptorIdForOperation(opName: String, accountId: ByteArray, pubKey: PubKey, optionalAuthDescriptorId: String?): WrappedByteArray {
        val flags = getOperationAuthFlags(opName)

        return if (client.legacyFTAuth) {
            val authDescriptor = client.findAuthDescriptorQueryLegacy(accountId, pubKey)
                    .toList<AuthDescriptor>()
                    .find { it.args[1].asByteArray().wrap() == pubKey.wData }
                    ?: throw PrintMessage("No valid account descriptor found. User not authorized for operation $opName", statusCode = 1)
            if (!authDescriptor.isValid(flags)) {
                throw PrintMessage("No valid account descriptor found. Operation $opName requires the flag(s): $flags, while the flag(s) of the auth descriptor is: ${authDescriptor.flags}", statusCode = 1)
            }
            authDescriptor.id
        } else {
            val authDescriptor = client.findAuthDescriptorQuery(accountId, pubKey).find { descriptor ->
                when {
                    !optionalAuthDescriptorId.isNullOrEmpty() -> {
                        descriptor.id == optionalAuthDescriptorId.hexStringToWrappedByteArray()
                    }

                    descriptor.authType == AuthType.S -> {
                        descriptor.args[1].asByteArray().wrap() == pubKey.wData
                    }

                    descriptor.authType == AuthType.M -> {
                        descriptor.args[2].asArray().any { signer ->
                            signer.asByteArray().wrap() == pubKey.wData
                        }
                    }

                    else -> {
                        throw PrintMessage("Authtype: ${descriptor.authType} is not supported in FTAuthenticator")
                    }
                }
            }
                    ?: throw PrintMessage("No valid account descriptor found. User not authorized for operation $opName", statusCode = 1)
            if (!isValid(flags, authDescriptor)) {
                throw PrintMessage("No valid account descriptor found. Operation $opName requires the flag(s): $flags, while the flag(s) of the auth descriptor is: ${authDescriptor.getFlags()}", statusCode = 1)
            }
            authDescriptor.id
        }
    }

    private fun isValid(requiredFlags: List<String>, authDescriptor: Ft4GetAccountAuthDescriptorsBySignerResult): Boolean {
        val flags = authDescriptor.getFlags()
        return flags.containsAll(requiredFlags)
    }

    private fun Ft4GetAccountAuthDescriptorsBySignerResult.getFlags() = this.args.asArray().first().asArray().map { it.asString() }

    private fun findAccountId(pubKey: PubKey): ByteArray = client.findAccountsQuery(pubKey).let {
        if (it.isEmpty()) throw PrintMessage("No FT4 Account found for public key: $pubKey", statusCode = 1)
        if (it.size == 1) it.first()
        else if (terminal.terminalInfo.inputInteractive) {
            terminal.prompt("More than one account found, which one should we use: ", choices = it.map { ac -> ac.toHex() })
                    ?.hexStringToByteArray()
                    ?: throw Abort()
        } else {
            throw PrintMessage("More than one account found, please specify which one to use with --ft-account-id option",
                    statusCode = 1)
        }
    }

    private fun getOperationAuthFlags(opName: String): List<String> {
        try {
            return client.query("ft4.get_auth_flags", gtv(mapOf("op_name" to gtv(opName)))).asArray().map { it.asString() }
        } catch (e: ClientError) {
            throw PrintMessage("Failed to get auth-handler flags: ${e.errorMessage}", statusCode = 1)
        }
    }

    data class AuthDescriptor(
            @Name("id") val id: WrappedByteArray,
            @Name("args") val args: Gtv,
            @Name("created") val created: Long,
            @Name("auth_type") val authType: String,
            @Name("rules") @Nullable val rules: Gtv?,
    ) {
        val flags by lazy { args.asArray().first().asArray().map { it.asString() } }

        fun isValid(requiredFlags: List<String>) = flags.containsAll(requiredFlags)
    }
}
