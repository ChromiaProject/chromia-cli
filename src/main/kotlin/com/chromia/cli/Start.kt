package com.chromia.cli

import com.chromia.cli.util.configFile
import com.chromia.cli.util.sourceDirOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import mu.KotlinLogging
import mu.withLoggingContext
import net.postchain.PostchainNode
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.gtv.GtvToBlockchainRidFactory
import net.postchain.base.withReadWriteConnection
import net.postchain.common.exception.UserMistake
import net.postchain.config.app.AppConfig
import net.postchain.core.EContext
import net.postchain.metrics.BLOCKCHAIN_RID_TAG
import net.postchain.metrics.CHAIN_IID_TAG
import net.postchain.metrics.NODE_PUBKEY_TAG
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.tools.runcfg.*
import net.postchain.rell.utils.DiskGeneralDir
import net.postchain.rell.utils.GeneralDir
import net.postchain.rell.utils.MainRellCliEnv
import net.postchain.rell.utils.RellCliLogUtils
import org.apache.commons.configuration2.PropertiesConfiguration
import java.io.File
import java.io.StringReader
import java.nio.file.Path

private fun CliktCommand.deployXmlOption() =
        argument(name = "run.xml", ).path(mustExist = true, canBeDir = false, canBeFile = true, mustBeReadable = true).default(Path.of("config/run.xml"))

class StartCommand: CliktCommand(help= "Starts a node"){
    private val sourceDir by sourceDirOption()
    private val runXmlFile by deployXmlOption()
    private val config by configFile()
    private val unitTest by option("-.test").flag()

    //
    //postchainNode
    //blockchainAPI för att spara addConfiguration
    //start node efter det
    //efter att den kör bör kunna göra addConfiguration igen för updates
    override fun run() {
        val runConfig = generateRunConfig(false) //TODO replace
        //val runConfig = RellPostAppCliConfig()
        startPostchainNode(runConfig)
    }

    fun generateCli(
            runConfFile: File
    ): RellPostAppCliConfig {
        val cSourceDir = C_SourceDir.diskDir(sourceDir)

        val configDir = runConfFile.absoluteFile.parentFile
        val generalConfigDir = DiskGeneralDir(configDir)
        val params = RellRunConfigParams(cSourceDir, generalConfigDir, config.rellVersion, unitTest)

        val runConfText = runConfFile.readText()
        val config = generate(params, runConfFile.path, runConfText)

        return RellPostAppCliConfig(cSourceDir, configDir, config)
    }

    fun generate(
            params: RellRunConfigParams,
            confPath: String,
            confText: String
    ): RellPostAppConfig {
        val parserOpts = RunConfigParserOptions(unitTest = unitTest)
        val rcfg = readConfig(params.configDir, confPath, confText, parserOpts)

        val rawNodeConfig = if (unitTest) rcfg.testNodeConfig else rcfg.nodeConfig
        check(rawNodeConfig != null) { "Node config not defined!" }

        val nodeConfig = RunConfigNodeConfigGen.generateNodeConfig(rawNodeConfig, params.configDir)

        val testNodeConfig = if (rcfg.testNodeConfig == null) null else {
            RunConfigNodeConfigGen.generateNodeConfig(rcfg.testNodeConfig!!, params.configDir)
        }

        val replaceSigners = if (rawNodeConfig.addSigners) nodeConfig.signers else null
        val chainConfigs = RunConfigChainConfigGen.generateChainsConfigs(MainRellCliEnv, params, rcfg, replaceSigners)

        return RellPostAppConfig(nodeConfig, testNodeConfig, chainConfigs, wipeDb = rcfg.wipeDb)
    }

    private fun readConfig(
            configDir: GeneralDir,
            confPath: String,
            confText: String,
            parserOpts: RunConfigParserOptions
    ): Rcfg_Run {
        var conf = config.blockchainConfigs[0]
        println(conf)
        var nodes = true
        var nodeConfig: Rcfg_NodeConfig = Rcfg_NodeConfig("node-config.properties", null, true)
        var testNodeConfig: Rcfg_NodeConfig = Rcfg_NodeConfig("node-config-test.properties", null, true)
        var chains: List<Rcfg_Chain> = listOf(
                Rcfg_Chain("My_Rell_Project" ,
                        1,
                        listOf(
                                Rcfg_ChainConfig(
                                        0,
                                        Rcfg_App(R_ModuleName.of("main"), mapOf(), true),
                                        listOf(),
                                        false,
                                        mapOf()),
                        ),
                        listOf()
                )
        )
        var tests: List<Rcfg_TestModule> = listOf()
        return Rcfg_Run(nodeConfig, testNodeConfig, chains, tests ,true
        )
    }




    private fun generateRunConfig(test: Boolean): RellPostAppCliConfig {
        return generateCli(runXmlFile.toFile())
    }

    private val log = run {
        RellCliLogUtils.initLogging()
        KotlinLogging.logger("PostchainApp")
    }

    private fun getNodeConfig(rellAppConf: RellPostAppCliConfig, rellAppNode: RellPostAppNode): AppConfig {
        if (rellAppNode.srcPropsPath != null) {
            val file = File(rellAppNode.srcPropsPath)
            val fullFile = if (file.isAbsolute) file else File(rellAppConf.configDir, rellAppNode.srcPropsPath)
            return AppConfig.fromPropertiesFile(fullFile.absolutePath)
        }

        val text = rellAppNode.srcPropsText!!
        val conf = PropertiesConfiguration()
        conf.layout.load(conf, StringReader(text))
        return AppConfig(conf)
    }


    private fun startPostchainNode(rellAppConf: RellPostAppCliConfig): AppConfig {
        val nodeAppConf = getNodeConfig(rellAppConf, rellAppConf.config.node)

        val node = PostchainNode(nodeAppConf, rellAppConf.config.wipeDb)

        val chainsSorted = rellAppConf.config.chains.sortedBy { it.iid }

        for (chain in chainsSorted) {
            val genesisConfig = chain.configs.getValue(0).gtvConfig
            val brid = GtvToBlockchainRidFactory.calculateBlockchainRid(genesisConfig, node.postchainContext.cryptoSystem)
            withLoggingContext(
                    NODE_PUBKEY_TAG to nodeAppConf.pubKey,
                    CHAIN_IID_TAG to chain.iid.toString(),
                    BLOCKCHAIN_RID_TAG to brid.toHex()
            ) {
                withReadWriteConnection(node.postchainContext.storage, chain.iid) { eContext: EContext ->
                    BlockchainApi.initializeBlockchain(eContext, brid, override = true, genesisConfig)
                }
                log.info { "Chain '${chain.name}' ID = ${chain.iid} RID = ${brid.toHex()}" }

                check(brid.data.contentEquals(chain.brid.toByteArray())) {
                    "Chain '${chain.name}' (${chain.iid}): calculated BRID = ${chain.brid.toHex()}, postchain BRID = ${brid.toHex()}"
                }

                for ((height, config) in chain.configs) {
                    if (height != 0L) {
                        log.info("Adding configuration for chain: ${chain.iid}, height: $height")
                        withReadWriteConnection(node.postchainContext.storage, chain.iid) { eContext: EContext ->
                            BlockchainApi.addConfiguration(eContext, height, override = true, config.gtvConfig)
                        }
                    }
                }
            }
        }

        for (chain in chainsSorted) {
            try {
                node.startBlockchain(chain.iid)
            } catch (e: UserMistake) {
                throw UserMistake("Failed to start chain '${chain.name}' (IID = ${chain.iid})", e)
            } catch (e: Throwable) {
                throw RuntimeException("Failed to start chain '${chain.name}' (IID = ${chain.iid})", e)
            }
        }
        return nodeAppConf
    }

}