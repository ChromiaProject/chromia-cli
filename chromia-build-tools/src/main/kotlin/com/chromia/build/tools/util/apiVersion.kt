package com.chromia.build.tools.util

import com.chromia.directory1.version.apiVersion
import net.postchain.client.core.PostchainQuery

val PostchainQuery.apiVersion get(): Long {
    return try {
        apiVersion()
    } catch (e: Exception) {
        1
    }
}