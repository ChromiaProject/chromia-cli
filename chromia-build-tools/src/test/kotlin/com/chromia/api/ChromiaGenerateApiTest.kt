package com.chromia.api

import assertk.assertThat
import assertk.assertions.exists
import com.chromia.build.tools.testData
import com.chromia.cli.model.parseModel
import net.postchain.rell.api.base.RellCliEnv
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class ChromiaGenerateApiTest {

    @Test
    fun `Can generate docs site`(@TempDir dir: Path) {
        testData(dir)
        ChromiaGenerateApi.docsSite(RellCliEnv.NULL, parseModel(dir.resolve("chromia.yml")), dir)
        assertThat(dir.resolve("build/site")).exists()
    }
}
