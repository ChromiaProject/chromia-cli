package com.chromia.cli.check_compatilibity

import net.postchain.common.exception.UserMistake
import net.postchain.rell.api.base.RellApiBaseUtils
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.gtx.PostchainSqlInitProjExt
import net.postchain.rell.api.gtx.RellApiGtxUtils
import net.postchain.rell.api.gtx.RellApiRunTests
import net.postchain.rell.api.gtx.Rt_BlockRunnerConfig
import net.postchain.rell.api.gtx.Rt_DynamicBlockRunnerStrategy
import net.postchain.rell.api.gtx.Rt_PostchainUnitTestBlockRunner
import net.postchain.rell.base.compiler.base.utils.C_SourceDir
import net.postchain.rell.base.lib.test.Lib_RellTest
import net.postchain.rell.base.lib.type.Rt_IntValue
import net.postchain.rell.base.lib.type.Rt_TextValue
import net.postchain.rell.base.model.R_App
import net.postchain.rell.base.model.R_Attribute
import net.postchain.rell.base.model.R_EntityType
import net.postchain.rell.base.model.R_EnumType
import net.postchain.rell.base.model.R_ModuleName
import net.postchain.rell.base.model.Rt_EntityValue
import net.postchain.rell.base.model.expr.R_CreateExpr
import net.postchain.rell.base.model.expr.R_CreateExpr.Companion.buildSql
import net.postchain.rell.base.runtime.Rt_AppContext
import net.postchain.rell.base.runtime.Rt_Exception
import net.postchain.rell.base.runtime.Rt_GtvModuleArgsSource
import net.postchain.rell.base.runtime.Rt_SqlContext
import net.postchain.rell.base.runtime.Rt_Value
import net.postchain.rell.base.sql.SqlInitLogging
import net.postchain.rell.base.sql.SqlManager
import net.postchain.rell.base.sql.SqlUtils
import net.postchain.rell.base.utils.Rt_UnitTestBlockRunner
import net.postchain.rell.base.utils.toImmList

private const val RELL_TRANSACTION_ENTITY_NAME = "transaction"

/**
 * Manages the compatibility check by deploying, comparing versions and populating entity data.
 */
class CheckCompatibility(
        private val toBlockchain: BlockchainSource,
        private val logger: LogWrapper = LogWrapper()
) {
    fun upgradeFrom(
            from: BlockchainSource,
            blockchainName: String,
            withData: Boolean = true,
    ) {

        logger.phase("Building application 1 (legacy version of dapp)")
        val fromApp = from.buildApp(blockchainName, logger)

        logger.phase("Building application 2 (new version of dapp)")
        val toApp = toBlockchain.buildApp(blockchainName, logger)

        var entitiesToPopulate = listOf<EntityDependency>()
        if (withData) {
            logger.phase("Finding application differences")
            val configUpdates = ConfigUpdates(fromApp.app, toApp.app, logger)
            val (updatedEntities, entities) = configUpdates.getUpdatedEntitiesWithDependencies()
            logger.echo("$updatedEntities entities updated")

            entitiesToPopulate = entities
        }

        logger.phase("Deploying application 1")
        deploy(fromApp, true, from.source) { sqlManager, sqlCtx ->

            if (withData && entitiesToPopulate.isNotEmpty()) {
                logger.phase("Creates mock data")
                populateEntities(sqlManager, sqlCtx, entitiesToPopulate)
                logger.echo("Populated ${entitiesToPopulate.size} entities with data")
            }
        }

        logger.phase("Deploying application 2")
        deploy(toApp, false, toBlockchain.source)
    }

    private fun populateEntities(sqlManager: SqlManager, sqlCtx: Rt_SqlContext, entities: List<EntityDependency>) {

        entities
                .map { it.entity }
                .forEach { entity ->

                    if (entity.simpleName == RELL_TRANSACTION_ENTITY_NAME) {
                        populateTransactionEntity(sqlManager)
                    } else {

                        val values = R_CreateExpr.CreateValues(entity.attributes.values.toList(), createEntityValues(entity.attributes.values))
                        val rtSql = buildSql(sqlCtx, entity, values, "0")

                        logger.verbose("Populate ${entity.simpleName}: ${rtSql.sql}")

                        try {
                            sqlManager.execute(true) { sqlExecutor ->

                                rtSql.execute(sqlExecutor)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
    }

    private fun populateTransactionEntity(sqlManager: SqlManager) {
        // It would be nice to call c_utils.createTransactionEntity instead
        val s = """INSERT INTO "c0.transactions"(tx_iid,tx_rid,tx_data,tx_hash,tx_number,block_iid)
                                        VALUES(0,E'\\x00',E'\\x00',E'\\x00',0,0);"""
        sqlManager.execute(true) { sqlExecutor ->

            sqlExecutor.execute(s)
        }
    }

    private fun createEntityValues(values: Collection<R_Attribute>): List<List<Rt_Value>> {

        val list = mutableListOf<List<Rt_Value>>()

        list.add(values
                .map {
                    val defaultValue = it.type.defaultValue() ?: when (it.type) {
                        is R_EntityType -> Rt_EntityValue(it.type as R_EntityType, 0)
                        is R_EnumType -> Rt_IntValue.get(0)
                        else -> {
                            logger.echo("No default value for type: $it.type")
                            Rt_TextValue.get("0")
                        }
                    }
                    defaultValue
                })

        return list
    }

    private fun deploy(app: CompiledApp, dropTables: Boolean, source: C_SourceDir, postDeploy: (SqlManager, Rt_SqlContext) -> Unit = { _, _ -> }) {

        val globalCtx = RellApiBaseUtils.createGlobalContext(
                app.options,
                typeCheck = false,
                outPrinter = app.config.outPrinter,
                logPrinter = app.config.logPrinter,
        )

        val blockRunner = createBlockRunner(app.config, source, app.app, app.rAppModules)

        val sqlCtx = RellApiBaseUtils.createSqlContext(app.app)
        val chainCtx = RellApiBaseUtils.createChainContext()

        RellApiGtxUtils.runWithSqlManager(
                dbUrl = app.config.databaseUrl,
                dbProperties = null,
                sqlLog = logger.verbose,
                sqlErrorLog = app.config.sqlErrorLog,
        ) { sqlMgr ->
            val appCtx = Rt_AppContext(
                    globalCtx,
                    chainCtx,
                    app.app,
                    repl = false,
                    test = true,
                    blockRunner = blockRunner,
                    moduleArgsSource = Rt_GtvModuleArgsSource(app.config.compileConfig.moduleArgs, app.options),
            )

            try {
                SqlUtils.initDatabase(
                        appCtx,
                        sqlCtx,
                        sqlMgr,
                        adapter = PostchainSqlInitProjExt,
                        dropTables = dropTables,
                        sqlInitLog = logger.verbose,
                )

                postDeploy(sqlMgr, sqlCtx)
            } catch (e: Rt_Exception) {
                throw UserMistake("Deployment failed:\n${e.message}")
            }
        }
    }

    // Code duplication from Rell
    private fun createBlockRunner(
            config: RellApiRunTests.Config,
            sourceDir: C_SourceDir,
            app: R_App,
            appModules: List<R_ModuleName>?,
    ): Rt_UnitTestBlockRunner {
        val keyPair = Lib_RellTest.BLOCK_RUNNER_KEYPAIR

        val blockRunnerCfg = Rt_BlockRunnerConfig(
                forceTypeCheck = true,
                sqlLog = config.sqlLog,
                dbInitLogLevel = SqlInitLogging.LOG_NONE,
        )

        val mainModules = when {
            appModules == null -> null
            config.activateTestDependencies -> (appModules + RellApiBaseUtils.getMainModules(app)).toSet().toImmList()
            else -> appModules
        }

        val gtvCompileConfig = RellApiCompile.Config.Builder(config.compileConfig)
                .quiet(true)
                .build()

        val blockRunnerStrategy = Rt_DynamicBlockRunnerStrategy(sourceDir, keyPair, mainModules, gtvCompileConfig)
        return Rt_PostchainUnitTestBlockRunner(keyPair, blockRunnerCfg, blockRunnerStrategy)
    }
}