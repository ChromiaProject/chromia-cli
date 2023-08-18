package com.chromia.build.tools

import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi

object RestApiInstance {

    private var restApi: RestApi? = null
    val apiPort = 7745
    val apiUrl = "http://localhost:$apiPort"

    private fun getInstance(): RestApi {
        if (restApi == null) restApi = RestApi(apiPort, "")
        return restApi!!
    }

    fun withModel(vararg model: Model, action: () -> Unit) {
        model.forEach { getInstance().attachModel(it.blockchainRid, it) }
        action()
        model.forEach { getInstance().detachModel(it.blockchainRid) }
    }
}
