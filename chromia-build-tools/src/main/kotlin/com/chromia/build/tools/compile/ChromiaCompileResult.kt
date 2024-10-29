package com.chromia.build.tools.compile

import com.chromia.build.tools.compile.BlockchainConfigurationWriter.storeConfig
import java.io.File
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder

@Deprecated("Use the new api instead", replaceWith = ReplaceWith("BlockchainConfiguration"))
data class ChromiaCompileResult(val name: String, val config: Gtv) {
    val configByteArray get() = GtvEncoder.encodeGtv(config)
    @Deprecated("Use the new api instead",
            ReplaceWith("BlockchainConfiguration.save(target.toPath(), fileName)", "com.chromia.api.result.BlockchainConfiguration"))
    fun save(target: File, fileName: String = name) = storeConfig(config, fileName, target.toPath())
}
