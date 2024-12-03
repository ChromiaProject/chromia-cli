package com.chromia.build.tools.compile

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv

fun withSigner(gtvConfig: Gtv, vararg signer: ByteArray) =
        if (gtvConfig["signers"] != null) {
            gtvConfig
        } else {
            gtv(
                    *gtvConfig.asDict().toList().toTypedArray(),
                    "signers" to gtv(signer.map { gtv(it) })
            )
        }
