package com.chromia.cli.tools.config

import java.io.File
import java.io.FileWriter
import net.postchain.common.BlockchainRid
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters

class ChromiaConfigWriter private constructor(val configFile: File) {

    companion object {
        val local = ChromiaConfigWriter(Level.LOCAL.file)
        val global = ChromiaConfigWriter(Level.GLOBAL.file)
        fun custom(file: File) = ChromiaConfigWriter(file)
    }

    enum class Level(val file: File) {
        LOCAL(ChromiaConfigLoader.localConfigurationFile()),
        GLOBAL(ChromiaConfigLoader.globalConfigurationFile()),
    }

    fun setBrid(blockchainRid: BlockchainRid) = setProperty("brid" to blockchainRid.toHex())

    fun setProperty(vararg property: Pair<String, Any>) = setProperty(property.toMap())

    fun setProperty(properties: Map<String, Any>) {
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
        properties.forEach { (k, v) -> configuration.setProperty(k, v) }
        configuration.write(FileWriter(configFile.absoluteFile))
    }
}