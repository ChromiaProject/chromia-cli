package com.chromia.cli.model

import com.chromia.cli.error.LibraryMismatchException
import com.chromia.cli.error.LibraryNonSafeFilesException
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
    fun verify(libraryFiles: List<File>, name: String) {

        if (insecure) {
            return
        }
        
        if (libraryFiles.any { !it.isDirectory && it.extension != "rell" }) {
            throw LibraryNonSafeFilesException(path, libraryFiles.filter { !it.isDirectory && it.extension != "rell" })
        }

        val calculator = GtvMerkleHashCalculator(Secp256K1CryptoSystem())
        val srcGtv = gtv(
                RELL_SOURCES_KEY to gtv(libraryFiles.filter { it.extension == "rell" }.map { gtv(it.readText()) })
        )

        val calcRid = WrappedByteArray(srcGtv.merkleHash(calculator))

        if (calcRid != this.rid) {
            throw LibraryMismatchException(rid?.toHex() ?: "", calcRid.toHex(), name)
        }
    }
}