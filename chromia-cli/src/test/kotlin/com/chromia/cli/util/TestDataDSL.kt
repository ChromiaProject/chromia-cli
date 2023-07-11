package com.chromia.cli.util

import java.io.File
import java.nio.file.Path
import net.postchain.crypto.KeyPair

class TestDataBuilder {

    private var content = """
        module;
        query hello() = "Hi!";
        operation call_op(value: integer) {} 
    """.trimIndent()
    private val sourceFiles = mutableMapOf<String, () -> String>("main.rell" to { content })
    private val configBuilder = ConfigBuilder()
    private var secretBuilder: SecretBuilder? = null

    fun content(init: String) {
        content = init
    }

    fun addFile(name: String, content: String) {
        sourceFiles[name] = { content }
    }

    internal fun createFiles(target: Path) {
        val sourceFolder = File(target.toFile(), "src")
        sourceFiles.forEach { (name, content) ->
            File(sourceFolder, name).also { it.parentFile.mkdirs() }.writeText(content())
        }
        configBuilder.createFile(target)
        secretBuilder?.createFile(target)
    }

    fun config(init: ConfigBuilder.() -> Unit) {
        init(configBuilder)
    }

    fun secret(init: SecretBuilder.() -> Unit = {}) {
        secretBuilder = SecretBuilder()
        init(secretBuilder!!)
    }

}

class ConfigBuilder {
    private var content = """
        blockchains:
          hello:
            module: main
            config:
              blockstrategy:
                maxblocktime: 1000
    """.trimIndent()
    private var deployments = ""
    private var libs = ""
    private var test = ""

    fun blockchains(init: String) { content = init }

    fun deployments(init: String) { deployments = init }
    fun libs(init: String) { libs = init }
    fun test(init: String) { test = init }

    internal fun createFile(target: Path) {
        val sb = StringBuilder()
        sb.append(content)
        if (deployments.isNotEmpty()) sb.append("\n$deployments")
        if (libs.isNotEmpty()) sb.append("\n$libs")
        if (test.isNotEmpty()) sb.append("\n$test")
        File(target.toFile(), "config.yml").writeText(sb.toString())
    }
}

class SecretBuilder {
    private var keyPair = KeyPair.of("03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05", "BBBDFE956021912512E14BB081B27A35A0EABC4098CB687E973C434006BCE114")
    fun keyPair(pubkey: String, privkey: String) {
        keyPair = KeyPair.of(pubkey, privkey)
    }

    fun createFile(target: Path) {
        File(target.toFile(), ".chromia/config").also { it.parentFile.mkdirs() }.writeText("""
           pubkey=${keyPair.pubKey.hex()} 
           privkey=${keyPair.privKey.hex()} 
        """.trimIndent() )
    }
}

fun testData(dir: Path, init: TestDataBuilder.() -> Unit = {}) {
    TestDataBuilder().apply(init)
            .createFiles(dir)
}