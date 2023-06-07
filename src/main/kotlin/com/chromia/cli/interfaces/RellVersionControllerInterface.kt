package com.chromia.cli.interfaces

import net.postchain.client.request.Endpoint
import net.postchain.common.BlockchainRid

interface RellVersionControllerInterface {
    fun getTargetVersion(endpoint: Endpoint, brid: BlockchainRid): String
}