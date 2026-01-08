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

        val comparison = comparator.compareSchemas(schema, schema)

        assertThat(comparison.entityDifferences).isEmpty()
        assertThat(comparison.enumDifferences).isEmpty()
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

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.entityDifferences).hasSize(1)
        assertThat(comparison.entityDifferences[0]).all {
            prop(EntityDifference::name).isEqualTo("post")
            prop(EntityDifference::changeType).isEqualTo(ChangeType.ADDED)
            prop(EntityDifference::fieldDifferences).hasSize(2)
        }

        val titleDiff = comparison.entityDifferences[0].fieldDifferences.find { it.name == "title" }!!
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

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.entityDifferences).hasSize(1)
        assertThat(comparison.entityDifferences[0]).all {
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

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.entityDifferences).hasSize(1)
        val entityDiff = comparison.entityDifferences[0]
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
        val comparison = comparator.compareSchemas(emptySchema, emptySchema)
        assertThat(comparison.entityDifferences).isEmpty()
        assertThat(comparison.enumDifferences).isEmpty()
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

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.entityDifferences).hasSize(1)
        assertThat(comparison.entityDifferences[0].fieldDifferences).hasSize(1)
        assertThat(comparison.entityDifferences[0].fieldDifferences[0]).all {
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

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.entityDifferences).hasSize(1)
        val fieldDiff = comparison.entityDifferences[0].fieldDifferences[0]
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

    @Test
    fun `when enum is added should detect addition`() {
        val oldSchema = Schema(emptyList(), emptyList())
        val newSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("active", 0),
                                EnumField("inactive", 1)
                        ))
                )
        )

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.enumDifferences).hasSize(1)
        assertThat(comparison.enumDifferences[0]).all {
            prop(EnumDifference::name).isEqualTo("status")
            prop(EnumDifference::changeType).isEqualTo(ChangeType.ADDED)
            prop(EnumDifference::isDangerous).isFalse()
        }
    }

    @Test
    fun `when enum is removed should detect removal`() {
        val oldSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("active", 0),
                                EnumField("inactive", 1)
                        ))
                )
        )
        val newSchema = Schema(emptyList(), emptyList())

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.enumDifferences).hasSize(1)
        assertThat(comparison.enumDifferences[0]).all {
            prop(EnumDifference::name).isEqualTo("status")
            prop(EnumDifference::changeType).isEqualTo(ChangeType.REMOVED)
        }
    }

    @Test
    fun `when enum value is added at end should not be dangerous`() {
        val oldSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("active", 0),
                                EnumField("inactive", 1)
                        ))
                )
        )
        val newSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("active", 0),
                                EnumField("inactive", 1),
                                EnumField("banned", 2)
                        ))
                )
        )

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.enumDifferences).hasSize(1)
        assertThat(comparison.enumDifferences[0]).all {
            prop(EnumDifference::name).isEqualTo("status")
            prop(EnumDifference::changeType).isEqualTo(ChangeType.MODIFIED)
            prop(EnumDifference::isDangerous).isFalse()
            prop(EnumDifference::valueDifferences).hasSize(1)
        }

        val valueDiff = comparison.enumDifferences[0].valueDifferences[0]
        assertThat(valueDiff).all {
            prop(EnumFieldDifference::name).isEqualTo("banned")
            prop(EnumFieldDifference::changeType).isEqualTo(ChangeType.ADDED)
            prop(EnumFieldDifference::oldField).isNull()
            prop(EnumFieldDifference::newField).isNotNull().all {
                prop(EnumField::name).isEqualTo("banned")
                prop(EnumField::ordinal).isEqualTo(2)
            }
        }
    }

    @Test
    fun `when enum value is added in middle should be dangerous`() {
        val oldSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("active", 0),
                                EnumField("banned", 1)
                        ))
                )
        )
        val newSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("active", 0),
                                EnumField("inactive", 1),
                                EnumField("banned", 2)
                        ))
                )
        )

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.enumDifferences).hasSize(1)
        assertThat(comparison.enumDifferences[0]).all {
            prop(EnumDifference::name).isEqualTo("status")
            prop(EnumDifference::changeType).isEqualTo(ChangeType.MODIFIED)
            prop(EnumDifference::isDangerous).isTrue()
        }

        assertThat(comparison.hasDangerousEnumChanges()).isTrue()
    }

    @Test
    fun `when enum value is removed should be dangerous`() {
        val oldSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("active", 0),
                                EnumField("inactive", 1),
                                EnumField("banned", 2)
                        ))
                )
        )
        val newSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("active", 0),
                                EnumField("banned", 1)
                        ))
                )
        )

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.enumDifferences).hasSize(1)
        assertThat(comparison.enumDifferences[0]).all {
            prop(EnumDifference::name).isEqualTo("status")
            prop(EnumDifference::changeType).isEqualTo(ChangeType.MODIFIED)
            prop(EnumDifference::isDangerous).isTrue()
        }

        val removedValue = comparison.enumDifferences[0].valueDifferences.find { it.name == "inactive" }
        assertThat(removedValue).isNotNull()
        assertThat(removedValue!!.changeType).isEqualTo(ChangeType.REMOVED)

        assertThat(comparison.hasDangerousEnumChanges()).isTrue()
    }

    @Test
    fun `when enum value ordinal changes should be dangerous`() {
        val oldSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("active", 0),
                                EnumField("inactive", 1)
                        ))
                )
        )
        val newSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("inactive", 0),
                                EnumField("active", 1)
                        ))
                )
        )

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.enumDifferences).hasSize(1)
        assertThat(comparison.enumDifferences[0]).all {
            prop(EnumDifference::name).isEqualTo("status")
            prop(EnumDifference::changeType).isEqualTo(ChangeType.MODIFIED)
            prop(EnumDifference::isDangerous).isTrue()
            prop(EnumDifference::valueDifferences).hasSize(2)
        }

        val activeValueDiff = comparison.enumDifferences[0].valueDifferences.find { it.name == "active" }
        assertThat(activeValueDiff).isNotNull()
        assertThat(activeValueDiff!!).all {
            prop(EnumFieldDifference::changeType).isEqualTo(ChangeType.MODIFIED)
            prop(EnumFieldDifference::oldField).isNotNull().prop(EnumField::ordinal).isEqualTo(0)
            prop(EnumFieldDifference::newField).isNotNull().prop(EnumField::ordinal).isEqualTo(1)
        }

        assertThat(comparison.hasDangerousEnumChanges()).isTrue()
    }

    @Test
    fun `when comparing schemas with multiple enum changes should detect all`() {
        val oldSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("active", 0),
                                EnumField("inactive", 1)
                        )),
                        Enum("priority", listOf(
                                EnumField("low", 0),
                                EnumField("high", 1)
                        ))
                )
        )
        val newSchema = Schema(
                emptyList(),
                listOf(
                        Enum("status", listOf(
                                EnumField("active", 0),
                                EnumField("inactive", 1),
                                EnumField("banned", 2)
                        )),
                        Enum("color", listOf(
                                EnumField("red", 0)
                        ))
                )
        )

        val comparison = comparator.compareSchemas(oldSchema, newSchema)

        assertThat(comparison.enumDifferences).hasSize(3)

        val statusDiff = comparison.enumDifferences.find { it.name == "status" }
        assertThat(statusDiff).isNotNull()
        assertThat(statusDiff!!.changeType).isEqualTo(ChangeType.MODIFIED)

        val priorityDiff = comparison.enumDifferences.find { it.name == "priority" }
        assertThat(priorityDiff).isNotNull()
        assertThat(priorityDiff!!.changeType).isEqualTo(ChangeType.REMOVED)

        val colorDiff = comparison.enumDifferences.find { it.name == "color" }
        assertThat(colorDiff).isNotNull()
        assertThat(colorDiff!!.changeType).isEqualTo(ChangeType.ADDED)
    }
}