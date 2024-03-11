package com.chromia.cli.model

import com.chromia.build.tools.model.ensureType
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
        fun load(data: Map<String, Any>, additionalProperty: String) = RellLibraryModel(
                registry = ensureType<String>(data["registry"], "libs", additionalProperty, "registry"),
                tagOrBranch = ensureType<String?>(data["tagOrBranch"], "libs", additionalProperty, "tagOrBranch"),
                path = ensureType<String>(data["path"], "libs", additionalProperty, "path"),
                insecure = ensureType<Boolean?>(data["insecure"], "libs", additionalProperty, "insecure")
                        ?: false,
                rid = ensureType<ByteArray?>(data["rid"], "libs", additionalProperty, "rid")?.wrap()
        )
    }
}
