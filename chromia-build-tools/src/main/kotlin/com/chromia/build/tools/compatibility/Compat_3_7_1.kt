package com.chromia.build.tools.compatibility

import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid
import net.postchain.gtv.GtvFactory

object Compat_3_7_1 {

    fun TransactionBuilder.proposeBlockchainOperation(myPubkey: ByteArray,
                                                      configData: ByteArray,
                                                      bcName: String,
                                                      containerName: String) =
            addOperation("propose_blockchain", GtvFactory.gtv(myPubkey),
                    GtvFactory.gtv(configData),
                    GtvFactory.gtv(bcName),
                    GtvFactory.gtv(containerName))

    fun TransactionBuilder.proposeConfigurationAtOperation(myPubkey: ByteArray,
                                                           blockchainRid: BlockchainRid,
                                                           configData: ByteArray,
                                                           height: Long,
                                                           force: Boolean) =
            addOperation("propose_configuration_at", GtvFactory.gtv(myPubkey),
                    GtvFactory.gtv(blockchainRid),
                    GtvFactory.gtv(configData),
                    GtvFactory.gtv(height),
                    GtvFactory.gtv(force))

    fun TransactionBuilder.proposeConfigurationOperation(myPubkey: ByteArray,
                                                         blockchainRid: BlockchainRid,
                                                         configData: ByteArray) =
            addOperation("propose_configuration", GtvFactory.gtv(myPubkey),
                    GtvFactory.gtv(blockchainRid),
                    GtvFactory.gtv(configData))
}
