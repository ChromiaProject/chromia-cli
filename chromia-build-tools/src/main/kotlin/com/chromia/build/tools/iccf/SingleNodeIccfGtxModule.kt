package com.chromia.build.tools.iccf

import net.postchain.PostchainContext
import net.postchain.core.BlockchainConfiguration
import net.postchain.gtx.PostchainContextAware

/**
 * GTXModule as a simplified replacement where all chains are on the same node.
 *
 * NOT TO BE USED IN PRODUCTION!
 *
 * Production implemenetation found here:
 * https://gitlab.com/chromaway/postchain-chromia/-/blob/3.14.11/chromia-infrastructure/src/main/kotlin/net/postchain/d1/iccf/IccfGTXModule.kt
 */
class SingleNodeIccfGtxModule : AbstractTestIccfGtxModule<SingleNodeIccfGtxModule.Config>(
        Config(),
        ::SingleNodeIccfGtxOperation
), PostchainContextAware {

    override fun initializeContext(configuration: BlockchainConfiguration, postchainContext: PostchainContext) {
        conf.apply {
            context = postchainContext
        }
    }

    class Config {
        lateinit var context: PostchainContext
    }
}
