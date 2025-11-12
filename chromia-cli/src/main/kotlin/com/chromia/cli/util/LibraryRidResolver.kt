package com.chromia.cli.util

import com.chromia.build.tools.lib.createLibraryChainClient
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.RellLibraryModel
import com.chromia.library.chain.versioning.external.getLibrary
import com.chromia.library.chain.versioning.external.getLibraryRid
import com.github.ajalt.clikt.core.PrintMessage
import net.postchain.common.types.WrappedByteArray

object LibraryRidResolver {

    fun resolveLibraryRids(model: ChromiaModel): ChromiaModel {
        val libs = model.libs.map {
            if (isLibraryChainLib(it.value)) {
                val client = createLibraryChainClient(it.value.registry, it.value.brid)
                val rid = client.getLibraryRid(it.key, it.value.version!!)
                    ?: throw PrintMessage("Unable to get rid for ${it.key}")
                val name = client.getLibrary(it.key)?.displayName
                    ?: throw PrintMessage("Library ${it.key} not found")
                name to it.value.copy(rid = WrappedByteArray(rid))
            } else {
                it.key to it.value
            }
        }
        return model.copy(libs = libs.toMap())
    }

    private fun isLibraryChainLib(libModel: RellLibraryModel) = libModel.version != null
}
