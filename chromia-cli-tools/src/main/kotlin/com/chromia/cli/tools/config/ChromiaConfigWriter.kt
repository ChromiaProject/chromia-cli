package com.chromia.cli.tools.config

import java.io.File
import java.io.FileWriter
import net.postchain.common.BlockchainRid
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters

class ChromiaConfigWriter(val level: Level) {
    val configFile get() = level.file

    enum class Level(val file: File) {
        LOCAL(ChromiaConfigLoader.localConfigurationFile()),
        GLOBAL(ChromiaConfigLoader.globalConfigurationFile())
    }

    fun setBrid(blockchainRid: BlockchainRid) = setProperty("brid" to blockchainRid.toHex())

    fun setProperty(vararg property: Pair<String, Any>) {
        val configuration = if (configFile.exists()) {
            Parameters().properties()
                    .setFile(configFile)
                    .let {
                        FileBasedConfigurationBuilder(PropertiesConfiguration::class.java)
                                .configure(it)
                                .configuration
                    }
        } else {
            configFile.parentFile?.mkdirs()
            PropertiesConfiguration()
        }
        property.forEach { (k, v) -> configuration.setProperty(k, v) }
        configuration.write(FileWriter(configFile.absoluteFile))
    }
}