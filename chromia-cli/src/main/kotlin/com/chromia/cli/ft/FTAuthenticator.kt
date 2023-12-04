package com.chromia.cli.ft

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.mordant.terminal.Terminal
import net.postchain.client.core.PostchainQuery
import net.postchain.client.exception.ClientError
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap
import net.postchain.crypto.PubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.Name
import net.postchain.gtv.mapper.Nullable
import net.postchain.gtv.mapper.toList

class FTAuthenticator(private val client: PostchainQuery, private val terminal: Terminal) {

    fun addAuthenticationOperation(transactionBuilder: TransactionBuilder, opName: String, pubKey: PubKey, accountIdOverride: String?) {
        validateFt4Version()

        val accountId = findAccountId(pubKey, accountIdOverride)
        val authDescriptor = findAuthDescriptor(accountId, pubKey, opName)
        val operationAuthFlags = getOperationAuthFlags(opName)
        authDescriptor.requireAuthFlags(operationAuthFlags, opName)

        transactionBuilder.addOperation("ft4.ft_auth", accountId, gtv(authDescriptor.id))
    }

    private fun getOperationAuthFlags(opName: String): List<String> {
        try {
            return client.query("ft4.get_auth_flags", gtv(mapOf("op_name" to gtv(opName)))).asArray().map { it.asString() }
        } catch (e: ClientError) {
            throw PrintMessage("Failed to get auth-handler flags: ${e.errorMessage}", statusCode = 1)
        }
    }


    private fun validateFt4Version() {
        val blackListedVersions = listOf("0.1.0r")
        val version = try {
            client.query("ft4.get_version", gtv(mapOf())).asString()
        } catch (e: ClientError) {
            throw PrintMessage("Dapp is not FT compatible: ${e.errorMessage}", statusCode = 1)
        }
        if (version in blackListedVersions) throw PrintMessage("FT version $version not supported", statusCode = 1)
    }

    private fun findAuthDescriptor(accountId: GtvByteArray, pubKey: PubKey, opName: String): AuthDescriptor {
        val authDescriptors = client.query(
                "ft4.get_account_auth_descriptors_by_participant_id",
                gtv(mapOf("account_id" to accountId, "participant_id" to gtv(pubKey.data)))
        )
        return authDescriptors.toList<AuthDescriptor>().find { it.args[1].asByteArray().wrap() == pubKey.wData }
                ?: throw PrintMessage("No valid account descriptor found. User not authorized for operation $opName", statusCode = 1)
    }

    private fun findAccountId(pubKey: PubKey, accountId: String?): GtvByteArray {
        if (!accountId.isNullOrBlank()) return gtv(accountId.hexStringToByteArray())
        val accountIds = client.query("ft4.get_accounts_by_participant_id", gtv(mapOf("id" to gtv(pubKey.data)))).asArray()
        if (accountIds.isEmpty()) throw PrintMessage("No accounts found for pubkey: $pubKey", statusCode = 1)
        return if (accountIds.size > 1) {
            terminal.prompt("More than one account found, which one should we use: ", choices = accountIds.map { it.asByteArray().toHex() })
                    ?.let { gtv(it.hexStringToByteArray()) }
                    ?: throw Abort()
        } else accountIds.first() as GtvByteArray
    }

    data class AuthDescriptor(
            @Name("id") val id: WrappedByteArray,
            @Name("args") val args: Gtv,
            @Name("created") val created: Long,
            @Name("auth_type") val authType: String,
            @Name("rules") @Nullable val rules: Gtv?
    ) {
        private val flags by lazy { args.asArray().first().asArray().map { it.asString() } }

        fun requireAuthFlags(operationAuthFlags: List<String>, opName: String) {
            if (!flags.containsAll(operationAuthFlags)) {
                throw PrintMessage("No valid account descriptor found. Operation $opName requires the flag(s): $operationAuthFlags, while the flag(s) of the auth descriptor is: $flags", statusCode = 1)
            }
        }
    }
}
