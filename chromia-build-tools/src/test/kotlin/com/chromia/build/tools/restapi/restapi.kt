package com.chromia.build.tools.restapi

import net.postchain.common.exception.UserMistake
import net.postchain.d1.cluster.ClusterManagement
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import net.postchain.gtx.GtxQuery
import net.postchain.rell.base.model.R_LangVersion


fun CachedModel.withClusterManagement(clusterManagement: ClusterManagement) = ClusterManagementModel(this, clusterManagement)
fun CachedModel.withCompression() = CompressableModel(this)

fun CachedModel.withValidConfiguration() = object : CachedModel by this {
    override fun validateBlockchainConfiguration(configuration: Gtv) {}
}

fun CachedModel.withInvalidConfiguration() = object : CachedModel by this {
    override fun validateBlockchainConfiguration(configuration: Gtv) {
        throw UserMistake("Invalid configuration")
    }
}

fun CachedModel.withRellVersion(version: String) = withRellVersion(R_LangVersion.of(version))
fun CachedModel.withRellVersion(version: R_LangVersion) = withQuery("rell.get_rell_version", GtvFactory.gtv(version.str()))

fun CachedModel.withQuery(name: String, response: Gtv) = withQuery(name) { response }

fun CachedModel.withQuery(name: String, response: (GtxQuery) -> Gtv) = object : CachedModel by this {
    override fun query(query: GtxQuery) = when (query.name) {
        name -> response(query)
        else -> this@withQuery.query(query)
    }
}
