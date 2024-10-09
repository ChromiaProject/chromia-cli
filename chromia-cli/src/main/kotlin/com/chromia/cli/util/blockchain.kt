package com.chromia.cli.util

import com.chromia.api.impl.compile.standardGtxModules
import com.chromia.api.result.BlockchainConfiguration
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.builder.GtvBuilder
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.rell.api.base.RellCliEnv
import java.io.File


private val knownGtxModules = listOf(
        "net.postchain.d1.anchoring.system.SystemAnchoringGTXModule",
        "net.postchain.d1.anchoring.cluster.ClusterAnchoringGTXModule",
        "net.postchain.d1.icmf.IcmfSenderGTXModule",
        "net.postchain.d1.icmf.IcmfReceiverGTXModule",
        "net.postchain.d1.iccf.IccfGTXModule",
)

fun BlockchainConfiguration.removeKnownGtxModules(cliEnv: RellCliEnv): BlockchainConfiguration {
    val configModules = config["gtx"]?.get("modules")!!.asArray() // Not null since default values are added
    val intersect = configModules.intersect(knownGtxModules)

    if (intersect.isNotEmpty()) {
        cliEnv.error("Warning filtering out modules from configuration;\n ${intersect.joinToString("\n")}")
    }

    val gtvBuilder = GtvBuilder()
    gtvBuilder.update(config)
    val gtxModules = configModules
            .filter { it.asString() !in knownGtxModules }
            .map { GtvBuilder.GtvNode.decode(it) }
            .let { GtvBuilder.GtvArrayNode(it, GtvBuilder.GtvArrayMerge.REPLACE) }

    gtvBuilder.update(gtxModules, "gtx", "modules")
    return BlockchainConfiguration(name, gtvBuilder.build())
}

fun BlockchainConfiguration.keepOnlyStandardGtxModules(): BlockchainConfiguration {
    val gtvBuilder = GtvBuilder()
    gtvBuilder.update(config)
    val gtxModules = standardGtxModules
            .map { GtvBuilder.GtvNode.decode(GtvFactory.gtv(it)) }
            .let { GtvBuilder.GtvArrayNode(it, GtvBuilder.GtvArrayMerge.REPLACE) }

    gtvBuilder.update(gtxModules, "gtx", "modules")
    return BlockchainConfiguration(name, gtvBuilder.build())
}

fun readBlockchainConfigFile(file: File): Gtv = if (file.extension == "gtv") {
    file.inputStream().use { inputStream ->
        GtvDecoder.decodeGtv(inputStream)
    }
} else {
    GtvMLParser.parseGtvML(file.readText())
}
