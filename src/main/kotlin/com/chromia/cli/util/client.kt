package com.chromia.cli.util

import com.chromia.directory1.common.Codename
import com.chromia.directory1.common.Version
import com.chromia.directory1.common.directoryVersion
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.d1.cluster.ClusterManagement
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.cookie.StandardCookieSpec
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.util.Timeout
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler


val PostchainClient.directory1Version get(): Version {
    return try {
        directoryVersion()
    } catch (e: Exception) {
        Version(Codename.Delta, "0.1.0")
    }
}

// TODO: use from postchain-client
fun defaultHttpHandler(config: PostchainClientConfig) = ApacheClient(HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setCookieSpec(StandardCookieSpec.IGNORE)
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(config.connectTimeout.toMillis()))
                .setResponseTimeout(Timeout.ofMilliseconds(config.responseTimeout.toMillis()))
                .build()).build())

fun interface HttpHandlerFactory {
    fun buildHttpHandler(config: PostchainClientConfig): HttpHandler
}

fun interface ClusterManagementFactory {
    fun buildClusterManagement(client: PostchainClient): ClusterManagement
}
