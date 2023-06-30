package com.chromia.cli.model

import net.postchain.common.types.WrappedByteArray

data class RellLibraryModel(
        val registry: String,
        val tagOrBranch: String? = null,
        val path: String,
        val insecure: Boolean = false,
        val rid: WrappedByteArray?,
)
