package com.chromia.cli.compile

import com.chromia.cli.model.ChromiaCliModel
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.config.app.AppConfig
import net.postchain.crypto.secp256k1_derivePubKey
import net.postchain.rell.utils.PostchainUtils.cryptoSystem
import org.apache.commons.configuration2.BaseConfiguration
import java.io.File

object NodeConfig {
     fun getNodeConfig(nodeConfigFile: File): AppConfig {
         return AppConfig.fromPropertiesFile(nodeConfigFile.absolutePath)
     }

     fun getDefaultNodeConfig(config: ChromiaCliModel): AppConfig {
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
             setProperty("database.username", config.database.username)
             setProperty("database.password", config.database.password)
             setProperty("configuration.provider.node", "manual")
             setProperty("fastsync.exit_delay", 0)
         }
         return AppConfig(inMemoryConfig, true)
     }
 }