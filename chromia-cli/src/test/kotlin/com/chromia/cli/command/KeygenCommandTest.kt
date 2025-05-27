package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.testing.test
import net.postchain.common.PropertiesFileLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.Ignore

class KeygenCommandTest {

    @Test
    fun keygenRecover() {
        val file = kotlin.io.path.createTempFile()
        KeygenCommand().parse(arrayOf(
                "-m", "picnic shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train",
                "-f", file.absolutePathString(),
        ))

        val keys = PropertiesFileLoader.load(file.absolutePathString())
        assertThat("02AF635148608B9A18DF11241F1862624C3E7CCEDEC0864FEE00B3D4E7093CC4CF").isEqualTo(keys.getString("pubkey"))
        assertThat("CE59E2F0E7342EFB12A889B4168E3C7D909EC858849C0CA5FFAB78541C22AB65").isEqualTo(keys.getString("privkey"))
        assertThat(file.parent.resolve("${file.name}_mnemonic")).exists()
    }

    @Test
    fun invalidLengthOfMnemonic() {
        val exception = assertThrows<IllegalArgumentException> {
            KeygenCommand().parse(arrayOf("-m", "invalid mnemonic", "-f", "dummy_file"))
        }
        assertEquals("Invalid number of words in mnemonic. Supported number of words are 12 or 24", exception.message)
    }

    @Test
    fun generateFileInWorkingDir() {
        KeygenCommand().parse(listOf("--file", ".secret"))
        val file = File(".secret")
        val mnemonicFile = File(".secret_mnemonic")
        assertThat(file).exists()
        assertThat(mnemonicFile).exists()
        file.delete()
        mnemonicFile.delete()
    }

    @Test
    fun generateFileWithPath(@TempDir dir: Path) {
        KeygenCommand().parse(listOf("--file", "${dir}/.secret"))
        val file = dir.resolve(".secret")
        val mnemonicFile = dir.resolve(".secret_mnemonic")
        assertThat(file).exists()
        assertThat(mnemonicFile).exists()
    }

    @Test
    fun commandThrowsExceptionWithMissingAttributeTest(@TempDir dir: Path) {
        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            val error = KeygenCommand().test(arrayOf(
                    "-m", "new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train"
            ))
            assertThat(error.stderr).isEqualTo("Usage: keygen [<options>]\n\nError: must provide one of --file, --get-pubkey, --dry, --key-id\n")
        }
    }

    @Test
    fun storeKeyPairFilesWithUserSetKeyId(@TempDir dir: Path) {
        val myKeyId = "myKeyId"
        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            KeygenCommand().parse(arrayOf(
                    "-m", "new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train",
                    "--key-id", myKeyId
            ))
        }
        val privateKeyFile = dir.resolve(myKeyId)
        val publicKeyFile = dir.resolve("$myKeyId.pubkey")
        val mnemonicFile = dir.resolve("${myKeyId}_mnemonic")

        dir.listDirectoryEntries().containsAll(listOf(privateKeyFile, publicKeyFile, mnemonicFile))
        assertThat(privateKeyFile.readText()).isEqualTo("7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429")
        assertThat(publicKeyFile.readText()).isEqualTo("02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765")
        assertThat(mnemonicFile.readText()).contains("new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train")
    }

    @Test
    fun duplicateKeyIdThrowsErrorTest(@TempDir dir: Path) {
        val myKeyId = "myKeyId"
        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            KeygenCommand().parse(arrayOf("--key-id", myKeyId,
                    "-m", "new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train"
            ))
            val res = KeygenCommand().test(arrayOf("--key-id", myKeyId))
            //Make sure only output is the message
            assertThat(res.output).isEqualTo("Keypair with id: myKeyId already exists\n")
        }
        val privateKeyFile = dir.resolve(myKeyId)
        val publicKeyFile = dir.resolve("$myKeyId.pubkey")
        val mnemonicFile = dir.resolve("${myKeyId}_mnemonic")

        dir.listDirectoryEntries().containsAll(listOf(privateKeyFile, publicKeyFile))
        assertThat(privateKeyFile.readText()).isEqualTo("7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429")
        assertThat(publicKeyFile.readText()).isEqualTo("02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765")
        assertThat(mnemonicFile.readText()).contains("new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train")
    }

    @Test
    fun usingMultipleAttributesThrowsErrorTest(@TempDir dir: Path) {
        val error = KeygenCommand().test(
                arrayOf(
                        "-m", "new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train",
                        "--key-id", "myKeyId",
                        "--file", ".secret",
                ))
        assertThat(error.stderr).isEqualTo("Usage: keygen [<options>]\n\nError: option --file cannot be used with --get-pubkey or --dry or --key-id\n")
    }

    @Test
    fun dryRunDoNotSaveKeys(@TempDir dir: Path) {
        val myKeyId = "myKeyId"
        val secretFile = dir.resolve(".secret")

        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            val output = KeygenCommand().test(arrayOf(
                    "-m", "new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train",
                    "--dry"
            ))

            assertThat(output.output).isEqualTo("""
                mnemonic:  new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train 
                pubkey:    02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765
                privkey:   7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429
                
            """.trimIndent())
        }
        val privateKeyFile = dir.resolve(myKeyId)
        val publicKeyFile = dir.resolve("$myKeyId.pubkey")

        assertThat(privateKeyFile.exists()).isEqualTo(false)
        assertThat(publicKeyFile.exists()).isEqualTo(false)
        assertThat(secretFile.exists()).isEqualTo(false)
    }

    @Test
    fun testRetrievePublicKeyWithSpecificKeyId(@TempDir dir: Path) {
        val keyId = "testKeyId"
        val pubkeyValue = "02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765"
        val privkeyValue = "7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429"
        
        val privateKeyFile = dir.resolve(keyId).toFile()
        privateKeyFile.writeText(privkeyValue)
        
        val publicKeyFile = dir.resolve("$keyId.pubkey").toFile()
        publicKeyFile.writeText(pubkeyValue)
        
        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            val output = KeygenCommand().test(arrayOf("--get-pubkey", keyId))
            assertThat(output.output).contains("pubkey: $pubkeyValue")
        }
    }
    
    @Test
    fun testRetrievePublicKeyWithNonExistentKeyId(@TempDir dir: Path) {
        val nonExistentKeyId = "nonExistentKeyId"
        
        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            val output = KeygenCommand().test(arrayOf("--get-pubkey", nonExistentKeyId))
            assertThat(output.output).isEqualTo("No keypair found for key-id: $nonExistentKeyId\n")
        }
    }
    
    @Test
    fun testRetrievePublicKeyFromGlobalConfig(@TempDir dir: Path) {
        val pubkeyValue = "02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765"
        val privkeyValue = "7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429"
        
        val config = dir.resolve("config").toFile()
        config.writeText("""
            pubkey: $pubkeyValue
            privkey: $privkeyValue
        """.trimIndent())
        
        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            val output = KeygenCommand().test(arrayOf("--get-pubkey"))
            assertThat(output.output).contains("pubkey: $pubkeyValue")
        }
    }
    
    @Test
    fun testRetrievePublicKeyWithNoKeyFound(@TempDir dir: Path) {
        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            val output = KeygenCommand().test(arrayOf("--get-pubkey"))
            assertThat(output.output)
                .isEqualTo("No pubkey found, in either your local/global configuration or the active key-id\n")
        }
    }

    @Test
    @Ignore("can't set user.dir to workingDir in order to test the retrieval of pubkey from local config")
    fun testRetrievePublicKeyFromLocalConfig(@TempDir dir: Path) {
        val pubkeyValue = "02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765"
        val privkeyValue = "7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429"

        val workingDir = dir.resolve("working_dir").toFile().also { it.mkdir() }

        val chromiaDir = workingDir.resolve(".chromia").also { it.mkdir() }

        val configFile = chromiaDir.resolve("config").also { it.createNewFile() }
        configFile.writeText("""
            pubkey: $pubkeyValue
            privkey: $privkeyValue
        """.trimIndent())

        EnvironmentVariables("CHROMIA_CONFIG", null)
            .and("user.dir", chromiaDir.absolutePath)
            .execute {
                val result = KeygenCommand().test(arrayOf("--get-pubkey"))
                println("currentDir --> ${dir.toAbsolutePath()}")
                println("currentDir --> ${System.getProperty("user.dir")}")
                assertThat(result.output).contains("pubkey: $pubkeyValue")
        }
    }
}
