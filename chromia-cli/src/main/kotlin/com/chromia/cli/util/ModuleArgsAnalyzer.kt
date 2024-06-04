package com.chromia.cli.util

import net.postchain.client.core.PostchainQuery
import net.postchain.client.exception.ClientError
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv

fun interface ModuleArgsAnalyzer {
    fun getModuleArgs(): Map<String, Map<String, Gtv>>?
}

class RellModuleArgsAnalyzer(private val client: PostchainQuery) : ModuleArgsAnalyzer {
    override fun getModuleArgs() = try {
        client.query("rell.get_module_args", gtv(mapOf("modules" to gtv(listOf())))).asDict().mapValues { it.value.asDict() }
    } catch (e: ClientError) {
        null
    }
}
