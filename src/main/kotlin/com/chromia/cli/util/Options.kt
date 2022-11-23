package com.chromia.cli.util

import com.chromia.cli.model.ChromiaCliModel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import net.postchain.common.BlockchainRid
import net.postchain.gtv.yaml.GtvYaml
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.runtime.Rt_ChainSqlMapping
import java.io.File
import java.nio.file.Path

//TODO add auto completion
//val name by option(completionCandidates = CompletionCandidates.Custom {
//    """
//        WORDS=${'$'}(echo completion1 completion2)
//        COMPREPLY=(${'$'}(compgen -W "${'$'}WORDS" -- "${'$'}{COMP_WORDS[${'$'}COMP_CWORD]}"))
//        """.trimIndent()
//})

fun CliktCommand.sourceDirOption() =
        option("-d", "--source-folder", help = "Rell source directory")
                .file(mustExist = true, canBeFile = false, canBeDir = true)
                .default(File(System.getProperty("user.dir")))

fun CliktCommand.outputDirOption() =
        option("-o", "--output-dir", help = "Generated configuration output dir").path(mustExist = false, canBeDir = true, canBeFile = false).default(Path.of("rell/build"))

fun CliktCommand.containerOption() = option("-c", "--container", help = "Container name")
fun  CliktCommand.nodeConfigFile() =
        option("-np", "--node-properties", help = "full path to override node properties file")
                .file(mustExist = true, canBeDir = false, canBeFile = true)
fun CliktCommand.brid() =
        option("-brid", "--blockchain-rid", help = "Blockchain RID")
                .convert { BlockchainRid.buildFromHex(it) }
                //.default(BlockchainRid(ByteArray(32)))

fun CliktCommand.deployTarget() = option("-dt", "--deploy-target", help= "If a specific target deploy model should be used")

fun CliktCommand.sql() =
        option("-db", "--database", help = "If a database is used ").flag()

fun CliktCommand.wipeSql() =
        option("-wipe", "--database-wipe", help = "If a database should be wiped").flag()

fun CliktCommand.showBridOption() = option(help = "Show blockchain rid from this configuration").flag()

fun CliktCommand.chainSQLMapper() =
        option("-cid", "--chainid", help = "Chainid, defaults to 100" )
                .long()
                .convert { Rt_ChainSqlMapping(it) }
                .default(Rt_ChainSqlMapping(100))

fun CliktCommand.configFile() = option( ).file()
        .convert { GtvYaml().load<ChromiaCliModel>(it)}
        .default(ChromiaCliModel())

fun CliktCommand.modulesFiles() =
        option("-m","--modules", help = "Optional comma separated list of file names under the module, ex: testFile1,testFile2... Will default to all modules in pwd")
                .convert { R_ModuleName.of(it) }.split(",")
fun CliktCommand.module() = option("-m","--module", help = "Name of module with rell method in")
        .convert { R_ModuleName.of(it) }

fun CliktCommand.entry() =  option("-e","--entry", help = "Name of method to run")

fun CliktCommand.arguments() = option("-a","--args", help = "List of arguments, comma separated (arg1,arg2,...)")
        .convert { it }.split(",")
        .default(listOf())