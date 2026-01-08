package com.chromia.cli.schema

data class Entity(
        val name: String,
        val fields: List<Field>,
        val isObject: Boolean = false
)

enum class IndexKind(val displayName: String) {
    UNIQUE("key"), INDEX("index")
}

data class Field(
        val name: String,
        val type: String,
        val nullable: Boolean = false,
        val defaultValue: String? = null,
        val indexKind: IndexKind? = null
)

data class EntityDifference(
        val name: String,
        val fieldDifferences: List<FieldDifference>,
        val changeType: ChangeType,
        val isObject: Boolean = false
) {
    val type: String = if (isObject) "Object" else "Entity"
}

data class FieldDifference(
        val name: String,
        val oldField: Field?,
        val newField: Field?,
        val changeType: ChangeType
)

data class EnumDifference(
        val name: String,
        val valueDifferences: List<EnumFieldDifference>,
        val changeType: ChangeType,
        val isDangerous: Boolean = false
)

data class EnumFieldDifference(
        val name: String,
        val oldField: EnumField?,
        val newField: EnumField?,
        val changeType: ChangeType
)

data class Enum(
        val name: String,
        val values: List<EnumField>
)

data class EnumField (
    val name: String,
    val ordinal: Int
)

data class Schema(val entities: List<Entity>, val enums: List<Enum> = emptyList())

data class SchemaComparison(
        val entityDifferences: List<EntityDifference>,
        val enumDifferences: List<EnumDifference>
) {
    fun hasDangerousEnumChanges(): Boolean = enumDifferences.any { it.isDangerous }
}

enum class ChangeType {
    ADDED, REMOVED, MODIFIED
}
