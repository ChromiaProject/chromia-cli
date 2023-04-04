package com.chromia.cli.util

import com.chromia.directory1.version.apiVersion
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainQuery
import net.postchain.d1.cluster.ClusterManagement

val PostchainQuery.apiVersion get(): Long {
    return try {
        apiVersion()
    } catch (e: Exception) {
        1
    }
}

val PostchainClient.pubkey get() = config.pubkey
val PostchainClientConfig.pubkey get() = signers.first().pubKey

fun interface ClusterManagementFactory {
    fun buildClusterManagement(client: PostchainQuery): ClusterManagement
}
