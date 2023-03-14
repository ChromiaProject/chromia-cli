package com.chromia.cli.util

import com.chromia.directory1.common.Codename
import com.chromia.directory1.common.Version
import com.chromia.directory1.common.directoryVersion
import net.postchain.client.core.PostchainClient




val PostchainClient.directory1Version get(): Version {
    return try {
        directoryVersion()
    } catch (e: Exception) {
        Version(Codename.Delta, "0.1.0")
    }
}
