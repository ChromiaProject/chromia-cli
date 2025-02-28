package com.chromia.cli.util

import com.chromia.build.tools.config.ChromiaClientConfig
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import com.chromia.cli.command.KeyPairSource

val PostchainClient.pubkey get() = config.pubkey
val PostchainClientConfig.pubkey get() = signers.first().pubKey

fun ChromiaClientConfig.configureSigners(keyPairSource: KeyPairSource?) {
    when (keyPairSource) {
        is KeyPairSource.SecretFile -> setSignerFromSecret(keyPairSource.file.toPath())
        is KeyPairSource.KeyId -> setSignerUsingKeyId(keyPairSource.name)
        null -> {}
    }
}
