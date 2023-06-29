package com.chromia.cli.util

import net.postchain.client.core.PostchainClient
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.Name
import net.postchain.gtv.mapper.Nullable
import net.postchain.gtv.mapper.toObject

class BlockchainAnalyzer(private val client: PostchainClient) {


    fun getAppStructure(): Map<String, RellModule> {
        return client.query("rell.get_app_structure", gtv(mapOf())).toObject<DappModules>().modules
    }

}

data class DappModules(@Name("modules") val modules: Map<String, RellModule>)

data class RellModule(
        @Name("name") val name: String,
        @Name("queries") @Nullable val queries: Map<String, RellFunction>?,
        @Name("operations") @Nullable val operations: Map<String, RellFunction>?,
        @Name("objects") @Nullable val objects: Map<String, RellObject>?
) {
    fun isEmpty() = queries.isNullOrEmpty() && operations.isNullOrEmpty() && objects.isNullOrEmpty()
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

data class RellObject(
        @Name("mount") val mount: String,
        @Name("attributes") val attributes: Map<String, RellAttribute>
)

data class RellAttribute(
        @Name("type") val type: Gtv,
        @Name("mutable") val mutable: Long
)