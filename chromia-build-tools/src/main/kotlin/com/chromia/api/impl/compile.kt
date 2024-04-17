package com.chromia.api.impl

import com.chromia.api.result.BlockchainConfiguration
import com.chromia.build.tools.compile.BlockchainConfigurationGenerator
import com.chromia.build.tools.lib.LibraryVerifyer
import com.chromia.cli.model.ChromiaModel
import net.postchain.common.types.WrappedByteArray
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash
import net.postchain.rell.api.base.RellApiBaseInternal
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.base.RellCliEnv
import net.postchain.rell.base.compiler.base.utils.C_SourceDir
import net.postchain.rell.base.utils.RellGtxConfigConstants
import java.nio.file.Path

private val hashCalculator = GtvMerkleHashCalculator(Secp256K1CryptoSystem())

fun compileGtv(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path): List<BlockchainConfiguration> {
    val sourceDir = model.compile.sourceFile(projectDir.toFile()).toPath()
    LibraryVerifyer(cliEnv).verifyLibs(sourceDir, model.libs)
    val blockchainConfigurationGenerator = BlockchainConfigurationGenerator(cliEnv, model.compile, projectDir)
    return blockchainConfigurationGenerator.generate(model.blockchains)
}

fun verify(cliEnv: RellCliEnv, model: ChromiaModel, sourceDir: Path): WrappedByteArray {
    LibraryVerifyer(cliEnv).verifyLibs(sourceDir, model.libs)

    val compileconfig = RellApiCompile.Config.Builder()
            .cliEnv(cliEnv)
            .mountConflictError(false)
            .moduleArgsMissingError(false)
            .version(model.compile.langVersion)
            .quiet(false)
            .build()

    val gtv = RellApiBaseInternal.compileGtv(compileconfig, C_SourceDir.diskDir(sourceDir.toFile()), null)
    val gtvSources = GtvFactory.gtv(mapOf(RellGtxConfigConstants.SOURCES_KEY to gtv[RellGtxConfigConstants.SOURCES_KEY]!!)) // TODO: Verify this
    return WrappedByteArray(gtvSources.merkleHash(hashCalculator))
}
