package com.chromia.build.tools.model

import assertk.assertThat
import assertk.assertions.isTrue
import com.chromia.build.tools.lib.LibraryVerifyer
import com.chromia.build.tools.testData
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.model.parseModel
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.rell.api.base.RellCliEnv
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class RellLibraryModelTest {

    @Test
    fun singleLibraryTest(@TempDir dir: Path) {
        testData(dir) {
            config {
                addLib("foo", RellLibraryModel("http://foo.com", null, "lib", false, "2415A364EF7DB349F3AECE7201094D9294915641D35F7D1842E83DAAF9BBC558".hexStringToWrappedByteArray()))
                addSourceFile("lib/foo/foo.rell", """module;""")
                addSourceFile("lib/foo/bar.rell", """module; //Bar""")
            }
        }

        val settings = parseModel(dir.resolve("chromia.yml").toFile())
        val libraryVerifyer = LibraryVerifyer(RellCliEnv.DEFAULT, dir.resolve("src/lib"))

        assertDoesNotThrow {
            libraryVerifyer.verifyLibs(settings.libs)
        }
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.key, it.value)).isTrue()
        }
    }

    //TODO this is not what we want, want the parser to throw error if duplicate keys "name" of libs
    @Test
    fun conflictingLibraryNameIsOverriddenTest(@TempDir dir: Path) {
        testData(dir) {
            config {
                addLib("foo", RellLibraryModel("http://foo.com", null, "lib", false, "2415A364EF7DB349F3AECE7201094D9294915641D35F7D1842E83DAAF9BBC558".hexStringToWrappedByteArray()))
                addLib("foo", RellLibraryModel("http://foo2.com", null, "lib", false, "615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5".hexStringToWrappedByteArray()))
            }
        }
        val settings = parseModel(dir.resolve("chromia.yml").toFile())
        Assertions.assertEquals(settings.libs.size, 1)
        Assertions.assertEquals(settings.libs["foo"]!!.registry, "http://foo2.com")
    }

    @Test
    fun multipleLibraryTest(@TempDir dir: Path) {
        testData(dir) {
            config {
                addLib("foo", RellLibraryModel("http://foo.com", null, "lib", false, "33B5C0C7909B01AD272346A49C4F4FCD6FF7E29685803F7F6C8B5EF320BF2F0C".hexStringToWrappedByteArray()))
                addLib("bar", RellLibraryModel("http://bar.com", null, "lib", false, "E9A6EE3D187533034566A53007CA034F328CD469D986048B31EEE86E238E6E14".hexStringToWrappedByteArray()))
                addSourceFile("lib/foo/main.rell", """module;""")
                addSourceFile("lib/foo/api.rell", """module;""")
                addSourceFile("lib/bar/main.rell", """module;""")
            }
        }
        val settings = parseModel(dir.resolve("chromia.yml").toFile())
        val libraryVerifyer = LibraryVerifyer(RellCliEnv.DEFAULT, dir.resolve("src/lib"))
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.key, it.value)).isTrue()
        }
    }

    @Test
    fun skipLibraryValidationTest(@TempDir dir: Path) {
        testData(dir) {
            config {
                addLib("foo", RellLibraryModel("http://foo.com", null, "lib", true, null))
                addSourceFile("lib/foo/main.rell", """module;""")
                addSourceFile("lib/foo/api.rell", """module;""")
            }
        }

        val settings = parseModel(dir.resolve("chromia.yml").toFile())
        val libraryVerifyer = LibraryVerifyer(RellCliEnv.DEFAULT, dir.resolve("src/lib"))
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.key, it.value)).isTrue()
        }
    }
}