package com.chromia.cli.util

import com.chromia.cli.tools.ft.initFtAuth
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.CoreCliktCommand
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainQuery

val PostchainClient.pubkey get() = config.pubkey
val PostchainClientConfig.pubkey get() = signers.first().pubKey

fun CoreCliktCommand.initFtAuthVerbose(
    client: PostchainQuery,
    blockchain: String? = null,
    brid: String? = null
) {
    try {
        initFtAuth(client)
    } catch (e: CliktError) {
        if (e.message?.contains("is not FT4 compatible") == true) {
            val errMsg = buildString {
                append("Dapp ")
                blockchain?.let { append("[$it] ") }
                brid?.let { append(" with RID '$it' ") }
                append("is not FT4 compatible")
            }
            throw CliktError(errMsg)
        } else {
            throw e
        }
    }
}
