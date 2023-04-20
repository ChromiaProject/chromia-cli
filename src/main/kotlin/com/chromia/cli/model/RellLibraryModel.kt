package com.chromia.cli.model

import net.postchain.common.types.WrappedByteArray

data class RellLibraryModel(
        val registry: String,
        val lib: String,
        val verifyRid: Boolean = true,
        val rid: WrappedByteArray?,
)