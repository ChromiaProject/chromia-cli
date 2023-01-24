package com.chromia.cli

import assertk.assert
import assertk.assertions.exists
import com.github.ajalt.clikt.core.context
import net.postchain.common.PropertiesFileLoader
import org.bitcoinj.crypto.MnemonicException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class KeygenTest {

    @Test
    fun keygen() {
        val file = kotlin.io.path.createTempFile()
        KeygenCommand().parse(arrayOf(
                "-m", "picnic shove leader great protect table leg witness walk night cable caution about produce engage armor first burden olive violin cube gentle bulk train",
                "-s", file.absolutePathString()))

        val keys = PropertiesFileLoader.load(file.absolutePathString())
        assertEquals("030C9C4203B80509B353F85792FB9F664918F6D2136D8FCE55BE1A985B89E058D3", keys.getString("pubkey"))
        assertEquals("A438E1FA331ACBB9DFD7E5F692B07F9250075752905F5763D268FA2356C24787", keys.getString("privkey"))

        val exception = assertThrows<MnemonicException.MnemonicLengthException> {
            KeygenCommand().parse(arrayOf("-m", "invalid mnemonic"))
        }
        assertEquals("Word list size must be multiple of three words.", exception.message)
    }

    @Test
    fun keygenTest(@TempDir dir: Path) {
        KeygenCommand().parse(listOf("--save", ".secret"))
        val file = File(".secret")
        assert(file).exists()
        file.delete()
    }
}
