package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.exists
import java.io.File
import kotlin.io.path.absolutePathString
import net.postchain.common.PropertiesFileLoader
import org.bitcoinj.crypto.MnemonicException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
}
