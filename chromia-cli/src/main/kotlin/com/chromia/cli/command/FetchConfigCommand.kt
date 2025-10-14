package com.chromia.cli.command

import com.chromia.build.tools.gtv.remove
import com.chromia.cli.tools.config.BlockchainOptions
import com.chromia.cli.util.EXPERIMENTAL_COMMAND
import com.chromia.cli.util.readBlockchainConfigFile
import com.chromia.cli.util.targetDirectoryOption
import com.chromia.directory1.cm_api.cmGetBlockchainApiUrls
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.base.configuration.BlockchainConfigurationData
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.toHex
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvInteger
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

    private val blockchainOptions by BlockchainOptions { echo(it, err = true) }.cooccurring()

    private val fileOption by option("-bc", "--blockchain-config", help = "Blockchain config file")
            .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    private val targetDir by targetDirectoryOption(help = "Directory to save blockchain config and Rell sources in")

    private val hash by option("--hash", help = "Calculate Merkle hash of the blockchain configuration").flag()

    override fun run() {
        val blockchainConfig: Gtv = fileOption?.let {
            readBlockchainConfigFile(it)
        } ?: blockchainOptions?.let {
            val client = it.url?.let { url ->
                it.config.setBrid(it.blockchainRid).setApiUrls(listOf(url)).client(PostchainClientProviderImpl())
            }
                    ?: it.config.client(PostchainClientProviderImpl()).let { directoryClient ->
                        val urls = directoryClient.cmGetBlockchainApiUrls(it.blockchainRid).toList()
                        require(urls.isNotEmpty()) {
                            "No urls found for blockchain with brid: '${it.blockchainRid}', from directory-chain with brid '${directoryClient.config.blockchainRid}'"
                        }
                        val endpoint = EndpointPool.default(urls)
                        PostchainClientImpl(directoryClient.config.copy(it.blockchainRid, endpoint))
                    }
            client.getConfiguration()
        } ?: throw UsageError("Need to specify either --blockchain-config or --blockchain-rid")

        if (hash) {
            val configHash = BlockchainConfigurationData.merkleHash(blockchainConfig)
            echo(configHash.toHex())
        }

        val blockchainConfigWithoutBloat = (blockchainConfig as? GtvDictionary)
                ?.remove("gtx", "rell", "sources")
                ?.remove("web_static", "content")
                ?: blockchainConfig

        val gtvYaml = GtvYaml()
        if (targetDir != null) {
            targetDir!!.mkdirs()
            echo("Saving blockchain config: blockchain-config.yml")
            gtvYaml.dump(blockchainConfigWithoutBloat, File(targetDir!!, "blockchain-config.yml"))

            val gtx = blockchainConfig["gtx"] as? GtvDictionary
            val rell = gtx?.get("rell") as? GtvDictionary
            val rellSources = (rell?.get("sources") as? GtvDictionary)?.dict ?: mapOf()
            val webStatic = blockchainConfig["web_static"] as? GtvDictionary
            val webStaticContent = (webStatic?.get("content") as? GtvDictionary)?.dict ?: mapOf()
            val webStaticCacheTtlSeconds = (webStatic?.get("cache_ttl_seconds") as? GtvInteger)?.integer

            if ((gtx != null && rell != null && rellSources.isNotEmpty()) || webStaticContent.isNotEmpty()) {
                echo("Creating chromia.yml")
                createChromiaYml(gtvYaml, rell, webStaticCacheTtlSeconds)
            }

            if (gtx != null && rell != null && rellSources.isNotEmpty()) {
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

            if (webStaticContent.isNotEmpty()) {
                val webDir = File(targetDir!!, "web")
                webDir.mkdir()
                webStaticContent.forEach { (name, resource) ->
                    if (resource is GtvDictionary) {
                        val content = resource.dict["content"]
                        if (content != null) {
                            echo("Saving web resource: web/$name")
                            val webFile = File(webDir, name)
                            webFile.parentFile.mkdirs()
                            when (content) {
                                is GtvString -> webFile.writeText(content.string)
                                is GtvByteArray -> webFile.writeBytes(content.bytearray)
                            }
                        }
                    }
                }
            }
        } else if (!hash) {
            echo(gtvYaml.dump(blockchainConfigWithoutBloat))
        }
    }

    private fun createChromiaYml(gtvYaml: GtvYaml, rell: GtvDictionary?, webStaticCacheTtlSeconds: Long?) {
        val moduleName = ((rell?.get("modules") as? GtvArray)?.array?.firstOrNull() as? GtvString)?.string
        val moduleArgs = (rell?.get("moduleArgs") as? GtvDictionary) ?: gtv(mapOf())
        val rellVersion = (rell?.get("version") as? GtvString)?.string
        val blockchainName = moduleName ?: "bc"
        val chromiaYaml = gtv(buildMap {
            put("blockchains", gtv(mapOf(blockchainName to gtv(buildMap {
                if (moduleName != null) {
                    put("module", gtv(moduleName))
                }
                if (moduleArgs.dict.isNotEmpty()) {
                    put("moduleArgs", moduleArgs)
                }
                if (webStaticCacheTtlSeconds != null) {
                    put("webStatic", gtv("web"))
                    put("webCacheTtlSeconds", gtv(webStaticCacheTtlSeconds))
                }
            }))))
            if (rellVersion != null) {
                put("compile", gtv(mapOf("rellVersion" to gtv(rellVersion))))
            }
        })
        gtvYaml.dump(chromiaYaml, File(targetDir!!, "chromia.yml"))
    }
}
