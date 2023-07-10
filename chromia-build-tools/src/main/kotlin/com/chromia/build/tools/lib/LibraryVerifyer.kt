package com.chromia.build.tools.lib

import com.chromia.build.tools.compile.ValidationException
import com.chromia.cli.model.RellLibraryModel
import java.io.File
import java.nio.file.Files
import kotlin.io.path.notExists
import net.postchain.common.types.WrappedByteArray
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash
import net.postchain.rell.api.base.RellCliEnv
import net.postchain.rell.base.utils.RellGtxConfigConstants

class LibraryVerifyer(private val env: RellCliEnv) {
    val hashCalculator = GtvMerkleHashCalculator(Secp256K1CryptoSystem())

    fun verifyLibs(source: File, libs: Map<String, RellLibraryModel>) {
        libs.forEach { (name, rellLibrary) ->
            val libraryLocation = source.toPath().resolve(InstallDirTarget.SOURCE.target).resolve(name)
            if (libraryLocation.notExists()) throw ValidationException("Library $name is not installed, install before building")
            val files = Files.walk(libraryLocation).map { it.toFile() }.toList()
            if (!verifyLib(rellLibrary, name, files)) throw ValidationException("Failed validation of library $name")
        }
    }

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
                RellGtxConfigConstants.RELL_SOURCES_KEY to GtvFactory.gtv(files.filter { it.isFile }.sortedBy { it.path }.map { GtvFactory.gtv(it.readText()) })
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
