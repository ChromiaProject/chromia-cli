package com.chromia.api.result

import com.chromia.build.tools.compile.BlockchainConfigurationWriter.storeConfig
import com.chromia.build.tools.compile.ValidationException
import com.chromia.build.tools.compile.withSigner
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.common.hexStringToByteArray
import java.io.File
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import java.nio.file.Path

data class BlockchainConfiguration(override val name: String, val config: Gtv): Named<Gtv> {
    override val value: Gtv
        get() = config
    val configByteArray get() = GtvEncoder.encodeGtv(config)

    fun withSigners(vararg signer: ByteArray) = BlockchainConfiguration(name, withSigner(config, *signer))

    fun save(target: Path, fileName: String = name) = storeConfig(config, fileName, target)

    fun validate() {
        try {
            GTXBlockchainConfigurationFactory.validateConfiguration(
                    withSigner(config, "000000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray()),
                    BlockchainRid.ZERO_RID // dummy blockchain RID, works with Rell and all standard GTX modules, might not work properly with custom GTX modules
            )
        } catch (e: UserMistake) {
            throw ValidationException(e.message!!)
        } catch (e: ClassNotFoundException) {
            throw ValidationException("Could not find Gtx module: ${e.message!!}")
        }
    }
}

interface Named<T> {
    val name: String
    val value: T
}
//data class Named<T>(val name: String, val value: T)
