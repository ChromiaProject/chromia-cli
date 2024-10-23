package com.chromia.cli.ft

import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.chromia.directory1.lib.ft4.external.accounts.getAccountAuthDescriptorsBySigner
import net.postchain.client.core.PostchainQuery
import net.postchain.crypto.PubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull

internal interface FTAuthQuery : PostchainQuery {

    val legacyFTAuth: Boolean
    fun findAuthDescriptorQuery(accountId: ByteArray, pubKey: PubKey): List<Ft4GetAccountAuthDescriptorsBySignerResult>
    fun findAuthDescriptorQueryLegacy(accountId: ByteArray, pubKey: PubKey): Gtv
    fun findAccountsQuery(pubKey: PubKey): List<ByteArray>
}

// Valid for version 0.1.1 -> 0.1.7
class FTAuth01Query(private val client: PostchainQuery) : FTAuthQuery, PostchainQuery by client {

    override val legacyFTAuth: Boolean
        get() = true

    override fun findAuthDescriptorQuery(accountId: ByteArray, pubKey: PubKey): List<Ft4GetAccountAuthDescriptorsBySignerResult> {
        TODO("Not yet implemented")
    }

    override fun findAuthDescriptorQueryLegacy(accountId: ByteArray, pubKey: PubKey) =
            client.query(
                    "ft4.get_account_auth_descriptors_by_participant_id",
                    gtv(mapOf("account_id" to gtv(accountId), "participant_id" to gtv(pubKey.data)))
            )

    override fun findAccountsQuery(pubKey: PubKey) =
            client.query(
                    "ft4.get_accounts_by_participant_id",
                    gtv(mapOf("id" to gtv(pubKey.data)))
            ).asArray().map { it.asByteArray() }.toList()
}

// Valid for versions 0.2.0 -> 0.3.1
open class FTAuth02Query(private val client: PostchainQuery) : FTAuthQuery, PostchainQuery by client {

    override val legacyFTAuth: Boolean
        get() = true

    override fun findAuthDescriptorQuery(accountId: ByteArray, pubKey: PubKey): List<Ft4GetAccountAuthDescriptorsBySignerResult> {
        TODO("Not yet implemented")
    }

    override fun findAuthDescriptorQueryLegacy(accountId: ByteArray, pubKey: PubKey) =
            client.query(
                    "ft4.get_account_auth_descriptors_by_signer",
                    gtv(mapOf("account_id" to gtv(accountId), "signer" to gtv(pubKey.data), "page_size" to gtv(100), "page_cursor" to GtvNull))
            ).asDict()["data"]!!

    override fun findAccountsQuery(pubKey: PubKey) =
            client.query(
                    "ft4.get_accounts_by_signer",
                    gtv(mapOf("id" to gtv(pubKey.data), "page_size" to gtv(100), "page_cursor" to GtvNull))
            ).asDict()["data"]!!.asArray().map { it.asDict()["id"]!!.asByteArray() }
}


// Valid for versions 0.4.0 ->
class FTAuth04Query(private val client: PostchainQuery) : FTAuth02Query(client) {
    override val legacyFTAuth: Boolean
        get() = false

    override fun findAuthDescriptorQuery(accountId: ByteArray, pubKey: PubKey): List<Ft4GetAccountAuthDescriptorsBySignerResult> =

            client.getAccountAuthDescriptorsBySigner(accountId, signer = pubKey.data)

}
