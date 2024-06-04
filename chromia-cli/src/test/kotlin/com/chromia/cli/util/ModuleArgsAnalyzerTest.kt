package com.chromia.cli.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import net.postchain.client.exception.ClientError
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.gtvml.GtvMLParser
import org.junit.jupiter.api.Test

class ModuleArgsAnalyzerTest {

    // Rell versions up until 0.13.12
    @Test
    fun legacy() {
        val analyzer = RellModuleArgsAnalyzer { name, _ ->
            require(name == "rell.get_module_args")
            throw ClientError("Query not found", null, "error", null)
        }
        val args = analyzer.getModuleArgs()
        assertThat(args).isNull()
    }

    // Rell version 0.13.13+
    @Test
    fun new() {
        val gtv = GtvMLParser.parseGtvML(javaClass.getResource("module_args_gtv.xml")!!.readText())
        val analyzer = RellModuleArgsAnalyzer { name, _ ->
            require(name == "rell.get_module_args")
            gtv
        }
        val args = analyzer.getModuleArgs()
        assertThat(args).isNotNull()
        assertThat(args!!.size).isEqualTo(3)
        assertThat(args["common"]!!["provider_quota_max_actions_per_day"]).isEqualTo(gtv(100))
    }
}
