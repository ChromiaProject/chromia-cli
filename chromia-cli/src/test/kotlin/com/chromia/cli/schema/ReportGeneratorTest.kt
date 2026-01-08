package com.chromia.cli.schema

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class ReportGeneratorTest {
    private val reportGenerator = ReportGenerator()

    @Test
    fun `should report added entity`() {
        val comparison = SchemaComparison(
            entityDifferences = listOf(
                EntityDifference(
                    name = "User",
                    changeType = ChangeType.ADDED,
                    fieldDifferences = emptyList()
                )
            ),
            enumDifferences = emptyList()
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("Entity 'User' added.")
        assertThat(changesReport.containsUnsafeChanges).isFalse()
    }

    @Test
    fun `should report removed entity`() {
        val comparison = SchemaComparison(
            entityDifferences = listOf(
                EntityDifference(
                    name = "User",
                    changeType = ChangeType.REMOVED,
                    fieldDifferences = emptyList()
                )
            ),
            enumDifferences = emptyList()
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("WARNING: Entity 'User' removed.")
        assertThat(changesReport.containsUnsafeChanges).isTrue()
    }

    @Test
    fun `should report added field in modified entity`() {
        val comparison = SchemaComparison(
            entityDifferences = listOf(
                EntityDifference(
                    name = "User",
                    changeType = ChangeType.MODIFIED,
                    fieldDifferences = listOf(
                        FieldDifference(
                            name = "email",
                            changeType = ChangeType.ADDED,
                            oldField = null,
                            newField = Field("email", "text", false, null)
                        )
                    )
                )
            ),
            enumDifferences = emptyList()
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("'User' modified:")
        assertThat(changesReport.report).contains("Attribute 'email' added to 'User'.")
        assertThat(changesReport.containsUnsafeChanges).isFalse()
    }

    @Test
    fun `should report modified field with type change`() {
        val comparison = SchemaComparison(
            entityDifferences = listOf(
                EntityDifference(
                    name = "User",
                    changeType = ChangeType.MODIFIED,
                    fieldDifferences = listOf(
                        FieldDifference(
                            name = "age",
                            changeType = ChangeType.MODIFIED,
                            oldField = Field("age", "integer", false, null),
                            newField = Field("age", "text", false, null)
                        )
                    )
                )
            ),
            enumDifferences = emptyList()
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("Attribute 'age' modified in 'User'")
        assertThat(changesReport.report).contains("Type changed from 'integer' to 'text'")
        assertThat(changesReport.containsUnsafeChanges).isTrue()
    }

    @Test
    fun `should report nullable property change`() {
        val comparison = SchemaComparison(
            entityDifferences = listOf(
                EntityDifference(
                    name = "User",
                    changeType = ChangeType.MODIFIED,
                    fieldDifferences = listOf(
                        FieldDifference(
                            name = "email",
                            changeType = ChangeType.MODIFIED,
                            oldField = Field("email", "text", false, null),
                            newField = Field("email", "text", true, null)
                        )
                    )
                )
            ),
            enumDifferences = emptyList()
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("Nullable property changed")
    }

    @Test
    fun `should report default value change`() {
        val comparison = SchemaComparison(
            entityDifferences = listOf(
                EntityDifference(
                    name = "User",
                    changeType = ChangeType.MODIFIED,
                    fieldDifferences = listOf(
                        FieldDifference(
                            name = "status",
                            changeType = ChangeType.MODIFIED,
                            oldField = Field("status", "text", false, "active"),
                            newField = Field("status", "text", false, "pending")
                        )
                    )
                )
            ),
            enumDifferences = emptyList()
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("Default value changed")
    }

    @Test
    fun `should report added enum`() {
        val comparison = SchemaComparison(
            entityDifferences = emptyList(),
            enumDifferences = listOf(
                EnumDifference(
                    name = "status",
                    changeType = ChangeType.ADDED,
                    valueDifferences = listOf(
                        EnumFieldDifference("active", null, EnumField("active", 0), ChangeType.ADDED),
                        EnumFieldDifference("inactive", null, EnumField("inactive", 1), ChangeType.ADDED)
                    ),
                    isDangerous = false
                )
            )
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("Enum 'status' added.")
        assertThat(changesReport.containsUnsafeChanges).isFalse()
    }

    @Test
    fun `should report safe enum modification when value added at end`() {
        val comparison = SchemaComparison(
            entityDifferences = emptyList(),
            enumDifferences = listOf(
                EnumDifference(
                    name = "status",
                    changeType = ChangeType.MODIFIED,
                    valueDifferences = listOf(
                        EnumFieldDifference("banned", null, EnumField("banned", 2), ChangeType.ADDED)
                    ),
                    isDangerous = false
                )
            )
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("Enum 'status' modified:")
        assertThat(changesReport.report).contains("Value 'banned' added at ordinal 2.")
        assertThat(changesReport.containsUnsafeChanges).isFalse()
    }

    @Test
    fun `should report dangerous enum modification when value added in middle`() {
        val comparison = SchemaComparison(
            entityDifferences = emptyList(),
            enumDifferences = listOf(
                EnumDifference(
                    name = "status",
                    changeType = ChangeType.MODIFIED,
                    valueDifferences = listOf(
                        EnumFieldDifference("inactive", null, EnumField("inactive", 1), ChangeType.ADDED),
                        EnumFieldDifference("banned", EnumField("banned", 1), EnumField("banned", 2), ChangeType.MODIFIED)
                    ),
                    isDangerous = true
                )
            )
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("WARNING: Enum 'status' modified:")
        assertThat(changesReport.report).contains("WARNING: Value 'inactive' added at ordinal 1 (not at the end - will shift existing ordinals).")
        assertThat(changesReport.containsUnsafeChanges).isTrue()
    }

    @Test
    fun `should report dangerous enum modification when value removed`() {
        val comparison = SchemaComparison(
            entityDifferences = emptyList(),
            enumDifferences = listOf(
                EnumDifference(
                    name = "status",
                    changeType = ChangeType.MODIFIED,
                    valueDifferences = listOf(
                        EnumFieldDifference("inactive", EnumField("inactive", 1), null, ChangeType.REMOVED)
                    ),
                    isDangerous = true
                )
            )
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("WARNING: Enum 'status' modified:")
        assertThat(changesReport.report).contains("WARNING: Value 'inactive' removed from 'status' (will shift ordinals of subsequent values).")
        assertThat(changesReport.containsUnsafeChanges).isTrue()
    }

    @Test
    fun `should report dangerous enum modification when ordinal changes`() {
        val comparison = SchemaComparison(
            entityDifferences = emptyList(),
            enumDifferences = listOf(
                EnumDifference(
                    name = "status",
                    changeType = ChangeType.MODIFIED,
                    valueDifferences = listOf(
                        EnumFieldDifference("active", EnumField("active", 0), EnumField("active", 1), ChangeType.MODIFIED),
                        EnumFieldDifference("inactive", EnumField("inactive", 1), EnumField("inactive", 0), ChangeType.MODIFIED)
                    ),
                    isDangerous = true
                )
            )
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("WARNING: Enum 'status' modified:")
        assertThat(changesReport.report).contains("WARNING: Value 'active' ordinal changed from 0 to 1.")
        assertThat(changesReport.report).contains("WARNING: Value 'inactive' ordinal changed from 1 to 0.")
        assertThat(changesReport.containsUnsafeChanges).isTrue()
    }

    @Test
    fun `should report both entity and enum changes together`() {
        val comparison = SchemaComparison(
            entityDifferences = listOf(
                EntityDifference(
                    name = "User",
                    changeType = ChangeType.ADDED,
                    fieldDifferences = emptyList()
                )
            ),
            enumDifferences = listOf(
                EnumDifference(
                    name = "status",
                    changeType = ChangeType.ADDED,
                    valueDifferences = listOf(
                        EnumFieldDifference("active", null, EnumField("active", 0), ChangeType.ADDED)
                    ),
                    isDangerous = false
                )
            )
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("Entity 'User' added.")
        assertThat(changesReport.report).contains("Enum 'status' added.")
        assertThat(changesReport.containsUnsafeChanges).isFalse()
    }

    @Test
    fun `should include documentation link when dangerous enum changes detected`() {
        val comparison = SchemaComparison(
            entityDifferences = emptyList(),
            enumDifferences = listOf(
                EnumDifference(
                    name = "status",
                    changeType = ChangeType.MODIFIED,
                    valueDifferences = listOf(
                        EnumFieldDifference("inactive", EnumField("inactive", 1), null, ChangeType.REMOVED)
                    ),
                    isDangerous = true
                )
            )
        )

        val changesReport = reportGenerator.getSchemaChangesReport(comparison, "test")

        assertThat(changesReport.report).contains("For more information on safe schema changes, visit: https://docs.chromia.com")
        assertThat(changesReport.containsUnsafeChanges).isTrue()
    }
} 