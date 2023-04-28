package com.chromia.cli.model

import net.postchain.common.types.WrappedByteArray
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash
import net.postchain.rell.module.ConfigConstants
import java.io.File

data class RellLibraryModel(
        val registry: String,
        val lib: String,
        val verifyRid: Boolean = true,
        val rid: WrappedByteArray?,
) {
    fun validateRid(libraryFiles: List<File>): Boolean {

        if (!verifyRid) {
            return true
        }

        val calculator = GtvMerkleHashCalculator(Secp256K1CryptoSystem())
        val srcGtv = gtv(
                ConfigConstants.RELL_SOURCES_KEY to gtv(libraryFiles.filter { !it.isDirectory }.map { gtv(it.readText()) })
        )

        return WrappedByteArray(srcGtv.merkleHash(calculator)) == this.rid
    }
}