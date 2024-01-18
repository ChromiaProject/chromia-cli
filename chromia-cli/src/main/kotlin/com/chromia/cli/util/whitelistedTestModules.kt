package com.chromia.cli.util

import net.postchain.rell.api.base.RellApiCompile


private val whitelistedTestGtxModuleMap: Map<String, String> = mapOf(
        "net.postchain.d1.iccf.IccfGTXModule" to DummyIccfGtxModule::class.qualifiedName!!
)
fun RellApiCompile.Config.Builder.addWhitelistedGTXModules(gtxModules: List<String>?) = apply {
    gtxModules?.let { m ->
        additionalGtxModules(m.mapNotNull { whitelistedTestGtxModuleMap[it] })
    }
}
