package com.chromia.cli.util

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory

fun withSigner(gtvConfig: Gtv, signer: ByteArray) =
        if (gtvConfig["signers"] != null) {
            gtvConfig
        } else {
            GtvFactory.gtv(
                    *gtvConfig.asDict().toList().toTypedArray(),
                    "signers" to GtvFactory.gtv(listOf(GtvFactory.gtv(signer)))
            )
        }
