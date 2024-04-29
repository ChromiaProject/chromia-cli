package com.chromia.api.impl.deployment

import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.BlockchainDeploymentResult
import com.chromia.build.tools.config.BlockchainConfigurationCompressor
import com.chromia.directory1.proposal_blockchain.findBlockchainRid
import net.postchain.base.gtv.GtvToBlockchainRidFactory
import net.postchain.client.core.PostchainClient
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.crypto.sha256Digest
import net.postchain.rell.api.base.RellCliEnv

fun interface DeploymentOperation {
    operator fun invoke(transactionBuilder: TransactionBuilder)
}

fun postTransaction(cliEnv: RellCliEnv, client: PostchainClient, configuration: BlockchainConfiguration, operation: DeploymentOperation): BlockchainDeploymentResult {
    val result = client
            .transactionBuilder()
            .addNop()
            .apply {
                operation(this)
            }
            .post()
    if (result.status == TransactionStatus.REJECTED) cliEnv.error("Deployment of blockchain ${configuration.name} failed: ${result.rejectReason ?: ""}")
    return BlockchainDeploymentResult(
            configuration,
            result.txRid,
            success = result.status != TransactionStatus.REJECTED
    )
}

fun awaitConfirmation(cliEnv: RellCliEnv, client: PostchainClient, partialResult: BlockchainDeploymentResult): BlockchainDeploymentResult {
    if (!partialResult.success) return partialResult
    val result = client.awaitConfirmation(partialResult.txRid, client.config.statusPollCount, client.config.statusPollInterval)
    when (result.status) {
        TransactionStatus.CONFIRMED -> {
            //chain.save(settings.targetDir, "${target}_${chain.name}_${Instant.now().toEpochMilli()}")
            return partialResult
        }

        TransactionStatus.REJECTED -> {
            cliEnv.error("Deployment of blockchain ${partialResult.blockchain.name} failed: ${result.rejectReason ?: ""}")
        }

        TransactionStatus.WAITING -> cliEnv.print("Deployment of blockchain ${partialResult.blockchain.name} still pending, tx-rid: ${partialResult.txRid.rid}")
        else -> throw RuntimeException("Cannot find status for this transaction")
    }
    return partialResult.copy(success = false)
}

fun findBlockchainRid(cliEnv: RellCliEnv, client: PostchainClient, apiVersion: Long, partialResult: BlockchainDeploymentResult): BlockchainDeploymentResult {
    if (!partialResult.success) return partialResult
    val maybeBcRid = if (apiVersion >= 8) {
        client.findBlockchainRid(partialResult.txRid.rid.hexStringToByteArray())?.let { BlockchainRid(it) }
    } else {
        GtvToBlockchainRidFactory.calculateBlockchainRid(partialResult.blockchain.config, ::sha256Digest)
    }
    if (maybeBcRid == null) {
        cliEnv.print("Deployment of blockchain ${partialResult.blockchain.name} was proposed, tx-rid: ${partialResult.txRid.rid}")
        return partialResult.copy(success = false)
    }
    cliEnv.print("Deployment of blockchain ${partialResult.blockchain.name} was successful")
    return partialResult.copy(blockchainRid = maybeBcRid)
}

fun compressConfiguration(client: PostchainClient, apiVersion: Long, partialResult: BlockchainConfiguration): BlockchainConfiguration {
    return partialResult.copy(
            config = BlockchainConfigurationCompressor.compress(client, partialResult.config, apiVersion)
    )
}

