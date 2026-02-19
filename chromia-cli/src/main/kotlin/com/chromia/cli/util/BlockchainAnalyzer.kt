package com.chromia.cli.util

import net.postchain.client.core.PostchainQuery
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.Name
import net.postchain.gtv.mapper.Nullable
import net.postchain.gtv.mapper.toObject

fun interface BlockchainAnalyzer {
    fun getAppStructure(): Map<String, RellModule>
}

class RellBlockchainAnalyzer(private val client: PostchainQuery) : BlockchainAnalyzer {
    override fun getAppStructure(): Map<String, RellModule> =
            client.query("rell.get_app_structure", gtv(mapOf())).toObject<DappModules>().modules
}

data class DappModules(@param:Name("modules") val modules: Map<String, RellModule>)

data class RellModule(
        @param:Name("name") val name: String,
        @param:Name("queries") @param:Nullable val queries: Map<String, RellQuery>?,
        @param:Name("operations") @param:Nullable val operations: Map<String, RellOperation>?,
        @param:Name("entities") @param:Nullable val entities: Map<String, RellEntity>?,
        @param:Name("objects") @param:Nullable val objects: Map<String, RellObject>?,
        @param:Name("structs") @param:Nullable val structures: Map<String, RellStructure>?,
) {
    fun isEmpty() = queries.isNullOrEmpty() && operations.isNullOrEmpty() && entities.isNullOrEmpty() && objects.isNullOrEmpty()
}

data class RellQuery(
        @param:Name("mount") val mount: String,
        @param:Name("parameters") val parameters: List<RellParameter>,
        @param:Name("type") val returnType: Gtv,
)

data class RellOperation(
        @param:Name("mount") val mount: String,
        @param:Name("parameters") val parameters: List<RellParameter>,
)

data class RellParameter(
        @param:Name("name") val name: String,
        @param:Name("type") val type: Gtv,
)

data class RellEntity(
        @param:Name("mount") val mount: String,
        @param:Name("attributes") private val rellAttributes: Gtv,
) {
    val attributes
        get(): List<RellAttribute> = if (rellAttributes is GtvDictionary)
            rellAttributes.dict.mapValues { RellAttribute.fromLegacyGtv(it.key, it.value) }.values.toList()
        else
            (rellAttributes as GtvArray).array.map { RellAttribute.fromNewGtv(it) }
}

data class RellObject(
        @param:Name("mount") val mount: String,
        @param:Name("attributes") private val rellAttributes: Gtv,
) {
    val attributes
        get(): List<RellAttribute> = if (rellAttributes is GtvDictionary)
            rellAttributes.dict.mapValues { RellAttribute.fromLegacyGtv(it.key, it.value) }.values.toList()
        else
            (rellAttributes as GtvArray).array.map { RellAttribute.fromNewGtv(it) }
}

data class RellStructure(
        @param:Name("attributes") private val rellAttributes: Gtv
) {
    val attributes
        get(): List<RellAttribute> = if (rellAttributes is GtvDictionary)
            rellAttributes.dict.mapValues { RellAttribute.fromLegacyGtv(it.key, it.value) }.values.toList()
        else
            (rellAttributes as GtvArray).array.map { RellAttribute.fromNewGtv(it) }
}

data class RellAttribute(
        val name: String,
        val type: Gtv,
        val mutable: Boolean,
) {
    companion object {
        fun fromNewGtv(gtv: Gtv): RellAttribute {
            val dict = gtv as GtvDictionary
            return RellAttribute(dict["name"]!!.asString(), dict["type"]!!, dict["mutable"]!!.asBoolean())
        }

        fun fromLegacyGtv(name: String, gtv: Gtv): RellAttribute {
            val dict = gtv as GtvDictionary
            return RellAttribute(name, dict["type"]!!, dict["mutable"]!!.asBoolean())
        }
    }
}