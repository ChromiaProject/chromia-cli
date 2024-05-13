package com.chromia.build.tools.lib

import com.chromia.build.tools.compile.ValidationException
import com.chromia.build.tools.lib.DirectoryHashCalculator.RidStrategy
import com.chromia.cli.model.RellLibraryModel
import java.nio.file.Path
import kotlin.io.path.notExists
import net.postchain.rell.api.base.RellCliEnv

class LibraryVerifyer(private val env: RellCliEnv, private val libRoot: Path) {

    fun verifyLibs(libs: Map<String, RellLibraryModel>) {
        libs.forEach { (name, rellLibrary) ->
            val libraryLocation = libRoot.resolve(name)
            if (libraryLocation.notExists()) throw ValidationException("Library $name is not installed, install before building")
            if (!verifyLib(name, rellLibrary)) throw ValidationException("Failed validation of library $name")
        }
    }

    fun verifyLib(name: String, model: RellLibraryModel, quiet: Boolean = false): Boolean {
        if (model.insecure) return true
        val libDir = libRoot.resolve(name)
        val hashCalculator = DirectoryHashCalculator(libRoot.parent)
        val libraryRid = hashCalculator.compute(libDir, RidStrategy.LIST)
        if (model.rid == libraryRid) return true
        if (!quiet) {
            env.error("""
                The rid for library $name does not match the configured value.
                Should be: ${model.rid}
                Was: $libraryRid
                Do not blindly copy the calculated rid as the integrity of the library cannot be verified.
                """.trimIndent())
        }
        return false
    }
}
