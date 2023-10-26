package com.chromia.cli.util

import net.postchain.client.core.PostchainQuery
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.Name
import net.postchain.gtv.mapper.Nullable
import net.postchain.gtv.mapper.toObject

class BlockchainAnalyzer(private val client: PostchainQuery) {


    fun getAppStructure(): Map<String, RellModule> {
        return client.query("rell.get_app_structure", gtv(mapOf())).toObject<DappModules>().modules
    }

}

data class DappModules(@Name("modules") val modules: Map<String, RellModule>)

data class RellModule(
        @Name("name") val name: String,
        @Name("queries") @Nullable val queries: Map<String, RellFunction>?,
        @Name("operations") @Nullable val operations: Map<String, RellFunction>?,
        @Name("structs") @Nullable val structures: Map<String, RellStructure>?
) {
    fun isEmpty() = queries.isNullOrEmpty() && operations.isNullOrEmpty() && structures.isNullOrEmpty()
}

data class RellFunction(
        @Name("mount") val mount: String,
        @Name("parameters") val parameters: List<RellParameter>,
        @Name("type") @Nullable val returnType: Gtv?
)

data class RellParameter(
        @Name("name") val name: String,
        @Name("type") val type: Gtv
)

data class RellStructure(
        @Name("attributes") private val rellAttributes: Gtv
) {
    val isLegacy = rellAttributes is GtvDictionary

    val legacyAttributes get() = (rellAttributes as GtvDictionary).dict.mapValues {
        val dict = it.value as GtvDictionary
        RellAttribute(it.key, dict["type"]!!, dict["mutable"]!!.asBoolean())
    }

    val attributes get() = (rellAttributes as GtvArray).array.map {
        val dict = it as GtvDictionary
        RellAttribute(dict["name"]!!.asString(), dict["type"]!!, dict["mutable"]!!.asBoolean())
    }
}

data class RellAttribute(
        val name: String,
        @Name("type") val type: Gtv,
        @Name("mutable") val mutable: Boolean
)