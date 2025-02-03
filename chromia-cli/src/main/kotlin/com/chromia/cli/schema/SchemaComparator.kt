package com.chromia.cli.schema

class SchemaComparator {
    fun compareSchemas(oldSchema: Schema, newSchema: Schema): List<EntityDifference> {
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
}
