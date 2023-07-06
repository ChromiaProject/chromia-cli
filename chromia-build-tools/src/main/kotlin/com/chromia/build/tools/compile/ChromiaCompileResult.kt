package com.chromia.build.tools.compile

import com.chromia.build.tools.compile.BlockchainConfigurationWriter.storeConfig
import java.io.File
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder

data class ChromiaCompileResult(val name: String, val config: Gtv) {
    val configByteArray get() = GtvEncoder.encodeGtv(config)
    fun save(target: File, fileName: String = name) = storeConfig(config, fileName, target.toPath())
}
