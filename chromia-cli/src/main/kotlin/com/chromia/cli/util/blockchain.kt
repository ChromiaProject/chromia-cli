package com.chromia.cli.util

import com.chromia.api.result.BlockchainConfiguration
import com.chromia.cli.command.node.AbstractNodeCommand
import com.chromia.cli.tools.env.CliktCliEnv
import net.postchain.gtv.builder.GtvBuilder
import net.postchain.rell.api.base.RellCliEnv


private val whiteListedGtxModules = listOf(
        "net.postchain.d1.anchoring.system.SystemAnchoringGTXModule",
        "net.postchain.d1.anchoring.cluster.ClusterAnchoringGTXModule",
        "net.postchain.d1.icmf.IcmfSenderGTXModule",
        "net.postchain.d1.icmf.IcmfReceiverGTXModule",
        "net.postchain.d1.iccf.IccfGTXModule",
)

fun BlockchainConfiguration.filterGtxModules(cliEnv: RellCliEnv): BlockchainConfiguration {
    val configModules = config["gtx"]?.get("modules")!!.asArray() // Not null since default values are added
    val intersect = configModules.intersect(whiteListedGtxModules)

    if (intersect.isNotEmpty()) {
        cliEnv.error("Warning filtering out modules from configuration;\n ${intersect.joinToString("\n")}")
    }

    val gtvBuilder = GtvBuilder()
    gtvBuilder.update(config)
    val gtxModules = configModules
            .filter { it.asString() !in whiteListedGtxModules }
            .map { GtvBuilder.GtvNode.decode(it) }
            .let { GtvBuilder.GtvArrayNode(it, GtvBuilder.GtvArrayMerge.REPLACE) }

    gtvBuilder.update(gtxModules, "gtx", "modules")
    return BlockchainConfiguration(name, gtvBuilder.build())
}
