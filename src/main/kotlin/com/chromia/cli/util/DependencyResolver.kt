package com.chromia.cli.util

import com.chromia.cli.model.InstallAttribute
import net.postchain.common.types.WrappedByteArray

interface DependencyResolver {
    fun checkHash(rid: WrappedByteArray): Boolean
    fun getLib(registry: String): RegisteredLib
    fun getDependencies(settings: Settings): List<InstallAttribute> {
        return settings.blockchains.values.flatMap { it.libs.values }.distinct()
    }
}

class BaseDependencyResolver : DependencyResolver {

    //TODO connect to dapp to get the registeredLib value
    override fun getLib(registry: String): RegisteredLib {
        //TODO get from the request so it is standardized
        return RegisteredLib("", WrappedByteArray.fromHex("11"))
    }

    //TODO implement hash checker
    override fun checkHash(rid: WrappedByteArray): Boolean {
        return true
    }
}

data class RegisteredLib(val name: String, val rid: WrappedByteArray)