package com.chromia.cli.model

import com.chromia.cli.lib.LibraryNonSafeFilesException
import net.postchain.common.types.WrappedByteArray
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash
import net.postchain.rell.base.utils.RellGtxConfigConstants.RELL_SOURCES_KEY
import java.io.File

data class RellLibraryModel(
        val registry: String,
        val tagOrBranch: String? = null,
        val path: String,
        val insecure: Boolean = false,
        val rid: WrappedByteArray?,
) {
    fun isValid(libraryFiles: List<File>): Pair<Boolean, WrappedByteArray?> {

        if (insecure) {
            return true to null
        }

        //TODO show which file might be malicious
        if (libraryFiles.any { !it.isDirectory && it.extension != "rell" }) {
            throw LibraryNonSafeFilesException(path)
        }

        val calculator = GtvMerkleHashCalculator(Secp256K1CryptoSystem())
        val srcGtv = gtv(
                RELL_SOURCES_KEY to gtv(libraryFiles.filter { it.extension == "rell" }.map { gtv(it.readText()) })
        )

        val calcRid = WrappedByteArray(srcGtv.merkleHash(calculator))
        return (calcRid == this.rid) to calcRid
    }
}