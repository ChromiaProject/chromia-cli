package com.chromia.cli.check_compatilibity

import net.postchain.rell.base.model.R_App
import net.postchain.rell.base.model.R_EntityDefinition
import net.postchain.rell.base.model.R_EntityType
import net.postchain.rell.base.model.R_Module

/**
 * Compared 2 compiled apps to identify changes. Currenlty updated entities.
 */
class ConfigUpdates(
        private val oldApp: R_App,
        private val newApp: R_App,
        private val logger: LogWrapper
) {
    fun getUpdatedEntitiesWithDependencies(): Pair<Int, List<EntityDependency>> {

        val updatedEntities = getUpdatedEntities(oldApp, newApp)
        val entitiesTree = getEntityDependencies(oldApp, updatedEntities)
        val entitiesPopulateOrder = getEntityDependencyOrder(entitiesTree)

        if (updatedEntities.isNotEmpty()) {
            logger.echo("Modified entities:")
            updatedEntities.values
                    .flatten()
                    .forEach { logger.echo(" -  ${it.simpleName}") }

            logger.verbose("Entities dependency tree:")
            logEntitiesDependencyTree(entitiesTree, 0)

            logger.verbose("Dependency order:")
            entitiesPopulateOrder
                    .forEach { logger.verbose(" - ${it.entity.simpleName}") }
        }

        return updatedEntities.map { it.value.size }.sum() to entitiesPopulateOrder
    }

    private fun getEntityDependencyOrder(entities: Set<EntityDependency>): List<EntityDependency> {

        val orderList = mutableListOf<EntityDependency>()
        val flatSet = flattenedEntitiesSet(entities)
        var nextLevelOfEntities: List<EntityDependency>

        do {
            nextLevelOfEntities = flatSet
                    .filter { !orderList.contains(it) }
                    .filter { it.dependsOn.isEmpty() || orderList.containsAll(it.dependsOn) }
            orderList.addAll(nextLevelOfEntities)
        } while (nextLevelOfEntities.isNotEmpty())

        return orderList
    }

    private fun flattenedEntitiesSet(entities: Set<EntityDependency>): Set<EntityDependency> {

        val result = mutableSetOf<EntityDependency>()
        entities.forEach {
            result.add(it)
            result.addAll(flattenedEntitiesSet(it.dependsOn))
        }

        return result
    }

    private fun getEntityDependencies(app: R_App, moduleEntitiesMap: Map<R_Module, Set<R_EntityDefinition>>): Set<EntityDependency> {

        val entities: MutableSet<EntityDependency> = mutableSetOf()

        moduleEntitiesMap.forEach { (_, moduleEntities) ->

            moduleEntities.forEach {
                entities.add(getEntityDependencies(app, it))
            }
        }

        return entities
    }

    private fun getEntityDependencies(app: R_App, entity: R_EntityDefinition): EntityDependency {

        val dependsOn = entity.attributes
                .values
                .filter { attribute -> attribute.type is R_EntityType }
                .map { getEntityDependencies(app, (it.type as R_EntityType).rEntity) }
                .toSet()

        return EntityDependency(entity, dependsOn)
    }

    private fun getUpdatedEntities(oldApp: R_App, newApp: R_App): MutableMap<R_Module, Set<R_EntityDefinition>> {

        val newEntities = newApp.modules.flatMap { it.entities.values }

        val moduleEntities = mutableMapOf<R_Module, Set<R_EntityDefinition>>()
        for (oldModule in oldApp.modules) {

            val entities = oldModule.entities
                    .filter { (_, v) ->

                        val newEntity = newEntities.find { it.simpleName == v.simpleName }
                        newEntity != null && !isEntityEqual(v, newEntity)
                    }

            if (entities.isNotEmpty()) {
                moduleEntities[oldModule] = entities.values.toSet()
            }
        }

        return moduleEntities
    }

    private fun isEntityEqual(e1: R_EntityDefinition, e2: R_EntityDefinition): Boolean {

        if (e1.attributes.keys != e2.attributes.keys) {
            return false
        }

        for ((k, v) in e1.attributes.entries) {
            if (v.type.name != e2.attributes[k]!!.type.name) {
                return false
            }
        }

        return true
    }

    private fun logEntitiesDependencyTree(entities: Set<EntityDependency>, indent: Int) {

        entities.forEach {
            val indentStr = (0..<indent).joinToString("") { "  " }
            logger.verbose("$indentStr - ${it.entity.simpleName}")
            logEntitiesDependencyTree(it.dependsOn, indent + 1)
        }
    }
}

/**
 * Entity and it dependencies used bo build entity dependency tree.
 */
class EntityDependency(
        val entity: R_EntityDefinition,
        val dependsOn: Set<EntityDependency> = mutableSetOf()
) {
    override fun hashCode(): Int {
        return entity.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntityDependency

        return entity == other.entity
    }
}
