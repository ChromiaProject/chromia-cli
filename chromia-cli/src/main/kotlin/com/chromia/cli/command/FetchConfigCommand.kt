package com.chromia.cli.command

import com.chromia.build.tools.gtv.remove
import com.chromia.cli.model.DefaultChromiaModelRellVersion
import com.chromia.cli.tools.config.BlockchainOptions
import com.chromia.cli.tools.launcher.createAliases
import com.chromia.cli.util.EXPERIMENTAL_COMMAND
import com.chromia.cli.util.readBlockchainConfigFile
import com.chromia.cli.util.targetDirectoryOption
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.d1.client.ChromiaClientProvider
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvString
import net.postchain.gtv.yaml.GtvYaml
import java.io.File

class FetchConfigCommand : ChromiaCommand(help = """
    Fetch blockchain configuration    
    $EXPERIMENTAL_COMMAND
""".trimIndent()
) {
    override val invokeWithoutSubcommand: Boolean
        get() = true

    override val hiddenFromHelp: Boolean
        get() = true

    override fun aliases() = createAliases()

    private val blockchainOptions by BlockchainOptions { echo(it, err = true) }.cooccurring()

    private val fileOption by option("-bc", "--blockchain-config", help = "Blockchain config file")
            .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    private val targetDir by targetDirectoryOption(help = "Directory to save blockchain config and Rell sources in")

    override fun run() {
        val blockchainConfig: Gtv = fileOption?.let {
            readBlockchainConfigFile(it)
        } ?: blockchainOptions?.let {
            val client = it.url?.let { url ->
                it.config.setBrid(it.blockchainRid).setApiUrls(url).client(PostchainClientProviderImpl())
            }
                    ?: ChromiaClientProvider.fromClientConfig(it.config.client(PostchainClientProviderImpl()).config)
                            .blockchain(it.blockchainRid)
            client.getConfiguration()
        } ?: throw UsageError("Need to specify either --blockchain-config or --blockchain-rid")

        val blockchainConfigWithoutRellSources = (blockchainConfig as? GtvDictionary)?.remove("gtx", "rell", "sources")
                ?: blockchainConfig
        val gtx = blockchainConfig["gtx"] as? GtvDictionary
        val rell = gtx?.get("rell") as? GtvDictionary
        val rellSources = (rell?.get("sources") as? GtvDictionary)?.dict ?: mapOf()

        val gtvYaml = GtvYaml()
        if (targetDir != null) {
            targetDir!!.mkdirs()
            echo("Saving blockchain config: blockchain-config.yml")
            gtvYaml.dump(blockchainConfigWithoutRellSources, File(targetDir!!, "blockchain-config.yml"))

            if (gtx != null && rell != null && rellSources.isNotEmpty()) {
                echo("Creating chromia.yml")
                createChromiaYml(rell, gtvYaml)

                val sourceDir = File(targetDir!!, "src")
                sourceDir.mkdir()
                rellSources.forEach { (name, source) ->
                    if (source is GtvString) {
                        echo("Saving Rell source: src/$name")
                        val sourceFile = File(sourceDir, name)
                        sourceFile.parentFile.mkdirs()
                        sourceFile.writeText(source.string)
                    }
                }
            }
        } else {
            echo(gtvYaml.dump(blockchainConfigWithoutRellSources))
        }
    }

    private fun createChromiaYml(rell: GtvDictionary, gtvYaml: GtvYaml) {
        val moduleName = ((rell["modules"] as? GtvArray)?.array?.firstOrNull() as? GtvString)?.string ?: "bc"
        val moduleArgs = (rell["moduleArgs"] as? GtvDictionary) ?: gtv(mapOf())
        val rellVersion = (rell["version"] as? GtvString)?.string ?: DefaultChromiaModelRellVersion
        val chromiaYaml = gtv(mapOf(
                "blockchains" to gtv(mapOf(moduleName to gtv(mapOf(
                        "module" to gtv(moduleName),
                        "moduleArgs" to moduleArgs
                )))),
                "compile" to gtv(mapOf("rellVersion" to gtv(rellVersion))),
        ))
        gtvYaml.dump(chromiaYaml, File(targetDir!!, "chromia.yml"))
    }
}
