package com.chromia.cli.tools.config

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import java.io.File
import java.nio.file.Path
import net.postchain.common.BlockchainRid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ChromiaConfigWriterTest {

    @Test
    fun createConfig(@TempDir dir: Path) {
        val configFile = File(dir.toFile(), "config")
        assertThat(configFile.exists()).isFalse()
        val writer = ChromiaConfigWriter.custom(configFile)
        writer.setBrid(BlockchainRid.ZERO_RID)
        writer.setProperty("url" to "http://localhost:7740")
        assertThat(configFile.readLines()).all {
            contains("brid = 0000000000000000000000000000000000000000000000000000000000000000")
            contains("url = http://localhost:7740")
        }
    }
}