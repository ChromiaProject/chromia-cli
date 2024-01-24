package com.chromia.cli.ft

import net.postchain.client.core.PostchainQuery
import net.postchain.crypto.PubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvNull

internal interface FTAuthQuery : PostchainQuery {
    fun findAuthDescriptorQuery(accountId: GtvByteArray, pubKey: PubKey): Gtv
    fun findAccountsQuery(pubKey: PubKey): List<ByteArray>
}

// Valid for version 0.1.1 -> 0.1.7
class FTAuth01Query(private val client: PostchainQuery) : FTAuthQuery, PostchainQuery by client {
    override fun findAuthDescriptorQuery(accountId: GtvByteArray, pubKey: PubKey) =
            client.query(
                    "ft4.get_account_auth_descriptors_by_participant_id",
                    GtvFactory.gtv(mapOf("account_id" to accountId, "participant_id" to GtvFactory.gtv(pubKey.data)))
            )

    override fun findAccountsQuery(pubKey: PubKey) =
            client.query(
                    "ft4.get_accounts_by_participant_id",
                    GtvFactory.gtv(mapOf("id" to GtvFactory.gtv(pubKey.data)))
            ).asArray().map { it.asByteArray() }.toList()
}

// Valid for versions 0.2.0 ->
class FTAuth02Query(private val client: PostchainQuery) : FTAuthQuery, PostchainQuery by client {
    override fun findAuthDescriptorQuery(accountId: GtvByteArray, pubKey: PubKey) =
            client.query(
                    "ft4.get_account_auth_descriptors_by_signer",
                    GtvFactory.gtv(mapOf("account_id" to accountId, "signer" to GtvFactory.gtv(pubKey.data), "page_size" to GtvFactory.gtv(100), "page_cursor" to GtvNull))
            ).asDict()["data"]!!

    override fun findAccountsQuery(pubKey: PubKey) =
            client.query(
                    "ft4.get_accounts_by_signer",
                    GtvFactory.gtv(mapOf("id" to GtvFactory.gtv(pubKey.data), "page_size" to GtvFactory.gtv(100), "page_cursor" to GtvNull))
            ).asDict()["data"]!!.asArray().map { it.asDict()["id"]!!.asByteArray() }
}
