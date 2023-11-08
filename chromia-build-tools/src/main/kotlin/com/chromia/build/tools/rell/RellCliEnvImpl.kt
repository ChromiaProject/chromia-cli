package com.chromia.build.tools.rell

import net.postchain.rell.api.base.RellCliEnv

class RellCliEnvImpl(private val printer: (String) -> Unit, private val errorPrinter: (String) -> Unit = printer): RellCliEnv {
    override fun error(msg: String) = errorPrinter(msg)
    override fun print(msg: String) = printer(msg)
}
