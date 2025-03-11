package com.chromia.cli.util

import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient

val PostchainClient.pubkey get() = config.pubkey
val PostchainClientConfig.pubkey get() = signers.first().pubKey
