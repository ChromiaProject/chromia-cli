package com.chromia.build.tools.rell

import net.postchain.rell.api.base.RellCliEnv

class CachedRellCliEnv(
        private val printer: (String) -> Unit,
        private val errorPrinter: (String) -> Unit = printer,
        private val cacheOutput: Boolean = false,
        private val cacheError: Boolean = false
) : RellCliEnv() {
    val errorCache = mutableListOf<String>()
    val outputCache = mutableListOf<String>()
    override fun error(msg: String) = errorPrinter(msg).also { if (cacheError) errorCache.add(msg) }
    override fun print(msg: String) = printer(msg).also { if (cacheOutput) outputCache.add(msg) }
}
