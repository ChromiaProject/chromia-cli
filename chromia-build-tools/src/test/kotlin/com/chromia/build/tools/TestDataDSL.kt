package com.chromia.build.tools

import com.chromia.build.tools.keystore.ChromiaKeyStore
import com.chromia.cli.model.RellLibraryModel
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import net.postchain.crypto.KeyPair
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class TestDataBuilder {

    private var content = """
        module;
        struct module_args { name = "Baloo"; }
        query hello() = "Hi!";
        operation call_op(value: integer) {} 
    """.trimIndent()
    private val sourceFiles = mutableMapOf<String, () -> String>("main.rell" to { content })
    private val configBuilder = ConfigBuilder()
    private var secretBuilder: SecretBuilder? = null
    private var keysStoreBuilder: KeyStoreBuilder? = null

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
        keysStoreBuilder?.createFiles(target)
    }

    fun config(init: ConfigBuilder.() -> Unit) {
        init(configBuilder)
    }

    fun secret(init: SecretBuilder.() -> Unit = {}) {
        secretBuilder = SecretBuilder()
        init(secretBuilder!!)
    }

    fun keyStore(init: KeyStoreBuilder.() -> Unit = {}) {
        keysStoreBuilder = KeyStoreBuilder()
        init(keysStoreBuilder!!)
    }

    companion object {
        val keyPair = KeyPair.of("03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05", "BBBDFE956021912512E14BB081B27A35A0EABC4098CB687E973C434006BCE114")
    }
}

class ConfigBuilder {
    private var definitions = ""
    private var content = """
        blockchains:
          hello:
            module: main
            config:
              blockstrategy:
                maxblocktime: 1000
    """.trimIndent()
    private var deployments = ""
    private val libStart = "\nlibs:\n"
    private var libs = libStart
    private var test = ""
    private var docs = ""
    private var compile = ""
    private var extra = ""
    private var database = """
        database:
          schema: integration_test_schema
    """.trimIndent()

    fun blockchains(init: String) {
        content = init
    }

    fun deployments(init: String) {
        deployments = init
    }

    //Intent is to use either of the lib function never both
    fun addLib(name: String, lib: RellLibraryModel) = apply {
        formatLibModule(name, lib)
    }

    //Intent is to use either of the lib function never both
    fun setFullLib(init: String) = apply {
        libs = init
    }

    private fun formatLibModule(name: String, lib: RellLibraryModel) {
        val sb = StringBuilder()
        sb.append(libs).append(lib.format(name))
        libs = sb.toString()
    }

    fun test(init: String) {
        test = init
    }

    fun docs(init: String) {
        docs = init
    }

    fun compile(init: String) {
        compile = init
    }

    fun extra(init: String) {
        extra = init
    }

    fun database(init: String) {
        database = init
    }

    fun definitions(init: String) {
        definitions = init
    }

    fun addDeploymentsConfig() {
        deployments("""
            deployments:
                test:
                    url: "http://localhost:7745"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    container: testcontainer
            """.trimIndent())
    }

    internal fun createFile(target: Path) {
        val sb = StringBuilder()
        if (definitions.isNotEmpty()) sb.append("$definitions\n")
        sb.append(content)
        if (deployments.isNotEmpty()) sb.append("\n$deployments")
        if (libs != libStart) sb.append("\n$libs")
        if (test.isNotEmpty()) sb.append("\n$test")
        if (docs.isNotEmpty()) sb.append("\n$docs")
        sb.append("\n$database")
        if (compile.isNotEmpty()) sb.append("\n$compile")
        if (extra.isNotEmpty()) sb.append("\n$extra")
        File(target.toFile(), "chromia.yml").writeText(sb.toString())
    }
}

class SecretBuilder {
    fun createFile(target: Path) {
        File(target.toFile(), ".chromia/config").also { it.parentFile.mkdirs() }.writeText("""
           pubkey=${TestDataBuilder.keyPair.pubKey.hex()} 
           privkey=${TestDataBuilder.keyPair.privKey.hex()} 
        """.trimIndent())
    }

    fun secretFile(target: Path) {
        File(target.toFile(), ".secret").writeText("""
            pubkey=039B9ED551D5BDCC52FF9418ED77FBA7D761B24B7D06596829771A6DEA50E613AD
            privkey=D33345577D6E08997D35D3D359DAF6CD4AF91651B2C006F9974E4B73E06574F7
        """.trimIndent())
    }
}

class KeyStoreBuilder {
    private val keyIdName: String = "keyIdUsedForTesting"
    fun createFiles(target: Path) {
        EnvironmentVariables("CHROMIA_HOME", target.absolutePathString()).execute {
            ChromiaKeyStore(keyIdName).saveKeyPair(TestDataBuilder.keyPair)
        }

        File(target.toFile(), "/config").also { it.parentFile.mkdirs() }.writeText("""
           key.id = $keyIdName
        """.trimIndent())
    }
}

fun testData(dir: Path, init: TestDataBuilder.() -> Unit = {}): TestDataBuilder {
    val testDataBuilder = TestDataBuilder()
    testDataBuilder.apply(init)
            .createFiles(dir)
    return testDataBuilder
}