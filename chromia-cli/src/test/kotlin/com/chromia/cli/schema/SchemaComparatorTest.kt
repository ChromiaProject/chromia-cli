package com.chromia.cli.schema

import assertk.all
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import assertk.assertions.*
import org.junit.jupiter.api.Test

class SchemaComparatorTest {
    private var comparator = SchemaComparator()

    @Test
    fun `when comparing identical schemas should return empty list`() {
        val schema = Schema(listOf(
                Entity("user", listOf(
                        Field("name", "text", false, null),
                        Field("age", "integer", false, null)
                ), isObject = false)
        ))

        val differences = comparator.compareSchemas(schema, schema)

        assertThat(differences).isEmpty()
    }

    @Test
    fun `when entity is added should detect addition with all field properties`() {
        // given
        val oldSchema = Schema(listOf(
                Entity("user", listOf(
                        Field("name", "text", false, null)
                ), isObject = false)
        ))

        val newSchema = Schema(listOf(
                Entity("user", listOf(
                        Field("name", "text", false, null)
                ), isObject = false),
                Entity("post", listOf(
                        Field("title", "text", false, "Draft"),
                        Field("content", "text", false, null)
                ), isObject = true)
        ))

        val differences = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(differences).hasSize(1)
        assertThat(differences[0]).all {
            prop(EntityDifference::name).isEqualTo("post")
            prop(EntityDifference::changeType).isEqualTo(ChangeType.ADDED)
            prop(EntityDifference::fieldDifferences).hasSize(2)
        }

        val titleDiff = differences[0].fieldDifferences.find { it.name == "title" }!!
        assertThat(titleDiff).all {
            prop(FieldDifference::oldField).isNull()
            prop(FieldDifference::newField).isNotNull().transform { field ->
                assertThat(field.nullable).isFalse()
                assertThat(field.defaultValue).isEqualTo("Draft")
                assertThat(field.type).isEqualTo("text")
            }
        }
    }

    @Test
    fun `when entity changes from regular to object type should detect modification`() {
        val oldSchema = Schema(listOf(
                Entity("config", listOf(
                        Field("settings", "map", false, null)
                ), isObject = false)
        ))

        val newSchema = Schema(listOf(
                Entity("config", listOf(
                        Field("settings", "map", false, null)
                ), isObject = true)
        ))

        val differences = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(differences).hasSize(1)
        assertThat(differences[0]).all {
            prop(EntityDifference::name).isEqualTo("config")
            prop(EntityDifference::changeType).isEqualTo(ChangeType.MODIFIED)
        }
    }

    @Test
    fun `when multiple fields are modified in different ways should detect all changes`() {
        val oldSchema = Schema(listOf(
                Entity("user", listOf(
                        Field("name", "text", false, null),
                        Field("age", "integer", false, "0"),
                        Field("status", "string", false, "active")
                ), isObject = false)
        ))

        val newSchema = Schema(listOf(
                Entity("user", listOf(
                        Field("name", "text", true, null),
                        Field("age", "text", false, "0"),
                        Field("email", "text", false, null)
                ), isObject = false)
        ))

        val differences = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(differences).hasSize(1)
        val entityDiff = differences[0]
        assertThat(entityDiff.fieldDifferences).hasSize(4)

        assertThat(entityDiff.fieldDifferences).extracting(FieldDifference::name)
                .containsAll("name", "age", "status", "email")

        val nameDiff = entityDiff.fieldDifferences.find { it.name == "name" }!!
        assertThat(nameDiff).all {
            prop(FieldDifference::changeType).isEqualTo(ChangeType.MODIFIED)
            prop(FieldDifference::oldField).isNotNull().transform { it.nullable }.isFalse()
            prop(FieldDifference::newField).isNotNull().transform { it.nullable }.isTrue()
        }

        val ageDiff = entityDiff.fieldDifferences.find { it.name == "age" }!!
        assertThat(ageDiff).all {
            prop(FieldDifference::changeType).isEqualTo(ChangeType.MODIFIED)
            prop(FieldDifference::oldField).isNotNull().transform { it.type }.isEqualTo("integer")
            prop(FieldDifference::newField).isNotNull().transform { it.type }.isEqualTo("text")
        }
    }

    @Test
    fun `when comparing empty schemas should return empty list`() {
        val emptySchema = Schema(emptyList())
        val differences = comparator.compareSchemas(emptySchema, emptySchema)
        assertThat(differences).isEmpty()
    }

    @Test
    fun `when comparing schema with empty entity should handle field differences correctly`() {
        val oldSchema = Schema(listOf(
                Entity("empty_entity", emptyList(), isObject = false)
        ))

        val newSchema = Schema(listOf(
                Entity("empty_entity", listOf(
                        Field("new_field", "text", false, null)
                ), isObject = false)
        ))

        val differences = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(differences).hasSize(1)
        assertThat(differences[0].fieldDifferences).hasSize(1)
        assertThat(differences[0].fieldDifferences[0]).all {
            prop(FieldDifference::name).isEqualTo("new_field")
            prop(FieldDifference::changeType).isEqualTo(ChangeType.ADDED)
        }
    }

    @Test
    fun `when field has complex changes should detect all modifications`() {
        val oldSchema = Schema(listOf(
                Entity("product", listOf(
                        Field("price", "decimal", false, "0.0")
                ), isObject = false)
        ))

        val newSchema = Schema(listOf(
                Entity("product", listOf(
                        Field("price", "decimal", true, "9.99")
                ), isObject = false)
        ))

        val differences = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(differences).hasSize(1)
        val fieldDiff = differences[0].fieldDifferences[0]
        assertThat(fieldDiff).all {
            prop(FieldDifference::changeType).isEqualTo(ChangeType.MODIFIED)
            prop(FieldDifference::oldField).isNotNull().all {
                prop(Field::nullable).isFalse()
                prop(Field::defaultValue).isEqualTo("0.0")
            }
            prop(FieldDifference::newField).isNotNull().all {
                prop(Field::nullable).isTrue()
                prop(Field::defaultValue).isEqualTo("9.99")
            }
        }
    }
}