package com.chromia.cli.compile.config

import net.postchain.common.BlockchainRid

data class NamedBlockchainRid(val name: String, val blockchainRid: BlockchainRid)
