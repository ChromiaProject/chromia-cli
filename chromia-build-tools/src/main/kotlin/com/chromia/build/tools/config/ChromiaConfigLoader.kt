package com.chromia.build.tools.config

import java.io.File
import net.postchain.common.PropertiesFileLoader
import net.postchain.rell.api.base.RellCliEnv
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration


class ChromiaConfigLoader(private val cliEnv: RellCliEnv) {

    companion object {
        private const val DEFAULT_CONFIG_FILENAME = ".chromia/config"
        private const val DEFAULT_PMC_CONFIG_FILENAME = ".pmc/config"
        private const val DEFAULT_CHROMIA_MODEL_FILENAME = "chromia.yml"
        private const val DEFAULT_CONFIG_MODEL_FILENAME = "config.yml"
        fun globalConfigurationFile() = File("${System.getProperty("user.home")}/${DEFAULT_CONFIG_FILENAME}")
        fun localConfigurationFile() = File(DEFAULT_CONFIG_FILENAME)
    }

    fun loadClientConfigFile(file: File? = null): ChromiaClientConfig {
        val config = PropertiesConfiguration()
        config.setProperty("status.poll-interval", 2000)
        loadFromFileIfExists(globalConfigurationFile(), config)
        if (!localConfigurationFile().exists() && File(DEFAULT_PMC_CONFIG_FILENAME).exists()) {
            cliEnv.print("Loading .pmc/config file. Rename to .chromia/config to silence this message")
            loadFromFileIfExists(File(DEFAULT_PMC_CONFIG_FILENAME), config) // Backwards compatibility
        }

        loadFromFileIfExists(localConfigurationFile(), config)
        loadFromFileIfExists(file, config)
        return ChromiaClientConfig.from(config)
    }

    private fun loadFromFileIfExists(file: File?, config: Configuration) {
        if (file != null && file.exists()) {
            val c = PropertiesFileLoader.load(file.absolutePath)
            c.keys.forEach { key -> config.setProperty(key, c.getProperty(key)) }
        }
    }

    fun findModelFile(explicitFile: File?): File? {
        if (explicitFile != null) {
            require(explicitFile.isFile) { "File $explicitFile is not a regular file" }
            return explicitFile.absoluteFile
        }
        val chromiaModelFile = File(DEFAULT_CHROMIA_MODEL_FILENAME)
        if (chromiaModelFile.exists() && chromiaModelFile.isFile) {
            return chromiaModelFile.absoluteFile
        }

        val configModelFile = File(DEFAULT_CONFIG_MODEL_FILENAME)
        if (configModelFile.exists() && configModelFile.isFile) {
            cliEnv.print("Found config.yml settings file. Rename to chromia.yml to silence this message")
            return configModelFile.absoluteFile
        }
        return null
    }
}