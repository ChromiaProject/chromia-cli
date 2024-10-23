package com.chromia.api.impl

import com.chromia.api.impl.compile.addDefaultEntries
import com.chromia.api.result.BlockchainConfiguration
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
import net.postchain.web.WebStaticGTXModuleFactory
import org.apache.tika.Tika
import java.nio.file.Files
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText

const val BlockchainConfigurationMaxSize = 26 * 1024 * 1024 // 26 MiB

fun compileGtv(cliEnv: RellCliEnv, model: ChromiaModel): List<BlockchainConfiguration> {
    LibraryVerifyer(cliEnv, model.compile.libFolder).verifyLibs(model.libs)
    val (libraries, blockchains) = model.blockchains.toList().partition { it.second.type == BlockchainModel.Type.LIBRARY }
            .let { (libs, chains) -> libs.toMap() to chains.toMap() }

    val tika = Tika()

    return blockchains.map { (bc, m) -> blockchainGtv(cliEnv, tika, model.compile, bc, m) } +
            libraries.map { (lib, m) -> libraryGtv(cliEnv, model.compile, lib, m) }
}

private fun blockchainGtv(cliEnv: RellCliEnv, tika: Tika, compileModel: CompileModel, name: String, blockchainModel: BlockchainModel): BlockchainConfiguration {
    val gtv = GtvBuilder().apply {
        addDefaultEntries(blockchainModel, compileModel,
                extraModules = if (blockchainModel.webStatic != null) listOf(WebStaticGTXModuleFactory::class.qualifiedName!!) else listOf()
        )

        val config = RellApiCompile.Config.Builder()
                .cliEnv(cliEnv)
                .moduleArgs(blockchainModel.moduleArgs)
                .mountConflictError(true)
                .moduleArgsMissingError(true)
                .version(compileModel.langVersion)
                .quiet(compileModel.quiet)
                .build()

        val rellBcConfig = RellApiCompile.compileGtv(config, compileModel.source.toFile(), blockchainModel.module)
        update(rellBcConfig, "gtx", "rell")
        blockchainModel.webStatic?.let { dir ->
            val cacheTtlSeconds = blockchainModel.webCacheTtlSeconds ?: 3600
            update(gtv(mapOf("cache_ttl_seconds" to gtv(cacheTtlSeconds.toLong()))), "web_static")

            var totalSize: Long = 0

            val content = gtv(Files.walk(dir).filter { it.isRegularFile() && it.isReadable() }.map {
                val size = Files.size(it)
                if (size > BlockchainConfigurationMaxSize) throw IllegalArgumentException(
                        "Static web content file ${it.name} is too large: $size (only $BlockchainConfigurationMaxSize allowed)")
                totalSize += size
                val contentType = tika.detect(it.name)
                dir.relativize(it).invariantSeparatorsPathString to gtv(mapOf(
                        "content_type" to gtv(contentType),
                        "content" to if (contentType.startsWith("text/")) gtv(it.readText()) else gtv(it.readBytes())))
            }.toList().toMap())
            update(gtv(mapOf("content" to content)), "web_static")

            if (totalSize > BlockchainConfigurationMaxSize) throw IllegalArgumentException(
                    "Total size of static web content is too large: $totalSize (only $BlockchainConfigurationMaxSize allowed)")
        }
    }.build()
    return BlockchainConfiguration(name, gtv)
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
