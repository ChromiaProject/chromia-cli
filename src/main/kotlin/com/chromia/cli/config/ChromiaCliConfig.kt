package com.chromia.cli.config

import net.postchain.rell.model.R_LangVersion
import net.postchain.rell.runtime.Rt_RellVersion

data class ChromiaCliConfig(
        private val database: DatabaseConfig = DatabaseConfig(),
        private val compiler: CompilerConfig = CompilerConfig(),
        private val blockchain: BlockchainConfig? = null,
        private val blockchains: List<BlockchainConfig> = listOf()
) {
    init {
       // require(blockchain != null || blockchains.isNotEmpty()) { "You must have blockchain or blockchains arg" } Creates a bug with --help
        require(!(blockchain != null && blockchains.isNotEmpty())) { "You may not have both blockchain and blockchains arg at the same time" }
    }
    val compilerOptions get() = compiler.getCompilerOptions()
    val compilerQuiet get() = compiler.quiet
    val rellVersion get() = R_LangVersion.of(compiler.rellVersion)

    val databaseUrl get() = database.url
    val databaseErrorLogging get() = database.logSqlErrors

    val blockchainConfigs get() = blockchain?.let { listOf(it) } ?: blockchains
}
