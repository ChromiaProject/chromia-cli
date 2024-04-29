package com.chromia.api.result

import net.postchain.client.core.TxRid
import net.postchain.common.BlockchainRid
import java.nio.file.Path

data class BlockchainDeploymentResult(
        val blockchain: BlockchainConfiguration,
        val txRid: TxRid,
        val blockchainRid: BlockchainRid? = null,
        val success: Boolean = false,
)

fun List<BlockchainDeploymentResult>.isSuccess() = all { it.success }
fun List<BlockchainDeploymentResult>.save(target: Path, prefix: String? = null, suffix: String? = null) = forEach { res ->
    res.blockchain.save(target, buildString {
        prefix?.let { append("${it}_") }
        append(res.blockchain.name)
        suffix?.let { append("_$it") }
    })
}
