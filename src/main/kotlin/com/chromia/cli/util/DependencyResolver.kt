package com.chromia.cli.util

import com.chromia.cli.model.RellLibraryModel
import net.postchain.common.types.WrappedByteArray

interface DependencyResolver {
    fun checkHash(rellLibrary: RellLibraryModel): Boolean
    fun getLib(name: String, rellLibrary: RellLibraryModel): RegisteredLib
    fun getDependencies(settings: Settings): Map<String, RellLibraryModel> {
        return settings.libs
    }
}

class BaseDependencyResolver : DependencyResolver {

    //TODO connect to dapp to get the registeredLib value
    override fun getLib(name: String, rellLibrary: RellLibraryModel): RegisteredLib {
        //TODO get from the request so it is standardized
        return RegisteredLib("", WrappedByteArray.fromHex("11"))
    }

    //TODO implement hash checker
    override fun checkHash(rellLibrary: RellLibraryModel): Boolean {
        if (!rellLibrary.verifyRid) {
            return true
        }
        
        return true
    }
}

data class RegisteredLib(val name: String, val rid: WrappedByteArray)