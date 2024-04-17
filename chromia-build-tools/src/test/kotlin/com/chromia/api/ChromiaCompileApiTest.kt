package com.chromia.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.testData
import com.chromia.cli.model.parseModel
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.rell.api.base.RellCliEnv
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class ChromiaCompileApiTest {
    private val cliEnv = RellCliEnv.NULL
    @Test
    fun `Compile Simple App`(@TempDir dir: Path) {
        testData(dir)
        val result = ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")), dir)
        assertThat(result.size).isEqualTo(1)
        assertThat(result.first().name).isEqualTo("hello")
        assertThat(result.first().config["gtx"]?.asDict()?.get("rell")?.asDict()?.containsKey("modules")).isEqualTo(true)
    }

    @Test
    fun `verify library`(@TempDir dir: Path) {
        testData(dir)
        val result = ChromiaCompileApi.verify(cliEnv, parseModel(dir.resolve("chromia.yml")), dir)
        assertThat(result).isEqualTo("0E07AA4ED877C392B2CD7215CD575913C26CB8392D5D9BE1A86C6BFB41FFF987".hexStringToWrappedByteArray())
    }
}
