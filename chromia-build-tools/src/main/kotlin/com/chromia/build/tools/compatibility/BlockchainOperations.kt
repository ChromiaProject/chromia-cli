package com.chromia.build.tools.compatibility

import com.chromia.build.tools.HeightFinder
import com.chromia.build.tools.compatibility.Compat_3_7_1.proposeBlockchainOperation
import com.chromia.build.tools.compatibility.Compat_3_7_1.proposeConfigurationAtOperation
import com.chromia.build.tools.compatibility.Compat_3_7_1.proposeConfigurationOperation
import com.chromia.directory1.proposal_blockchain.proposeBlockchainOperation
import com.chromia.directory1.proposal_blockchain.proposeConfigurationAtOperation
import com.chromia.directory1.proposal_blockchain.proposeConfigurationOperation
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid

class BlockchainOperations(private val apiVersion: Long, private val builder: TransactionBuilder, private val heightChecker: HeightFinder? = null) {

    fun newBlockchainOperation(myPubkey: ByteArray,
                               configData: ByteArray,
                               bcName: String,
                               containerName: String) {
        when {
            apiVersion < 2 -> builder.proposeBlockchainOperation(myPubkey, configData, bcName, containerName)
            else -> builder.proposeBlockchainOperation(myPubkey, configData, bcName, containerName, "")
        }
    }

    fun proposeConfiguration(myPubkey: ByteArray,
                             blockchainRid: BlockchainRid,
                             configData: ByteArray,
                             clusterName: String,
                             height: Long?,
                             force: Boolean) {
        if (height == null) {
            when {
                apiVersion < 2 -> builder.proposeConfigurationOperation(myPubkey, blockchainRid, configData)
                apiVersion < 5 && clusterName != "system" -> builder.proposeConfigurationAtOperation(myPubkey, blockchainRid, configData, heightChecker!!.findSafeHeight(blockchainRid), false, "")
                else -> builder.proposeConfigurationOperation(myPubkey, blockchainRid, configData, "")
            }
        } else {
            when {
                apiVersion < 2 -> builder.proposeConfigurationAtOperation(myPubkey, blockchainRid, configData, height, force)
                else -> builder.proposeConfigurationAtOperation(myPubkey, blockchainRid, configData, height, force, "")
            }
        }
    }
}
