package com.chromia.cli.compile

import com.chromia.build.tools.compile.ChromiaCompileApi
import com.chromia.build.tools.compile.ChromiaCompileResult
import com.chromia.cli.model.ChromiaModel
import java.io.File
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.rell.api.base.RellCliEnv

class ConfigExtractor(val model: ChromiaModel, val cliEnv: RellCliEnv) {

    fun extractConfigs(projectFolder: File, blockchainConfigs: List<File>, name: List<String>): Collection<ChromiaCompileResult> {
        val configsToAdd = if (blockchainConfigs.isEmpty()) {
            val blockchainsToCompile = model.blockchains.filter { name.isEmpty() || name.contains(it.key) }.keys
            ChromiaCompileApi.compile(cliEnv, model, projectFolder, blockchainsToCompile)
        } else {
            blockchainConfigs
                    .filter { name.isEmpty() || name.contains(it.nameWithoutExtension) }
                    .associate {
                        //TODO remove GTV is no longer supported
                        if (it.extension == "gtv") {
                            it.inputStream().use { inputStream ->
                                it.nameWithoutExtension to GtvDecoder.decodeGtv(inputStream)
                            }
                        } else {
                            it.nameWithoutExtension to GtvMLParser.parseGtvML(it.readText())
                        }
                    }
                    .map { ChromiaCompileResult(it.key, it.value) }

        }.sortedBy { it.name }

        return configsToAdd
    }
}