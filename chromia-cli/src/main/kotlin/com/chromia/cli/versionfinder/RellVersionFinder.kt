package com.chromia.cli.versionfinder

import net.postchain.client.request.Endpoint
import net.postchain.common.BlockchainRid
import net.postchain.rell.base.model.R_LangVersion

interface RellVersionFinder {
    fun getTargetVersion(endpoint: Endpoint, brid: BlockchainRid): R_LangVersion
}