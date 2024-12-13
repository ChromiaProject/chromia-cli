package com.chromia.cli.ft

import com.chromia.directory1.lib.ft4.core.accounts.AuthType
import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.chromia.directory1.lib.ft4.external.accounts.getAccountAuthDescriptorsBySigner
import com.chromia.directory1.lib.ft4.external.accounts.getAccountsBySigner
import com.chromia.directory1.lib.ft4.external.auth.ftAuthOperation
import com.chromia.directory1.lib.ft4.external.auth.getAuthFlags
import com.chromia.directory1.lib.ft4.utils.PagedResult
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.mordant.input.interactiveSelectList
import com.github.ajalt.mordant.terminal.Terminal
import net.postchain.client.core.PostchainQuery
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.hexStringToByteArray
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.toHex
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap
import net.postchain.crypto.PubKey


class FTAuthenticator internal constructor(private val client: PostchainQuery, private val terminal: Terminal) {

    fun addAuthenticationOperation(transactionBuilder: TransactionBuilder, opName: String, pubKey: PubKey, optionalAccountId: String? = null, optionalAuthDescriptorId: String? = null) {
        val account = optionalAccountId?.hexStringToByteArray() ?: findAccountId(pubKey)
        val authDescriptorId = findValidAuthDescriptorIdForOperation(opName, account, pubKey, optionalAuthDescriptorId)
        transactionBuilder.ftAuthOperation(account, authDescriptorId.data)
    }

    private fun findAuthDescriptors(descriptors: List<Ft4GetAccountAuthDescriptorsBySignerResult>, optionalAuthDescriptorId: String?, pubKey: PubKey): List<Ft4GetAccountAuthDescriptorsBySignerResult> {
        return descriptors.filter { descriptor ->
            when {
                !optionalAuthDescriptorId.isNullOrEmpty() -> {
                    descriptor.id == optionalAuthDescriptorId.hexStringToWrappedByteArray()
                }

                descriptor.authType == AuthType.S -> {
                    descriptor.getSingleKey().wrap() == pubKey.wData
                }

                descriptor.authType == AuthType.M -> {
                    descriptor.getMultiKeys().any { signer ->
                        signer.asByteArray().wrap() == pubKey.wData
                    }
                }

                else -> {
                    throw PrintMessage("Authtype: ${descriptor.authType} is not supported in FTAuthenticator")
                }
            }
        }
    }

    private fun findValidAuthDescriptorIdForOperation(opName: String, accountId: ByteArray, pubKey: PubKey, optionalAuthDescriptorId: String?): WrappedByteArray {
        val flags = client.getAuthFlags(opName)
        val authDescriptors = client.getAccountAuthDescriptorsBySigner(accountId, signer = pubKey.data)

        val authDescriptorsCandidates = findAuthDescriptors(authDescriptors, optionalAuthDescriptorId, pubKey)
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

    private fun Ft4GetAccountAuthDescriptorsBySignerResult.getFlags() = this.args.asArray().first().asArray().map { it.asString() }


    private fun findAccountId(pubKey: PubKey): ByteArray {
        val accounts = client.getAccountsBySigner(pubKey.data, 100, null).getAccountIds()
        return accountPicker(accounts, pubKey)
    }

    private fun PagedResult.getAccountIds() = this.data.map { it.asDict()["id"]!!.asByteArray() }

    private fun accountPicker(accounts: List<ByteArray>, pubKey: PubKey): ByteArray {
        return accounts.let {
            if (it.isEmpty()) throw PrintMessage("No FT4 Account found for public key: $pubKey", statusCode = 1)
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
}
