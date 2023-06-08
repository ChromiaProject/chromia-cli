package com.chromia.cli.versionfinder

import net.postchain.client.request.Endpoint
import net.postchain.common.BlockchainRid

interface RellVersionFinderInterface {
    fun getTargetVersion(endpoint: Endpoint, brid: BlockchainRid): String
}