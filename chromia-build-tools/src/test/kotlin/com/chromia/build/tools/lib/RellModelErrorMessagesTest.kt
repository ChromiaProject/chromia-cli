package com.chromia.build.tools.lib

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.build.tools.compile.ValidationException
import com.chromia.build.tools.testData
import com.chromia.cli.model.parseModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path


internal class RellModelErrorMessagesTest {

    @Test
    fun `unrecognized field in rell model test`(@TempDir dir: Path) {
        testData(dir) {
            config {
                setFullLib("""
                libs:
                    foo:
                        registry: http://foo.com
                        path: lib
                        rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                        some_unexpected_field: 123
                """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("""
            Following errors found in chromia.yml:
            Additional property 'foo' found but was invalid (location: libs->foo)
            Additional property 'some_unexpected_field' found but was invalid (location: libs->foo->some_unexpected_field)
        """.trimIndent())
    }

    @Test
    fun `can not parse blockchain config brid values that do not follow the brid format`(@TempDir dir: Path) {
        testData(dir) {
            config {
                blockchains("""
                blockchains:
                  a:
                    module: main
                    config:
                        historic_brid: 615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3
                        icmf:
                            receiver:
                              local:
                                - topic: "L_topic"
                                  brid: 615175A2847D739C2CD0EC27339E8128549E513654069E2912A
                            
                    """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("""
            Following errors found in chromia.yml:
            Additional property 'a' found but was invalid (location: blockchains->a)
            String doesn't match pattern ^(x"[0-9A-Fa-f]{64}")${'$'} - "615175A2847D739 ... 513654069E2912A" (location: blockchains->a->config->icmf->receiver->local->0->brid)
            String doesn't match pattern ^(x"[0-9A-Fa-f]{64}")${'$'} - "615175A2847D739 ... 654069E2912A7E3" (location: blockchains->a->config->historic_brid)
        """.trimIndent())
    }

    @Test
    fun `invalid indent in rell model test`(@TempDir dir: Path) {
        testData(dir) {
            config {
                blockchains("""
                blockchains:
              bc1:
                """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("""
            Unable to parse file chromia.yml due to unexpected value at line: 1, column: 0
                bc1:
                ^
        """.trimIndent())
    }

    @Test
    fun `incorrect type of additional property`(@TempDir dir: Path) {
        testData(dir) {
            config {
                blockchains("""
                blockchains:
                    - bc1:
                        module: main
                """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("""
            Following errors found in chromia.yml:
            Incorrect type, expected object (location: blockchains)
        """.trimIndent())
    }

    @Test
    fun `incorrect type byteArray for registry in library model expecting string test`(@TempDir dir: Path) {
        testData(dir) {
            config {
                extra("""
                libs:
                    foo:
                        path: lib
                        registry: x"1234"
                """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("Incorrect type, expected String (location: libs->foo->registry)")
    }

    @Test
    fun `incorrect type byteArray for tagOrBranch in library model expecting optional string test`(@TempDir dir: Path) {
        testData(dir) {
            config {
                extra("""
                libs:
                    foo:
                        registry: http://foo.com
                        path: lib
                        tagOrBranch: x"1234"
                """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("Incorrect type, expected String (location: libs->foo->tagOrBranch)")
    }

    @Test
    fun `incorrect type string for rid in library model expecting optional byteArray test`(@TempDir dir: Path) {
        testData(dir) {
            config {
                extra("""
                libs:
                    foo:
                        registry: http://foo.com
                        path: lib
                        rid: 615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5
                """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("""
            Following errors found in chromia.yml:
            Additional property 'foo' found but was invalid (location: libs->foo)
            String doesn't match pattern ^x"[a-fA-F0-9]+"${'$'} - "615175A2847D739 ... 2A7E3C3E7032DB5" (location: libs->foo->rid)
        """.trimIndent())
    }

    @Test
    fun `incorrect type length for brid in deployment model throws an error`(@TempDir dir: Path) {
        testData(dir) {
            config {
                deployments("""
                deployments:
                    foo:
                        url: "http://foo.com"
                        brid: 615175A2847D739C2CD0EC2733
                """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("""
        Following errors found in chromia.yml:
        Additional property 'foo' found but was invalid (location: deployments->foo)
        String doesn't match pattern ^(x"[0-9A-Fa-f]{64}")|([0-9A-Fa-f]{64})$ - "615175A2847D739C2CD0EC2733" (location: deployments->foo->brid)
        """.trimIndent())
    }

    @Test
    fun `missing quotes for hex when not denoting it as a bytearray throws an error`(@TempDir dir: Path) {
        testData(dir) {
            config {
                deployments("""
                deployments:
                    foo:
                        url: "http://foo.com"
                        brid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                        chains:
                            bc1: 1234123412341234123412341234123412341234123412341234123412341234
                """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("""
            Following errors found in chromia.yml:
            Additional property 'foo' found but was invalid (location: deployments->foo)
            Additional property 'bc1' found but was invalid (location: deployments->foo->chains->bc1)
            Incorrect type, expected string (location: deployments->foo->chains->bc1)
        """.trimIndent())
    }

    @Test
    fun `incorrect type string for insecure in library model boolean test`(@TempDir dir: Path) {
        testData(dir) {
            config {
                extra("""
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      insecure: 123
                """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("""
            Following errors found in chromia.yml:
            Additional property 'foo' found but was invalid (location: libs->foo)
            Incorrect type, expected boolean (location: libs->foo->insecure)
        """.trimIndent())
    }

    @Test
    fun `incorrect type byteArray for test module expecting list of Strings`(@TempDir dir: Path) {
        testData(dir) {
            config {
                test("""
                test:
                  modules: x"1234"
                """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("""
            Following errors found in chromia.yml:
            Incorrect type, expected array (location: test->modules)
        """.trimIndent())
    }

    @Test
    fun `incorrect type byteArray for database host expecting option string`(@TempDir dir: Path) {
        testData(dir) {
            config {
                database("""
                database:
                    schema: rell_dapp
                    host: x"1234"
                """.trimIndent())
            }
        }
        val throwable = assertThrows<ValidationException> { parseModel(dir.resolve("chromia.yml").toFile()) }
        assertThat(throwable.message!!).contains("Incorrect type, expected String (location: database->host)")
    }
}