package com.chromia.cli.util

import com.chromia.directory1.version.apiVersion
import net.postchain.client.core.PostchainClient
import net.postchain.d1.cluster.ClusterManagement

val PostchainClient.apiVersion get(): Long {
    return try {
        apiVersion()
    } catch (e: Exception) {
        1
    }
}

fun interface ClusterManagementFactory {
    fun buildClusterManagement(client: PostchainClient): ClusterManagement
}
