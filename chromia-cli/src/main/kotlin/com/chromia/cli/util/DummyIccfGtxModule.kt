package com.chromia.cli.util

import com.chromia.build.tools.iccf.AbstractTestIccfGtxModule
import net.postchain.core.TxEContext
import net.postchain.gtx.GTXOperation

class DummyIccfGtxModule: AbstractTestIccfGtxModule<DummyIccfGtxModule.Conf>(
        conf = Conf(),
        {_,data -> object : GTXOperation(data) {
            override fun checkCorrectness() { }
            override fun apply(ctx: TxEContext)= true
        } }
) {
    class Conf
}
