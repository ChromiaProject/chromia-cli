package com.chromia.cli.versionfinder

import net.postchain.client.request.Endpoint
import net.postchain.common.BlockchainRid

class CanNotFindBlockchainException(brid: BlockchainRid, endpoint: Endpoint) : RuntimeException(
        "Can not find blockchain with blockchainRID: $brid on node ${endpoint.url}"
)

