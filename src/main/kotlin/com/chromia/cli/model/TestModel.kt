package com.chromia.cli.model

import net.postchain.gtv.Gtv

data class TestModel(
        val modules: List<String> = listOf(),
        val moduleArgs: Map<String, Map<String, Gtv>> = mapOf(),
        val failOnError: Boolean = true,
)
