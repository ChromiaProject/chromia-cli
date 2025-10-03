package com.chromia.cli.util

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.util.LibraryChainNetworkUtils.CHROMIA_MAINNET
import com.chromia.cli.util.LibraryChainNetworkUtils.libraryPredefinedNetworks
import com.chromia.library.chain.versioning.external.getLibrary
import com.chromia.library.chain.versioning.external.getLibraryRid
import com.github.ajalt.clikt.core.PrintMessage
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.types.WrappedByteArray

object LibraryRidResolver {

    fun resolveLibraryRids(model: ChromiaModel): ChromiaModel {
        val libs = model.libs.map {
            if (isLibraryChainLib(it.value)) {
                val client = createConfiguredClient(it.value.registry)
                val rid = client.getLibraryRid(it.key, it.value.version!!)
                    ?: throw PrintMessage("Unable to get rid for ${it.key}")
                val name = client.getLibrary(it.key)?.displayName
                    ?: throw PrintMessage("Library ${it.key} not found")
                name to it.value.copy(rid = WrappedByteArray(rid))
            } else {
                it.key to it.value
            }
        }
        return model.copy(libs = libs.toMap())
    }

    private fun isLibraryChainLib(libModel: RellLibraryModel) = libModel.version != null

    private fun createConfiguredClient(url: String? = null, brid: BlockchainRid? = null): PostchainClient {
        val networkConfig = url?.let { libraryPredefinedNetworks[it]?.invoke() }
        val targetUrl = networkConfig?.url ?: url ?: CHROMIA_MAINNET
        val targetBrid = brid ?: libraryPredefinedNetworks[targetUrl]?.invoke()?.brid
            ?: throw PrintMessage("Brid of library_chain is required")

        val postchainConfig = PostchainClientConfig.defaultConfig
            .copy(
                endpointPool = EndpointPool.singleUrl(targetUrl),
                blockchainRid = targetBrid
            )
        return PostchainClientProviderImpl().createClient(postchainConfig)
    }
}
