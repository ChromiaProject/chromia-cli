package com.chromia.cli.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.gtvml.GtvMLParser
import org.junit.jupiter.api.Test

class BlockchainAnalyzerTest {

    // Rell versions up until 0.13.2
    @Test
    fun legacyStructureTest() {
        val gtv = GtvMLParser.parseGtvML(javaClass.getResource("legacy_structure_gtv.xml")!!.readText())
        val analyzer = RellBlockchainAnalyzer { name, _ ->
            require(name == "rell.get_app_structure")
            gtv
        }
        val structure = analyzer.getAppStructure()
        assertThat(structure.size).isEqualTo(1)
        assertThat(structure["main"]!!.structures!!.size).isEqualTo(2)
        assertThat(structure["main"]!!.structures!!["module_args"]!!.attributes.size).isEqualTo(2)
        assertThat(structure["main"]!!.structures!!["module_args"]!!.attributes[0]).isEqualTo(RellAttribute("bar", gtv("text"), false))
        assertThat(structure["main"]!!.structures!!["module_args"]!!.attributes[1]).isEqualTo(RellAttribute("foo", gtv("integer"), false))
    }

    // Rell version 0.13.3+
    @Test
    fun structureTest() {
        val gtv = GtvMLParser.parseGtvML(javaClass.getResource("structure_gtv.xml")!!.readText())
        val analyzer = RellBlockchainAnalyzer { name, _ ->
            require(name == "rell.get_app_structure")
            gtv
        }
        val structure = analyzer.getAppStructure()
        assertThat(structure.size).isEqualTo(1)
        assertThat(structure["main"]!!.structures!!.size).isEqualTo(2)
        assertThat(structure["main"]!!.structures!!["module_args"]!!.attributes.size).isEqualTo(2)
        assertThat(structure["main"]!!.structures!!["module_args"]!!.attributes[0]).isEqualTo(RellAttribute("foo", gtv("integer"), false))
        assertThat(structure["main"]!!.structures!!["module_args"]!!.attributes[1]).isEqualTo(RellAttribute("bar", gtv("text"), false))
    }

    // No module args
    @Test
    fun noModuleArgsTest() {
        val gtv = GtvMLParser.parseGtvML(javaClass.getResource("no_module_args_gtv.xml")!!.readText())
        val analyzer = RellBlockchainAnalyzer { name, _ ->
            require(name == "rell.get_app_structure")
            gtv
        }
        val structure = analyzer.getAppStructure()
        assertThat(structure.size).isEqualTo(1)
        assertThat(structure["main"]!!.structures!!.size).isEqualTo(1)
        assertThat(structure["main"]!!.structures!!["module_args"]).isNull()
    }
}
