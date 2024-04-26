package com.chromia.cli.versionfinder

import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.exception.ClientError
import net.postchain.client.request.Endpoint
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.rell.base.model.R_LangVersion
import org.http4k.core.Status

class PostchainRellVersionFinder(
        private val template: PostchainClientConfig,
        private val clientProvider: PostchainClientProvider
) : RellVersionFinder {

    override fun getTargetVersion(endpoint: Endpoint, brid: BlockchainRid): R_LangVersion {
        return try {
            clientProvider.createClient(template.copy(endpointPool = EndpointPool.singleUrl(endpoint.url)))
                    .query("rell.get_rell_version", gtv(mapOf()))
                    .let { R_LangVersion.of(it.asString()) }
        } catch (e: ClientError) {
            if (e.status == Status.NOT_FOUND) throw CanNotFindBlockchainException(brid, endpoint)
            throw e
        }
    }
}
