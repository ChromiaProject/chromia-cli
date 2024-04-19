package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import net.postchain.common.PropertiesFileLoader
import org.bitcoinj.crypto.MnemonicException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class KeygenCommandTest {

    @Test
    fun keygenRecoverDeprecated() {
        val file = kotlin.io.path.createTempFile()
        KeygenCommand().parse(arrayOf(
                "-m", "picnic shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train",
                "-s", file.absolutePathString(),
                "--deprecated-recovery"))

        val keys = PropertiesFileLoader.load(file.absolutePathString())
        assertEquals("030C9C4203B80509B353F85792FB9F664918F6D2136D8FCE55BE1A985B89E058D3", keys.getString("pubkey"))
        assertEquals("A438E1FA331ACBB9DFD7E5F692B07F9250075752905F5763D268FA2356C24787", keys.getString("privkey"))
    }

    @Test
    fun invalidLengthOfMnemonicDeprecated() {
        val exception = assertThrows<MnemonicException.MnemonicLengthException> {
            KeygenCommand().parse(arrayOf("-m", "invalid mnemonic", "--deprecated-recovery"))
        }
        assertEquals("Word list size must be multiple of three words.", exception.message)
    }

    @Test
    fun invalidLengthOfMnemonic() {
        val exception = assertThrows<IllegalArgumentException> {
            KeygenCommand().parse(arrayOf("-m", "invalid mnemonic"))
        }
        assertEquals("Invalid number of words in mnemonic. Supported number of words are 12 or 24", exception.message)
    }

    @Test
    fun keygenInvalidCombinationOfSettings() {
        val exception = assertThrows<IllegalStateException> {
            KeygenCommand().parse(listOf("--save", ".secret", "--deprecated-recovery"))
        }
        assertEquals("Mnemonic is needed to use --deprecated-recovery", exception.message)
    }

    @Test
    fun generateFile() {
        KeygenCommand().parse(listOf("--save", ".secret"))
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
    fun savingToSettingsFileTakesPrecedence(@TempDir dir: Path) {
        val myKeyId = "myKeyId"
        val secretFile = dir.resolve(".secret")

        EnvironmentVariables("CHROMIA_HOME", dir.toString()).execute {
            KeygenCommand().parse(arrayOf(
                    "-m", "new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train",
                    "--key-id", myKeyId,
                    "--save", secretFile.toString()
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
            KeygenCommand().parse(arrayOf(
                    "-m", "new shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train",
                    "--key-id", myKeyId,
                    "--save", secretFile.toString(),
                    "--dry"
            ))
        }
        val privateKeyFile = dir.resolve(myKeyId)
        val publicKeyFile = dir.resolve("$myKeyId.pubkey")

        assertThat(privateKeyFile.exists()).isEqualTo(false)
        assertThat(publicKeyFile.exists()).isEqualTo(false)
        assertThat(secretFile.exists()).isEqualTo(false)
    }
}
