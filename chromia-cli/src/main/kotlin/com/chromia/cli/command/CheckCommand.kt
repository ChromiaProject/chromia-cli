package com.chromia.cli.command

import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.util.BuildCliEnv
import com.chromia.cli.util.addWhitelistedGTXModules
import com.chromia.cli.util.hideLibWarningsOption
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import net.postchain.gtv.Gtv
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.base.RellCliException
import net.postchain.rell.api.gtx.RellApiRunTests

class CheckCommand : ChromiaCommand(help = "Check Rell code for compilation errors") {

    private val settings by chromiaModelOption()

    private val sourceDir by lazy { settings.sourceDir }

    private val hideLibWarnings by hideLibWarningsOption()

    override fun run() {
        try {
            settings.model.blockchains
                    .forEach { bc ->
                        if (bc.value.test.modules.isNotEmpty()) {
                            runTestsForBlockchain(bc.key, bc.value, bc.value.test.modules)
                        }
                    }
            if (settings.model.test.modules.isNotEmpty()) {
                runUnitTests(settings.model.test.modules)
            }
        } catch (e: RellCliException) {
            throw CliktError(e.message)
        }
    }

    private fun runTestsForBlockchain(blockchain: String, chainConfig: BlockchainModel, testModules: List<String>) {
        val appModules = listOf(chainConfig.module
                ?: throw CliktError("Cannot check tests for blockchain $blockchain without main module"))

        val additionalModules = chainConfig.config["gtx"]?.get("modules")?.asArray()?.map { it.asString() }

        val testModuleArgs = mergeModuleArgs(chainConfig.moduleArgs, chainConfig.test.moduleArgs)
        val testConf = createTestConfig(testModuleArgs, additionalModules, appModuleInTestsError = true)

        RellApiRunTests.runTests(testConf, sourceDir, appModules, testModules)
    }

    private fun runUnitTests(testModules: List<String>) {
        val testModuleArgs = settings.model.test.moduleArgs
        val testConf = createTestConfig(testModuleArgs)

        RellApiRunTests.runTests(testConf, sourceDir, listOf(), testModules)
    }

    private fun createTestConfig(testModuleArgs: Map<String, Map<String, Gtv>>, additionalGtxModules: List<String>? = null,
                                 appModuleInTestsError: Boolean = false): RellApiRunTests.Config {
        val compileConf = RellApiCompile.Config.Builder()
                .moduleArgs(testModuleArgs)
                .cliEnv(BuildCliEnv(this, hideLibWarnings))
                .includeTestSubModules(true)
                .appModuleInTestsError(appModuleInTestsError)
                .addWhitelistedGTXModules(additionalGtxModules)
                .moduleArgsMissingError(true)
                .mountConflictError(true)
                .version(settings.model.compile.langVersion)
                .quiet(settings.model.compile.quiet)
                .build()
        return RellApiRunTests.Config.Builder()
                .compileConfig(compileConf)
                .testPatterns(listOf("@")) // a pattern which cannot match any test
                .stopOnError(false)
                .logPrinter(::echo)
                .outPrinter(::echo)
                .printTestCases(false)
                .sqlLog(false)
                .build()
    }

    private fun mergeModuleArgs(first: Map<String, Map<String, Gtv>>,
                                second: Map<String, Map<String, Gtv>>): Map<String, Map<String, Gtv>> {
        return (first.asSequence() + second.asSequence())
                .groupBy({ it.key }, { it.value })
                .mapValues { (_, values) ->
                    values.flatMap { map -> map.entries }.associate(Map.Entry<String, Gtv>::toPair)
                }
    }
}
