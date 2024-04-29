package com.chromia.build.tools.config

import com.chromia.directory1.common.queries.getCompressedConfigurationParts
import net.postchain.client.core.PostchainClient
import net.postchain.common.wrap
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.GtvObjectMapper
import net.postchain.gtv.mapper.Name
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash

//Implementation taken from: https://gitlab.com/chromaway/core-tools/management-console/-/blob/6ff79ec5fd9bd8fcd59c0cbe25f589ed4ba45ae4/src/main/kotlin/net/postchain/mc/cli/util/BlockchainConfigurationCompressor.kt
object BlockchainConfigurationCompressor {
    private const val COMPRESSED_ROOTS_CONFIG_KEY = "compressed_roots"
    private const val GTX_CONFIG_KEY = "gtx"
    private const val RELL_CONFIG_KEY = "rell"
    private const val RELL_SOURCES_CONFIG_KEY = "sources"

    private val RELL_SOURCES_PATH = listOf(GTX_CONFIG_KEY, RELL_CONFIG_KEY, RELL_SOURCES_CONFIG_KEY)
    private val merkleHashCalculator = GtvMerkleHashCalculator(Secp256K1CryptoSystem())

    fun compress(client: PostchainClient, configuration: Gtv, apiVersion: Long): Gtv {
        if (apiVersion < 27) return configuration

        val rellSources = configuration[GTX_CONFIG_KEY]?.get(RELL_CONFIG_KEY)?.get(RELL_SOURCES_CONFIG_KEY)?.asDict()
                ?: return configuration
        val compressedConfig = configuration.asDict().toMutableMap()

        val rellSourceHashes = rellSources.mapValues { it.value.merkleHash(merkleHashCalculator).wrap() }
        val uniqueHashes = rellSourceHashes.values.toSet().map { it.data }
        val alreadyCompressed = client.getCompressedConfigurationParts(uniqueHashes.toSet()).map { it.wrap() }

        val compressedRellSources = mutableMapOf<String, Gtv>()
        val compressedKeys = mutableListOf<CompressedKey>()
        for ((fileName, hash) in rellSourceHashes) {
            if (alreadyCompressed.contains(hash)) {
                compressedKeys.add(CompressedKey(fileName, hash.data))
            } else {
                compressedRellSources[fileName] = rellSources[fileName]!!
            }
        }

        val compressedGtx = compressedConfig[GTX_CONFIG_KEY]!!.asDict().toMutableMap()
        val compressedRell = compressedGtx[RELL_CONFIG_KEY]!!.asDict().toMutableMap()
        compressedRell[RELL_SOURCES_CONFIG_KEY] = gtv(compressedRellSources)
        compressedGtx[RELL_CONFIG_KEY] = gtv(compressedRell)
        compressedConfig[GTX_CONFIG_KEY] = gtv(compressedGtx)

        val compressionConfig = CompressedRoot(
                RELL_SOURCES_PATH,
                compressedKeys
        )
        compressedConfig[COMPRESSED_ROOTS_CONFIG_KEY] = gtv(listOf(GtvObjectMapper.toGtvDictionary(compressionConfig)))

        return gtv(compressedConfig)
    }
}

data class CompressedRoot(
        @Name("path")
        val path: List<String>,

        @Name("compressed_keys")
        val compressedKeys: List<CompressedKey>
)

data class CompressedKey(
        @Name("content_key")
        val contentKey: String,

        @Name("content_hash")
        val contentHash: ByteArray
)
