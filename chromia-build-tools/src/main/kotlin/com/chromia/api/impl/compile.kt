package com.chromia.api.impl

import com.chromia.api.result.BlockchainConfiguration
import com.chromia.build.tools.compile.BlockchainConfigurationGenerator
import com.chromia.build.tools.lib.DirectoryHashCalculator
import com.chromia.build.tools.lib.DirectoryHashCalculator.RidStrategy
import com.chromia.build.tools.lib.LibraryVerifyer
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.CompileModel
import com.chromia.cli.model.RellLibraryModel
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.builder.GtvBuilder
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.base.RellCliEnv
import net.postchain.rell.base.utils.RellGtxConfigConstants

fun compileGtv(cliEnv: RellCliEnv, model: ChromiaModel): List<BlockchainConfiguration> {
    LibraryVerifyer(cliEnv, model.compile.libFolder).verifyLibs(model.libs)
    val (libraries, blockchains) = model.blockchains.toList().partition { it.second.type == BlockchainModel.Type.LIBRARY }
            .let { (libs, chains) -> libs.toMap() to chains.toMap() }

    return BlockchainConfigurationGenerator(cliEnv, model.compile).generate(blockchains) +
            libraries.map { (lib, m) -> libraryGtv(cliEnv, model.compile, lib, m) }
}

private fun libraryGtv(cliEnv: RellCliEnv, compileModel: CompileModel, name: String, library: BlockchainModel): BlockchainConfiguration {
    val compileconfig = RellApiCompile.Config.Builder()
            .cliEnv(cliEnv)
            .includeTestSubModules(true)
            .mountConflictError(false)
            .moduleArgsMissingError(false)
            .appModuleInTestsError(false)
            .version(compileModel.langVersion)
            .quiet(false)
            .build()

    RellApiCompile.compileApp(compileconfig, compileModel.source.toFile(), listOf(library.module), library.test.modules)

    val libFolder = compileModel.libFolder.resolve(name)
    if (!libFolder.exists()) throw IllegalArgumentException("Library $name not found. Please verify that the name of the library matches the folder name in lib folder.")
    val rid = DirectoryHashCalculator(compileModel.source).compute(libFolder, RidStrategy.LIST)

    val gtv = GtvBuilder().apply {
        update(gtv(compileModel.rellVersion), "gtx", "rell", RellGtxConfigConstants.LANG_VERSION_KEY)
        update(gtv(rid), "rid")
    }.build()

    cliEnv.print(RellLibraryModel("", path = libFolder.relativeTo(compileModel.root).pathString, rid = rid).format(name))
    return BlockchainConfiguration(name, gtv)
}

fun verify(cliEnv: RellCliEnv, model: ChromiaModel): Boolean {
    LibraryVerifyer(cliEnv, model.compile.libFolder).verifyLibs(model.libs)

    val compileconfig = RellApiCompile.Config.Builder()
            .cliEnv(cliEnv)
            .mountConflictError(false)
            .moduleArgsMissingError(false)
            .version(model.compile.langVersion)
            .quiet(false)
            .build()

    return RellApiCompile.compileApp(compileconfig, model.compile.source.toFile(), null).valid
}
