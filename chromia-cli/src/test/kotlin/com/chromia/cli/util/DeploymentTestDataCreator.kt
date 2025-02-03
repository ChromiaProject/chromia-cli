package com.chromia.cli.util

import com.chromia.build.tools.restapi.CachedModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.TestModel
import java.io.File
import java.nio.file.Path
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.GtvObjectMapper

class DeploymentTestModel(val model: TestModel, val yamlConfig: Gtv = DeploymentTestDataCreator.yamlConfig) : CachedModel by model {
    override fun getBlockchainConfiguration(height: Long): ByteArray? {
        return GtvEncoder.encodeGtv(yamlConfig)
    }
}

object DeploymentTestDataCreator {

    val keyIdName = "keyIdUsedForTesting"
    const val pubkey = "03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05"
    const val privkey = "BBBDFE956021912512E14BB081B27A35A0EABC4098CB687E973C434006BCE114"

    val mainRellContent = """
                module;
                import a_file.*;
                query hello() = "Hi!";
                operation call_op(value: integer) {}
                entity person {
                    first_name: text;
                    last_name: text;
                }
                entity car {
                    brand: text;
                    number: text;
                    owner: person;
                }
                entity card {
                    number: text;
                    owner: person;
                }
            """.trimIndent()

    val otherRellFileContent = """
                module;
                query hello_a() = "Hi!";
                operation call_op_a(value: integer) {}
            """.trimIndent()

    val rellSources = mapOf(
            "main.rell" to mainRellContent,
            "a_file.rell" to otherRellFileContent
    )

    val yamlConfig = buildGtvConfig(rellSources)


    fun buildGtvConfig(sources: Map<String, String>): Gtv {
        val rellConfig = gtv(mapOf(
                "compilerVersion" to gtv("0.14.5"),
                "modules" to gtv(listOf(gtv("main"))),
                "sources" to GtvObjectMapper.toGtvDictionary(sources),
                "strictGtvConversion" to gtv(1),
                "version" to gtv("0.13.14")
        ))

        val gtxConfig = gtv(mapOf(
                "modules" to gtv(listOf(
                        gtv("net.postchain.rell.module.RellPostchainModuleFactory"),
                        gtv("net.postchain.gtx.StandardOpsGTXModule")
                )),
                "rell" to rellConfig
        ))

        val blockStrategy = gtv(mapOf(
                "mininterblockinterval" to gtv(1000),
                "name" to gtv("net.postchain.base.BaseBlockBuildingStrategy")
        ))

        val revoltConfig = gtv(mapOf(
                "fast_revolt_status_timeout" to gtv(2000),
                "revolt_when_should_build_block" to gtv(1)
        ))

        return gtv(mapOf(
                "add_primary_key_to_header" to gtv(1),
                "blockstrategy" to blockStrategy,
                "config_consensus_strategy" to gtv("HEADER_HASH"),
                "configurationfactory" to gtv("net.postchain.gtx.GTXBlockchainConfigurationFactory"),
                "gtx" to gtxConfig,
                "revolt" to revoltConfig
        ))
    }


    val deployedChainBrid = BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000002")
    val deployedChainModel = DeploymentTestModel(TestModel(deployedChainBrid, 1), yamlConfig)
    val wrongConfigBrid = BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000003")

    fun unitTestApp(dir: Path) {
        rellSources.forEach { (path, content) ->
            with(File(dir.toFile(), "src/$path")) {
                parentFile.mkdirs()
                writeText(content)
            }
        }
        with(File(dir.toFile(), "chromia.yml")) {
            writeText("""
                blockchains:
                  my_rell_dapp:
                    module: main
                    config:
                      blockstrategy:
                        maxblocktime: 1000
                  deployed:
                    module: main      
                  wrongConfig: 
                    module: main
                    moduleArgs:
                      main:
                        name: { nameIsInterprededAsDict }
                deployments:
                  test:
                    url: "${RestApiInstance.apiUrl}"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    container: foo
                    chains:
                      deployed: x"${deployedChainBrid.toHex()}"
                      wrongConfig: x"${wrongConfigBrid.toHex()}"
            """.trimIndent())
        }
        with(File(dir.toFile(), ".secret")) {
            writeText("""
            privkey = $privkey
            pubkey = $pubkey
            """.trimIndent())
        }

        with(File(dir.toFile(), "config")) {
            writeText("""
            keyId = $keyIdName
            """.trimIndent())
        }
    }
}
