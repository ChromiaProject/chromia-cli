package com.chromia.cli.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.common.BlockchainRid
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.runtime.Rt_ChainSqlMapping
import java.io.File

//TODO add auto completion
//val name by option(completionCandidates = CompletionCandidates.Custom {
//    """
//        WORDS=${'$'}(echo completion1 completion2)
//        COMPREPLY=(${'$'}(compgen -W "${'$'}WORDS" -- "${'$'}{COMP_WORDS[${'$'}COMP_CWORD]}"))
//        """.trimIndent()
//})

fun CliktCommand.sourceDirOption() =
        option(help = "Rell source directory")
                .file(mustExist = true, canBeFile = false, canBeDir = true)
                .default(File(System.getProperty("user.dir")))

fun CliktCommand.modulesFiles() =
        option("-tm","--test-modules", help = "Comma separated list of file names under the module, ex: testFile1,testFile2,...")
                .convert { R_ModuleName.of(it) }.split(",")
fun CliktCommand.brid() =
        option("-brid", "--blockchain-rid", help = "Blockchain RID")
                .convert { BlockchainRid.buildFromHex(it) }
                .default(BlockchainRid(ByteArray(32)))

fun CliktCommand.sql() =
        option("-db", "--database", help = "If a database is used ").flag()

fun CliktCommand.databaseOption() =
        option("-p", "--db-properties", help = "File path with database settings" )
                .file(mustExist = true, canBeFile = true, canBeDir = false)
                .default(File("databaseConfig.yml"))

fun CliktCommand.settingsOption() =
        option("-s", "--settings", help = "Alternate path for the settings file" )
                .file(mustExist = true, canBeFile = true, canBeDir = false)
                .default(File("compilerConfig.yml"))

fun CliktCommand.chainSQLMapper() =
        option("-cid", "--chainid", help = "Chainid, defaults to 100" )
                .long()
                .convert { Rt_ChainSqlMapping(it) }
                .default(Rt_ChainSqlMapping(100))
