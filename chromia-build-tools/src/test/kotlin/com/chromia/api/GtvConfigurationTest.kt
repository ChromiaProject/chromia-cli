package com.chromia.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.testData
import com.chromia.cli.model.parseModel
import java.nio.file.Path
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.rell.api.base.RellCliEnv
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GtvConfigurationTest {

    @Test
    fun `Configuration is as expected`(@TempDir dir: Path) {
        testData(dir) {
            content("module;")
            config {
                blockchains("""
                blockchains:
                  a:
                    module: main
                    """.trimIndent()
                )
            }
        }

        val model = parseModel(dir.resolve("chromia.yml"))
        val gtv = ChromiaCompileApi.build(RellCliEnv.DEFAULT, model)[0].config
        val expectedGtv = GtvMLParser.parseGtvML(javaClass.getResource("expectedGtvConfiguration.xml")!!.readText())

        assertThat(gtv).isEqualTo(expectedGtv)
    }
}
