package com.chromia.cli.ft

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.mordant.terminal.Terminal
import net.postchain.client.core.PostchainQuery
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.common.types.WrappedByteArray
import net.postchain.crypto.PubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.mapper.Name
import net.postchain.gtv.mapper.Nullable
import net.postchain.gtv.mapper.toList

class FTAuthenticator(private val client: PostchainQuery, private val terminal: Terminal) {

    private val blackListedVersions = listOf("0.1.0r")
    fun addAuthenticationOperation(transactionBuilder: TransactionBuilder, opName: String, pubKey: PubKey) {
        val version = client.query("ft4.get_version", GtvFactory.gtv(mapOf())).asString()
        if (version in blackListedVersions) throw PrintMessage("FT version $version not supported", statusCode = 1)
        val accountIds = client.query("ft4.get_accounts_by_participant_id", GtvFactory.gtv(mapOf("id" to GtvFactory.gtv(pubKey.data)))).asArray()
        if (accountIds.isEmpty()) throw PrintMessage("No ft accounts found for pubkey $pubKey", statusCode = 1)
        val accountId = if (accountIds.size > 1) {
            terminal.prompt("More than one account found, which one should we use: ${accountIds.map { it.asByteArray().toHex() }}", choices = accountIds.map { it.asByteArray().toHex() })
                    ?.let { GtvFactory.gtv(it.hexStringToByteArray()) }
                    ?: throw Abort()
        } else accountIds.first()
        val authDescriptors = client.query(
                "ft4.get_account_auth_descriptors_by_participant_id",
                GtvFactory.gtv(mapOf("account_id" to accountId, "participant_id" to GtvFactory.gtv(pubKey.data)))
        )
                .toList<AuthDescriptor>()
        val flags = client.query("ft4.get_auth_flags", GtvFactory.gtv(mapOf("op_name" to GtvFactory.gtv(opName)))).asArray().map { it.asString() }
        val matchingDescriptor = authDescriptors.firstOrNull { flags.any { flag -> it.containsFlag(flag) } }
                ?: throw PrintMessage("No valid account descriptor found. User not authorized for operation $opName", statusCode = 1)
        transactionBuilder.addOperation("ft4.ft_auth", accountIds.first(), GtvFactory.gtv(matchingDescriptor.id))
    }

    data class AuthDescriptor(
            @Name("id") val id: WrappedByteArray,
            @Name("args") val args: Gtv,
            @Name("created") val created: Long,
            @Name("auth_type") val authType: String, // Enum
            @Name("rules") @Nullable val rules: Gtv?
    ) {
        val flags by lazy { args.asArray().first().asArray().map { it.asString() } }
        fun containsFlag(flag: String) = flags.contains(flag)
    }
}
