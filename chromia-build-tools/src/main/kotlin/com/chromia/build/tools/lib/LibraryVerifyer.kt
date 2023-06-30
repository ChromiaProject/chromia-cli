package com.chromia.build.tools.lib

import com.chromia.cli.model.RellLibraryModel
import net.postchain.common.types.WrappedByteArray
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash
import net.postchain.rell.api.base.RellCliEnv
import net.postchain.rell.base.utils.RellGtxConfigConstants
import java.io.File

class LibraryVerifyer(private val env: RellCliEnv) {
    val hashCalculator = GtvMerkleHashCalculator(Secp256K1CryptoSystem())

    fun verifyLib(model: RellLibraryModel, name: String, files: List<File>, quiet: Boolean = false): Boolean {
        if (model.insecure) return true

        if (files.any { !it.isDirectory && it.extension != "rell" }) {
            if (!quiet) {
                env.error("Library $name contains files that are not rell files.")
                env.error("Affected files: ${files.filter { !it.isDirectory && it.extension != "rell" }}")
            }
            return false
        }

        val srcGtv = GtvFactory.gtv(
                RellGtxConfigConstants.RELL_SOURCES_KEY to GtvFactory.gtv(files.filter { it.extension == "rell" }.map { GtvFactory.gtv(it.readText()) })
        )

        val calculatedRid = WrappedByteArray(srcGtv.merkleHash(hashCalculator))
        if (calculatedRid != model.rid) {
            if (!quiet) {
                env.error("""
                The rid for library $name does not match the configured value.
                Should be: ${model.rid}
                Was: $calculatedRid
                Do not blindly copy the calculated rid as the integrity of the library cannot be verified.
                """.trimIndent())
            }
        }
        return calculatedRid == model.rid
    }
}
