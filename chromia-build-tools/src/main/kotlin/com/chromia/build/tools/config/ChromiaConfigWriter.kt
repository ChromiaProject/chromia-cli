package com.chromia.build.tools.config

import java.io.File
import java.io.FileWriter
import net.postchain.common.BlockchainRid
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters

class ChromiaConfigWriter private constructor(val configFile: File) {

    companion object {
        val local = ChromiaConfigWriter(ChromiaConfigLoader.localConfigurationFile())
        val global = ChromiaConfigWriter(ChromiaConfigLoader.globalConfigurationFile())
        fun custom(file: File) = ChromiaConfigWriter(file)
    }

    fun setBrid(blockchainRid: BlockchainRid) = setProperty("brid" to blockchainRid.toHex())

    fun setKeyId(keyId: String) = setProperty("key.id" to keyId)

    fun setProperty(vararg property: Pair<String, Any>) = setProperty(property.toMap())

    fun setProperty(properties: Map<String, Any>) {
        val configuration = if (configFile.exists()) {
            Parameters().properties()
                    .setFile(configFile)
                    .let { parameters ->
                        FileBasedConfigurationBuilder(PropertiesConfiguration::class.java)
                                .configure(parameters)
                                .configuration
                    }
        } else {
            configFile.parentFile?.mkdirs()
            PropertiesConfiguration()
        }
        properties.forEach { (k, v) -> configuration.setProperty(k, v) }
        FileWriter(configFile.absoluteFile).use { configuration.write(it) }
    }
}
