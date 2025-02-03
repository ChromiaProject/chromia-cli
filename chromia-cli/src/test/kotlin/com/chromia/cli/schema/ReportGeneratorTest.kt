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
        val differences = listOf(
            EntityDifference(
                name = "User",
                changeType = ChangeType.ADDED,
                fieldDifferences = emptyList()
            )
        )

        val changesReport = reportGenerator.getSchemaChangesReport(differences, "test")

        assertThat(changesReport.report).contains("Entity 'User' added.")
        assertThat(changesReport.containsUnsafeChanges).isFalse()
    }

    @Test
    fun `should report removed entity`() {
        val differences = listOf(
            EntityDifference(
                name = "User",
                changeType = ChangeType.REMOVED,
                fieldDifferences = emptyList()
            )
        )

        val changesReport = reportGenerator.getSchemaChangesReport(differences, "test")

        assertThat(changesReport.report).contains("WARNING: Entity 'User' removed.")
        assertThat(changesReport.containsUnsafeChanges).isTrue()
    }

    @Test
    fun `should report added field in modified entity`() {
        val differences = listOf(
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
        )

        val changesReport = reportGenerator.getSchemaChangesReport(differences, "test")

        assertThat(changesReport.report).contains("'User' modified:")
        assertThat(changesReport.report).contains("Attribute 'email' added to 'User'.")
        assertThat(changesReport.containsUnsafeChanges).isFalse()
    }

    @Test
    fun `should report modified field with type change`() {
        val differences = listOf(
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
        )

        val changesReport = reportGenerator.getSchemaChangesReport(differences, "test")

        assertThat(changesReport.report).contains("Attribute 'age' modified in 'User'")
        assertThat(changesReport.report).contains("Type changed from 'integer' to 'text'")
        assertThat(changesReport.containsUnsafeChanges).isTrue()
    }

    @Test
    fun `should report nullable property change`() {
        val differences = listOf(
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
        )

        val changesReport = reportGenerator.getSchemaChangesReport(differences, "test")

        assertThat(changesReport.report).contains("Nullable property changed")
    }

    @Test
    fun `should report default value change`() {
        val differences = listOf(
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
        )

        val changesReport = reportGenerator.getSchemaChangesReport(differences, "test")

        assertThat(changesReport.report).contains("Default value changed")
    }
} 