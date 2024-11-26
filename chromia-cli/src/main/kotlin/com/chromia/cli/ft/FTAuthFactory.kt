package com.chromia.cli.ft

import com.chromia.directory1.lib.ft4.version.getVersion
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.mordant.terminal.Terminal
import net.postchain.client.core.PostchainQuery
import net.postchain.client.exception.ClientError

fun createFTAuthenticator(client: PostchainQuery, terminal: Terminal): FTAuthenticator {
    val version = try {
        client.getVersion()
    } catch (e: ClientError) {
        throw PrintMessage("Dapp is not FT4 compatible: ${e.errorMessage}", statusCode = 1)
    }
    // 0.0.* -> 0.3.*
    if (version.matches(Regex("^0\\.[0-3]\\.(0|[1-9]\\d*).*"))) {
        throw PrintMessage("Versions before release 0.4.0 are not supported, current FT4 version $version is to old", statusCode = 1)
    }
    return FTAuthenticator(client, terminal)
}
