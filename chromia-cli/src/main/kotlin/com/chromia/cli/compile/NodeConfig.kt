package com.chromia.cli.compile

import com.chromia.cli.model.ChromiaModel
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.config.app.AppConfig
import net.postchain.crypto.secp256k1_derivePubKey
import org.apache.commons.configuration2.BaseConfiguration
import java.io.File

object NodeConfig {
     fun getNodeConfig(nodeConfigFile: File): AppConfig {
         return AppConfig.fromPropertiesFile(File(nodeConfigFile.absolutePath))
     }

     fun getDefaultNodeConfig(config: ChromiaModel, overrides: Map<String, Any> = mapOf()): AppConfig {
         val privKey = "42".repeat(32).hexStringToByteArray()
         val pubKey = secp256k1_derivePubKey(privKey)

         val inMemoryConfig = BaseConfiguration().apply {
             setProperty("api.port", 7740)
             setProperty("messaging.privkey", privKey.toHex())
             setProperty("messaging.pubkey", pubKey.toHex())
             setProperty("messaging.port", 9870)
             setProperty("database.driverclass", config.databaseDriver)
             setProperty("database.url", config.databaseUrl)
             setProperty("database.schema", config.databaseSchema)
             setProperty("database.username", config.databaseUser)
             setProperty("database.password", config.databasePassword)
             setProperty("configuration.provider.node", "manual")
             setProperty("fastsync.exit_delay", 0)
             overrides.forEach { setProperty(it.key, it.value) }
         }
         return AppConfig(inMemoryConfig)
     }
 }