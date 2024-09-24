package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.testing.test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import net.postchain.common.PropertiesFileLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class KeygenCommandTest {

    @Test
    fun keygenRecover() {
        val file = kotlin.io.path.createTempFile()
        KeygenCommand().parse(arrayOf(
                "-m", "picnic shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train",
                "-f", file.absolutePathString(),
        ))

        val keys = PropertiesFileLoader.load(file.absolutePathString())
        assertEquals("02AF635148608B9A18DF11241F1862624C3E7CCEDEC0864FEE00B3D4E7093CC4CF", keys.getString("pubkey"))
        assertEquals("CE59E2F0E7342EFB12A889B4168E3C7D909EC858849C0CA5FFAB78541C22AB65", keys.getString("privkey"))
    }

    @Test
    fun invalidLengthOfMnemonic() {
        val exception = assertThrows<IllegalArgumentException> {
            KeygenCommand().parse(arrayOf("-m", "invalid mnemonic"))
        }
        assertEquals("Invalid number of words in mnemonic. Supported number of words are 12 or 24", exception.message)
    }

    @Test
    fun generateFile() {
        KeygenCommand().parse(listOf("--file", ".secret"))
        val file = File(".secret")
        assertThat(file).exists()
        file.delete()
    }

    @Test
    fun storeKeyPairFilesWithDefaultKeyId(@TempDir dir: Path) {
        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            KeygenCommand().parse(arrayOf(
                    "-m", "new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train"
            ))
        }
        val privateKeyFile = dir.resolve("chromia_key")
        val publicKeyFile = dir.resolve("chromia_key.pubkey")

        dir.listDirectoryEntries().containsAll(listOf(privateKeyFile, publicKeyFile))
        assertThat(privateKeyFile.readText()).isEqualTo("7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429")
        assertThat(publicKeyFile.readText()).isEqualTo("02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765")
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

        dir.listDirectoryEntries().containsAll(listOf(privateKeyFile, publicKeyFile))
        assertThat(privateKeyFile.readText()).isEqualTo("7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429")
        assertThat(publicKeyFile.readText()).isEqualTo("02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765")
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

        dir.listDirectoryEntries().containsAll(listOf(privateKeyFile, publicKeyFile))
        assertThat(privateKeyFile.readText()).isEqualTo("7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429")
        assertThat(publicKeyFile.readText()).isEqualTo("02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765")
    }

    @Test
    fun savingToSettingsFileTakesPrecedence(@TempDir dir: Path) {
        val myKeyId = "myKeyId"
        val secretFile = dir.resolve(".secret")

        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            KeygenCommand().parse(arrayOf(
                    "-m", "new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train",
                    "--key-id", myKeyId,
                    "--file", secretFile.toString()
            ))
        }
        val privateKeyFile = dir.resolve(myKeyId)
        val publicKeyFile = dir.resolve("$myKeyId.pubkey")

        assertThat(privateKeyFile.exists()).isEqualTo(false)
        assertThat(publicKeyFile.exists()).isEqualTo(false)
        assertThat(secretFile).exists()
    }

    @Test
    fun dryRunDoNotSaveKeys(@TempDir dir: Path) {
        val myKeyId = "myKeyId"
        val secretFile = dir.resolve(".secret")

        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            val output = KeygenCommand().test(arrayOf(
                    "-m", "new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train",
                    "--key-id", myKeyId,
                    "--file", secretFile.toString(),
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
}
