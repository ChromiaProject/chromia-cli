package com.chromia.cli.model

import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap

data class RellLibraryModel(
        val registry: String,
        val tagOrBranch: String? = null,
        val path: String,
        val insecure: Boolean = false,
        val rid: WrappedByteArray?,
) {
    companion object {
        fun load(data: Map<String, Any>) = RellLibraryModel(
                registry = data["registry"] as String,
                tagOrBranch = data["tagOrBranch"] as String?,
                path = data["path"] as String,
                insecure = data["insecure"]?.let { it as Boolean } ?: false,
                rid = data["rid"]?.let { (it as ByteArray).wrap() }
        )
    }
}
