package com.chromia.cli.ft

import com.chromia.directory1.lib.ft4.core.accounts.AuthType
import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import net.postchain.gtv.Gtv

fun Ft4GetAccountAuthDescriptorsBySignerResult.getFlags() = this.args.asArray().first().asArray().map { it.asString() }
fun Ft4GetAccountAuthDescriptorsBySignerResult.getNumberOfSigners(): Long {
    return if (this.authType == AuthType.S) {
        1
    } else {
        this.args.asArray()[1].asInteger()
    }
}

fun Ft4GetAccountAuthDescriptorsBySignerResult.getKeysAsFormattedString(): String {
    return if (this.authType == AuthType.S) {
        this.args.asArray()[1].toString()
    } else {
        this.args.asArray()[2].asArray().joinToString("\n")
    }
}


fun Ft4GetAccountAuthDescriptorsBySignerResult.getSingleKey(): ByteArray {
    //Second argument in gtv of AuthType.S has the public key
    return if (this.authType == AuthType.S) {
        this.args.asArray()[1].asByteArray()
    } else {
        throw IllegalArgumentException("can only be done one Auth descriptors of type S")
    }
}

fun Ft4GetAccountAuthDescriptorsBySignerResult.getMultiKeys(): Array<out Gtv> {
    //Third argument in gtv of AuthType.M has the public key
    return if (this.authType == AuthType.M) {
        this.args.asArray()[2].asArray()
    } else {
        throw IllegalArgumentException("can only be done one Auth descriptors of type M")
    }
}