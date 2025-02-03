package com.chromia.cli.schema

data class Report(val report: String, val containsUnsafeChanges: Boolean)

class ReportGenerator {

    fun getSchemaChangesReport(differences: List<EntityDifference>, chain: String): Report {
        val report = StringBuilder()
        var containsUnsafeChanges = false
        report.appendLine("-".repeat(80))
        report.appendLine("Schema changes for $chain")
        report.appendLine("-".repeat(80))

        for (diff in differences) {
            when (diff.changeType) {
                ChangeType.ADDED -> report.appendLine("${diff.type} '${diff.name}' added.")
                ChangeType.REMOVED -> {
                    containsUnsafeChanges = true
                    report.appendLine("WARNING: ${diff.type} '${diff.name}' removed. Please note that the corresponding table isn't physically dropped.")
                }
                ChangeType.MODIFIED -> {
                    report.appendLine("${diff.type} '${diff.name}' modified:")
                    for (fieldDiff in diff.fieldDifferences) {
                        when (fieldDiff.changeType) {
                            ChangeType.ADDED -> {
                                 report.appendLine("\tAttribute '${fieldDiff.name}' added to '${diff.name}'.")
                            }

                            ChangeType.REMOVED -> {
                                report.appendLine("\tWARNING: Attribute '${fieldDiff.name}' removed from '${diff.name}'. Please note that the corresponding column isn't physically dropped.")
                                containsUnsafeChanges = true
                            }
                            ChangeType.MODIFIED -> {
                                val fieldReport = StringBuilder()
                                if (fieldDiff.oldField?.type != fieldDiff.newField?.type) {
                                    containsUnsafeChanges = true
                                    fieldReport.append(" Type changed from '${fieldDiff.oldField?.type}' to '${fieldDiff.newField?.type}'.")
                                }
                                if (fieldDiff.oldField?.nullable != fieldDiff.newField?.nullable) {
                                    containsUnsafeChanges = true
                                    fieldReport.append(" Nullable property changed.")
                                }
                                if (fieldDiff.oldField?.defaultValue != fieldDiff.newField?.defaultValue) {
                                    containsUnsafeChanges = true
                                    fieldReport.append(" Default value changed.")
                                }
                                if (fieldDiff.oldField?.indexKind != fieldDiff.newField?.indexKind) {
                                    val (message, isUnsafe) = getIndexChangeMessage(fieldDiff.oldField, fieldDiff.newField)
                                    containsUnsafeChanges = containsUnsafeChanges || isUnsafe
                                    fieldReport.append(message)
                                }
                                val prefix = if (containsUnsafeChanges) "WARNING: " else ""
                                report.append("\t${prefix}Attribute '${fieldDiff.name}' modified in '${diff.name}'.")
                                report.append(fieldReport.toString())
                                report.appendLine()
                            }
                        }
                    }
                }
            }
        }

        if (containsUnsafeChanges) {
            report.appendLine()
            report.appendLine("For more information on safe schema changes, visit: https://docs.chromia.com/rell/language-features/modules/entity#changing-entity-definitions")
        }

        return Report(report.toString(), containsUnsafeChanges)
    }

    private fun getIndexChangeMessage(oldField: Field?, newField: Field?): Pair<String, Boolean> {
        return when {
            oldField?.indexKind == null && newField?.indexKind != null -> {
                val message = " ${newField.indexKind.displayName} added."
                Pair(message, false)
            }

            oldField?.indexKind != null && newField?.indexKind == null -> {
                val message = " ${oldField.indexKind.displayName} removed."
                Pair(message, false)
            }

            oldField?.indexKind != null && newField?.indexKind != null &&
                    oldField.indexKind != newField.indexKind -> {
                val message = " Changed index type from ${oldField.indexKind.displayName} " +
                        "to ${newField.indexKind.displayName}."
                Pair(message, false)
            }

            else -> Pair("", false)
        }
    }
}
