package com.chromia.cli.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import net.postchain.gtv.gtvml.GtvMLParser
import org.junit.jupiter.api.Test

class BlockchainAnalyzerTest {

    // Rell versions up until 0.13.2
    @Test
    fun legacyStructureTest() {
        val gtv = GtvMLParser.parseGtvML(javaClass.getResource("legacy_structure_gtv.xml")!!.readText())
        val analyzer = BlockchainAnalyzer { name, _ ->
            require(name == "rell.get_app_structure")
            gtv
        }
        val structure = analyzer.getAppStructure()
        assertThat(structure.size).isEqualTo(1)
        assertThat(structure["main"]!!.structures!!.size).isEqualTo(2)
        assertThat(structure["main"]!!.structures!!["module_args"]!!.isLegacy).isTrue()
        assertThat(structure["main"]!!.structures!!["module_args"]!!.legacyAttributes.size).isEqualTo(2)
    }

    // Rell version 0.13.3+
    @Test
    fun structureTest() {
        val gtv = GtvMLParser.parseGtvML(javaClass.getResource("structure_gtv.xml")!!.readText())
        val analyzer = BlockchainAnalyzer { name, _ ->
            require(name == "rell.get_app_structure")
            gtv
        }
        val structure = analyzer.getAppStructure()
        assertThat(structure.size).isEqualTo(1)
        assertThat(structure["main"]!!.structures!!.size).isEqualTo(2)
        assertThat(structure["main"]!!.structures!!["module_args"]!!.isLegacy).isFalse()
        assertThat(structure["main"]!!.structures!!["module_args"]!!.attributes.size).isEqualTo(2)
    }

    // No module args
    @Test
    fun noModuleArgsTest() {
        val gtv = GtvMLParser.parseGtvML(javaClass.getResource("no_module_args_gtv.xml")!!.readText())
        val analyzer = BlockchainAnalyzer { name, _ ->
            require(name == "rell.get_app_structure")
            gtv
        }
        val structure = analyzer.getAppStructure()
        assertThat(structure.size).isEqualTo(1)
        assertThat(structure["main"]!!.structures!!.size).isEqualTo(1)
        assertThat(structure["main"]!!.structures!!["module_args"]).isNull()
    }
}
