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

data class DappModules(@Name("modules") val modules: Map<String, RellModule>)

data class RellModule(
        @Name("name") val name: String,
        @Name("queries") @Nullable val queries: Map<String, RellQuery>?,
        @Name("operations") @Nullable val operations: Map<String, RellOperation>?,
        @Name("entities") @Nullable val entities: Map<String, RellEntity>?,
        @Name("objects") @Nullable val objects: Map<String, RellObject>?,
        @Name("structs") @Nullable val structures: Map<String, RellStructure>?,
) {
    fun isEmpty() = queries.isNullOrEmpty() && operations.isNullOrEmpty() && entities.isNullOrEmpty() && objects.isNullOrEmpty()
}

data class RellQuery(
        @Name("mount") val mount: String,
        @Name("parameters") val parameters: List<RellParameter>,
        @Name("type") val returnType: Gtv,
)

data class RellOperation(
        @Name("mount") val mount: String,
        @Name("parameters") val parameters: List<RellParameter>,
)

data class RellParameter(
        @Name("name") val name: String,
        @Name("type") val type: Gtv,
)

data class RellEntity(
        @Name("mount") val mount: String,
        @Name("attributes") private val rellAttributes: Gtv,
) {
    val attributes
        get(): List<RellAttribute> = if (rellAttributes is GtvDictionary)
            rellAttributes.dict.mapValues { RellAttribute.fromLegacyGtv(it.key, it.value) }.values.toList()
        else
            (rellAttributes as GtvArray).array.map { RellAttribute.fromNewGtv(it) }
}

data class RellObject(
        @Name("mount") val mount: String,
        @Name("attributes") private val rellAttributes: Gtv,
) {
    val attributes
        get(): List<RellAttribute> = if (rellAttributes is GtvDictionary)
            rellAttributes.dict.mapValues { RellAttribute.fromLegacyGtv(it.key, it.value) }.values.toList()
        else
            (rellAttributes as GtvArray).array.map { RellAttribute.fromNewGtv(it) }
}

data class RellStructure(
        @Name("attributes") private val rellAttributes: Gtv
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