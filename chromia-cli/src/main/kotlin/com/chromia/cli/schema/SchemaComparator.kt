package com.chromia.cli.schema

class SchemaComparator {
    fun compareSchemas(oldSchema: Schema, newSchema: Schema): SchemaComparison {
        val entityDifferences = compareEntities(oldSchema, newSchema)
        val enumDifferences = compareEnums(oldSchema, newSchema)
        return SchemaComparison(entityDifferences, enumDifferences)
    }

    private fun compareEntities(oldSchema: Schema, newSchema: Schema): List<EntityDifference> {
        val differences = mutableListOf<EntityDifference>()
        val allEntityNames = (oldSchema.entities.map { it.name } + newSchema.entities.map { it.name }).distinct()

        for (entityName in allEntityNames) {
            val oldEntity = oldSchema.entities.find { it.name == entityName }
            val newEntity = newSchema.entities.find { it.name == entityName }

            when {
                oldEntity == null -> differences.add(createEntityDifference(entityName, null, newEntity!!, ChangeType.ADDED))
                newEntity == null -> differences.add(createEntityDifference(entityName, oldEntity, null, ChangeType.REMOVED))
                else -> {
                    val fieldDiffs = compareFields(oldEntity, newEntity)
                    if (fieldDiffs.isNotEmpty() || oldEntity.isObject != newEntity.isObject) {
                        differences.add(EntityDifference(entityName, fieldDiffs, ChangeType.MODIFIED, oldEntity.isObject))
                    }
                }
            }
        }
        return differences
    }

    private fun compareEnums(oldSchema: Schema, newSchema: Schema): List<EnumDifference> {
        val differences = mutableListOf<EnumDifference>()
        val allEnumNames = (oldSchema.enums.map { it.name } + newSchema.enums.map { it.name }).distinct()

        for (enumName in allEnumNames) {
            val oldEnum = oldSchema.enums.find { it.name == enumName }
            val newEnum = newSchema.enums.find { it.name == enumName }

            when {
                oldEnum == null -> differences.add(createEnumDifference(enumName, null, newEnum!!, ChangeType.ADDED))
                newEnum == null -> differences.add(createEnumDifference(enumName, oldEnum, null, ChangeType.REMOVED))
                else -> {
                    val valueDiffs = compareEnumValues(oldEnum, newEnum)
                    if (valueDiffs.isNotEmpty()) {
                        val isDangerous = isEnumChangeDangerous(oldEnum, newEnum, valueDiffs)
                        differences.add(EnumDifference(enumName, valueDiffs, ChangeType.MODIFIED, isDangerous))
                    }
                }
            }
        }
        return differences
    }

    private fun compareFields(oldEntity: Entity, newEntity: Entity): List<FieldDifference> {
        val differences = mutableListOf<FieldDifference>()
        val allFieldNames = (oldEntity.fields.map { it.name } + newEntity.fields.map { it.name }).distinct()

        for (fieldName in allFieldNames) {
            val oldField = oldEntity.fields.find { it.name == fieldName }
            val newField = newEntity.fields.find { it.name == fieldName }

            when {
                oldField == null -> differences.add(FieldDifference(fieldName, null, newField, ChangeType.ADDED))
                newField == null -> differences.add(FieldDifference(fieldName, oldField, null, ChangeType.REMOVED))
                !fieldEqual(oldField, newField) -> differences.add(FieldDifference(fieldName, oldField, newField, ChangeType.MODIFIED))
            }
        }
        return differences
    }

    private fun fieldEqual(field1: Field, field2: Field): Boolean {
        return field1.type == field2.type &&
                field1.nullable == field2.nullable &&
                field1.defaultValue == field2.defaultValue &&
                field1.indexKind == field2.indexKind
    }

    private fun createEntityDifference(name: String, oldEntity: Entity?, newEntity: Entity?, changeType: ChangeType): EntityDifference {
        val fieldDiffs = when (changeType) {
            ChangeType.ADDED -> newEntity!!.fields.map { FieldDifference(it.name, null, it, ChangeType.ADDED) }
            ChangeType.REMOVED -> oldEntity!!.fields.map { FieldDifference(it.name, it, null, ChangeType.REMOVED) }
            else -> emptyList()
        }
        return EntityDifference(name, fieldDiffs, changeType, (oldEntity?.isObject ?: newEntity!!.isObject))
    }

    private fun compareEnumValues(oldEnum: Enum, newEnum: Enum): List<EnumFieldDifference> {
        val differences = mutableListOf<EnumFieldDifference>()
        val allValueNames = (oldEnum.values.map { it.name } + newEnum.values.map { it.name }).distinct()

        for (valueName in allValueNames) {
            val oldValue = oldEnum.values.find { it.name == valueName }
            val newValue = newEnum.values.find { it.name == valueName }

            when {
                oldValue == null -> differences.add(EnumFieldDifference(valueName, null, newValue, ChangeType.ADDED))
                newValue == null -> differences.add(EnumFieldDifference(valueName, oldValue, null, ChangeType.REMOVED))
                oldValue.ordinal != newValue.ordinal -> differences.add(EnumFieldDifference(valueName, oldValue, newValue, ChangeType.MODIFIED))
            }
        }
        return differences
    }

    private fun isEnumChangeDangerous(oldEnum: Enum, newEnum: Enum, valueDiffs: List<EnumFieldDifference>): Boolean {
        // Any removal is dangerous as it shifts ordinals
        if (valueDiffs.any { it.changeType == ChangeType.REMOVED }) {
            return true
        }

        // Check if any existing value has changed ordinal (dangerous)
        for (diff in valueDiffs) {
            if (diff.changeType == ChangeType.MODIFIED && diff.oldField != null && diff.newField != null) {
                if (diff.oldField.ordinal != diff.newField.ordinal) {
                    return true
                }
            }
        }

        // Check if any new values were added but not at the end
        val addedValues = valueDiffs.filter { it.changeType == ChangeType.ADDED }
        if (addedValues.isNotEmpty()) {
            val maxOldOrdinal = oldEnum.values.maxOfOrNull { it.ordinal } ?: -1
            for (added in addedValues) {
                if (added.newField != null && added.newField.ordinal <= maxOldOrdinal) {
                    // New value was inserted in the middle or beginning, not at the end
                    return true
                }
            }
        }

        return false
    }

    private fun createEnumDifference(name: String, oldEnum: Enum?, newEnum: Enum?, changeType: ChangeType): EnumDifference {
        val valueDiffs = when (changeType) {
            ChangeType.ADDED -> newEnum!!.values.map { EnumFieldDifference(it.name, null, it, ChangeType.ADDED) }
            ChangeType.REMOVED -> oldEnum!!.values.map { EnumFieldDifference(it.name, it, null, ChangeType.REMOVED) }
            else -> emptyList()
        }
        return EnumDifference(name, valueDiffs, changeType, isDangerous = false)
    }
}
