package com.chromia.build.tools.restapi

import java.time.Clock
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi

object RestApiInstance {

    private var restApi: RestApi? = null
    val apiPort = 7745
    val apiUrl = "http://localhost:$apiPort"

    private fun getInstance(): RestApi {
        if (restApi == null) restApi = RestApi(apiPort, "", clock = Clock.systemUTC())
        return restApi!!
    }

    fun withModel(vararg model: Model, action: () -> Unit) {
        val restApi = getInstance()
        model.forEach { restApi.attachModel(it.blockchainRid, it) }
        action()
        model.forEach { restApi.detachModel(it.blockchainRid) }
    }
}
