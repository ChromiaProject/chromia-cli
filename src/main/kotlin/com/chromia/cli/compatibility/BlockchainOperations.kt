package com.chromia.cli.compatibility

import com.chromia.cli.compatibility.Compat_3_7_1.proposeBlockchainOperation
import com.chromia.cli.compatibility.Compat_3_7_1.proposeConfigurationAtOperation
import com.chromia.cli.compatibility.Compat_3_7_1.proposeConfigurationOperation
import com.chromia.directory1.common.Version
import com.chromia.directory1.common.proposal.proposeBlockchainOperation
import com.chromia.directory1.common.proposal.proposeConfigurationAtOperation
import com.chromia.directory1.common.proposal.proposeConfigurationOperation
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid

class BlockchainOperations(private val version: Version, private val builder: TransactionBuilder) {

    fun newBlockchainOperation(myPubkey: ByteArray,
                               configData: ByteArray,
                               bcName: String,
                               containerName: String) {
        when (version.semver) {
            "0.1.0" -> builder.proposeBlockchainOperation(myPubkey, configData, bcName, containerName)
            else -> builder.proposeBlockchainOperation(myPubkey, configData, bcName, containerName, "")
        }
    }

    fun proposeConfiguration(myPubkey: ByteArray,
                             blockchainRid: BlockchainRid,
                             configData: ByteArray,
                             height: Long?,
                             force: Boolean) {
        if (height == null) {
            when (version.semver) {
                "0.1.0" -> builder.proposeConfigurationOperation(myPubkey, blockchainRid, configData)
                else -> builder.proposeConfigurationOperation(myPubkey, blockchainRid, configData, "")
            }
        } else {
            when (version.semver) {
                "0.1.0" -> builder.proposeConfigurationAtOperation(myPubkey, blockchainRid, configData, height, force)
                else -> builder.proposeConfigurationAtOperation(myPubkey, blockchainRid, configData, height, force, "")
            }
        }
    }
}
