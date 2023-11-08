package com.chromia.cli.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import net.postchain.gtv.parse.GtvParser
import org.junit.jupiter.api.Test

class BlockchainAnalyzerTest {

    // Rell versions up until 0.13.2
    @Test
    fun legacyStructureTest() {
        val analyzer = BlockchainAnalyzer { name, _ ->
            require(name == "rell.get_app_structure")
            GtvParser.parse(

                    "{modules={main={entities={house={attributes={floor_area={mutable=0, type=\"integer\"}, number={mutable=0, type=\"integer\"}, number_of_floors={mutable=0, type=\"integer\"}, number_of_rooms={mutable=0, type=\"integer\"}, street={mutable=0, type=\"main:street\"}}, indexes=[{attributes=[\"street\"]}], keys=[{attributes=[\"street\", \"number\"]}], log=0, mount=\"house\"}, my_entity={attributes={my_immutable={mutable=0, type=\"text\"}, my_index={mutable=0, type=\"text\"}, my_key={mutable=0, type=\"text\"}, my_mutable={mutable=1, type=\"text\"}}, indexes=[{attributes=[\"my_index\"]}], keys=[{attributes=[\"my_key\"]}], log=0, mount=\"my_entity\"}, street={attributes={address={mutable=0, type=\"text\"}}, indexes=[], keys=[{attributes=[\"address\"]}], log=0, mount=\"street\"}}, functions={a={parameters=[], type=\"unit\"}, foo={parameters=[], type={type=\"list\", value=\"integer\"}}, is_mansion={parameters=[{name=\"house\", type=\"main:house_info\"}], type=\"boolean\"}}, name=\"main\", operations={create_house={mount=\"create_house\", parameters=[{name=\"owner\", type=\"text\"}, {name=\"address\", type=\"text\"}]}, mutable_op={mount=\"mutable_op\", parameters=[]}}, structs={house_info={attributes={floor_area={mutable=0, type=\"integer\"}, number_of_floors={mutable=0, type=\"integer\"}, number_of_rooms={mutable=0, type=\"integer\"}}}, module_args={attributes={bar={mutable=0, type=\"text\"}, foo={mutable=0, type=\"integer\"}}}}}}}"
            )
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
        val analyzer = BlockchainAnalyzer { name, _ ->
            require(name == "rell.get_app_structure")
            GtvParser.parse(

                    "{modules={main={entities={house={attributes=[{mutable=0, name=\"street\", type=\"main:street\"}, {mutable=0, name=\"number\", type=\"integer\"}, {mutable=0, name=\"number_of_rooms\", type=\"integer\"}, {mutable=0, name=\"number_of_floors\", type=\"integer\"}, {mutable=0, name=\"floor_area\", type=\"integer\"}], indexes=[{attributes=[\"street\"]}], keys=[{attributes=[\"street\", \"number\"]}], log=0, mount=\"house\"}, my_entity={attributes=[{mutable=0, name=\"my_key\", type=\"text\"}, {mutable=0, name=\"my_index\", type=\"text\"}, {mutable=1, name=\"my_mutable\", type=\"text\"}, {mutable=0, name=\"my_immutable\", type=\"text\"}], indexes=[{attributes=[\"my_index\"]}], keys=[{attributes=[\"my_key\"]}], log=0, mount=\"my_entity\"}, street={attributes=[{mutable=0, name=\"address\", type=\"text\"}], indexes=[], keys=[{attributes=[\"address\"]}], log=0, mount=\"street\"}}, functions={a={parameters=[], type=\"unit\"}, foo={parameters=[], type={type=\"list\", value=\"integer\"}}, is_mansion={parameters=[{name=\"house\", type=\"main:house_info\"}], type=\"boolean\"}}, name=\"main\", operations={create_house={mount=\"create_house\", parameters=[{name=\"owner\", type=\"text\"}, {name=\"address\", type=\"text\"}]}, mutable_op={mount=\"mutable_op\", parameters=[]}}, structs={house_info={attributes=[{mutable=0, name=\"number_of_rooms\", type=\"integer\"}, {mutable=0, name=\"number_of_floors\", type=\"integer\"}, {mutable=0, name=\"floor_area\", type=\"integer\"}]}, module_args={attributes=[{mutable=0, name=\"foo\", type=\"integer\"}, {mutable=0, name=\"bar\", type=\"text\"}]}}}}}"
            )
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
        val analyzer = BlockchainAnalyzer { name, _ ->
            require(name == "rell.get_app_structure")
            GtvParser.parse(

                    "{modules={main={entities={house={attributes=[{mutable=0, name=\"street\", type=\"main:street\"}, {mutable=0, name=\"number\", type=\"integer\"}, {mutable=0, name=\"number_of_rooms\", type=\"integer\"}, {mutable=0, name=\"number_of_floors\", type=\"integer\"}, {mutable=0, name=\"floor_area\", type=\"integer\"}], indexes=[{attributes=[\"street\"]}], keys=[{attributes=[\"street\", \"number\"]}], log=0, mount=\"house\"}, my_entity={attributes=[{mutable=0, name=\"my_key\", type=\"text\"}, {mutable=0, name=\"my_index\", type=\"text\"}, {mutable=1, name=\"my_mutable\", type=\"text\"}, {mutable=0, name=\"my_immutable\", type=\"text\"}], indexes=[{attributes=[\"my_index\"]}], keys=[{attributes=[\"my_key\"]}], log=0, mount=\"my_entity\"}, street={attributes=[{mutable=0, name=\"address\", type=\"text\"}], indexes=[], keys=[{attributes=[\"address\"]}], log=0, mount=\"street\"}}, functions={a={parameters=[], type=\"unit\"}, foo={parameters=[], type={type=\"list\", value=\"integer\"}}, is_mansion={parameters=[{name=\"house\", type=\"main:house_info\"}], type=\"boolean\"}}, name=\"main\", operations={create_house={mount=\"create_house\", parameters=[{name=\"owner\", type=\"text\"}, {name=\"address\", type=\"text\"}]}, mutable_op={mount=\"mutable_op\", parameters=[]}}, structs={house_info={attributes=[{mutable=0, name=\"number_of_rooms\", type=\"integer\"}, {mutable=0, name=\"number_of_floors\", type=\"integer\"}, {mutable=0, name=\"floor_area\", type=\"integer\"}]}}}}}"
            )
        }
        val structure = analyzer.getAppStructure()
        assertThat(structure.size).isEqualTo(1)
        assertThat(structure["main"]!!.structures!!.size).isEqualTo(1)
        assertThat(structure["main"]!!.structures!!["module_args"]).isNull()
    }
}
