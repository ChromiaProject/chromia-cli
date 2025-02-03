package com.chromia.cli.schema

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import com.chromia.cli.model.DefaultChromiaModelRellVersion
import net.postchain.gtv.GtvFactory.gtv
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class BlockchainConfigSchemaParserTest {
    private val parser = BlockchainConfigSchemaParser()

    @Test
    fun `parse should extract schema from valid blockchain config`() {
        val config = gtv(mapOf(
                "gtx" to gtv(mapOf(
                        "rell" to gtv(mapOf(
                                "version" to gtv(DefaultChromiaModelRellVersion),
                                "modules" to gtv(listOf(gtv("main"))),
                                "sources" to gtv(mapOf(
                                        "main.rell" to gtv(""" 
                            module;                
                            entity user {
                                name: text;
                                age: integer;
                            }
                        """.trimIndent())
                                ))
                        ))
                ))
        ))

        val schema = parser.parse(config)

        assertThat(schema.entities).hasSize(1)
        assertThat(schema.entities[0]).all {
            prop(Entity::name).isEqualTo("user")
            prop(Entity::fields).hasSize(2)
            prop(Entity::isObject).isFalse()
        }

        val fields = schema.entities[0].fields
        assertThat(fields[0]).all {
            prop(Field::name).isEqualTo("name")
            prop(Field::type).isEqualTo("text")
            prop(Field::nullable).isFalse()
            prop(Field::defaultValue).isNull()
        }
        assertThat(fields[1]).all {
            prop(Field::name).isEqualTo("age")
            prop(Field::type).isEqualTo("integer")
            prop(Field::nullable).isFalse()
            prop(Field::defaultValue).isNull()
        }
    }

    @Test
    fun `parse extracts correct mount names`() {
        val config = gtv(mapOf(
                "gtx" to gtv(mapOf(
                        "rell" to gtv(mapOf(
                                "version" to gtv(DefaultChromiaModelRellVersion),
                                "modules" to gtv(listOf(gtv("main"))),
                                "sources" to gtv(mapOf(
                                        "main.rell" to gtv(""" 
                            module;                
                                       
                            entity user {
                                name: text;
                            }
                            
                            namespace ns1 {
                            
                                @mount('foo.bar.user')
                                entity user {
                                    name: text;
                                }
                                
                                @mount('a.b.c')
                                namespace ns2 {
                                    entity user {
                                        name: text;
                                    }
                                }
                            }
                        """.trimIndent())
                                ))
                        ))
                ))
        ))

        val schema = parser.parse(config)

        assertThat(schema.entities).hasSize(3)
        assertThat(schema.entities).extracting { it.name }.containsExactlyInAnyOrder("user", "foo.bar.user", "a.b.c.user")
    }

    @Test
    fun `parse should extract object definitions`() {
        val config = gtv(mapOf(
                "gtx" to gtv(mapOf(
                        "rell" to gtv(mapOf(
                                "version" to gtv(DefaultChromiaModelRellVersion),
                                "modules" to gtv(listOf(gtv("main"))),
                                "sources" to gtv(mapOf(
                                        "main.rell" to gtv(""" 
                            module;                
                            object event_stats {
                                mutable event_count: integer = 0;
                                mutable last_event: text = "n/a";
                            }
                        """.trimIndent())
                                ))
                        ))
                ))
        ))

        val schema = parser.parse(config)

        assertThat(schema.entities).hasSize(1)
        assertThat(schema.entities[0]).all {
            prop(Entity::name).isEqualTo("event_stats")
            prop(Entity::fields).hasSize(2)
            prop(Entity::isObject).isTrue()
        }

        val fields = schema.entities[0].fields
        assertThat(fields[0]).all {
            prop(Field::name).isEqualTo("event_count")
            prop(Field::type).isEqualTo("integer")
            prop(Field::nullable).isFalse()
            prop(Field::defaultValue).isNull()
        }
        assertThat(fields[1]).all {
            prop(Field::name).isEqualTo("last_event")
            prop(Field::type).isEqualTo("text")
            prop(Field::nullable).isFalse()
            prop(Field::defaultValue).isNull()
        }
    }

    @Test
    fun `parse should throw exception when rell version is missing`() {
        val config = gtv(mapOf(
                "gtx" to gtv(mapOf(
                        "rell" to gtv(mapOf(
                                "sources" to gtv(mapOf(
                                        "main.rell" to gtv("entity user { name: text; }")
                                ))
                        ))
                ))
        ))

        assertThrows<IllegalArgumentException> {
            parser.parse(config)
        }.message.let {
            assertThat(it).isEqualTo("No rell version found in blockchain config")
        }
    }

    @Test
    fun `parse should throw exception when sources are missing`() {
        val config = gtv(mapOf(
                "gtx" to gtv(mapOf(
                        "rell" to gtv(mapOf(
                                "version" to gtv(DefaultChromiaModelRellVersion)
                        ))
                ))
        ))

        assertThrows<IllegalArgumentException> {
            parser.parse(config)
        }.message.let {
            assertThat(it).isEqualTo("No sources found in blockchain config")
        }
    }

    @Test
    fun `createFilesInTempFolder should create correct file structure`() {
        val filesMap = mapOf(
                "dir1/file1.rell" to "content1",
                "dir2/file2.rell" to "content2"
        )

        val tempDir = parser.createFilesInTempFolder(filesMap)

        try {
            assertThat(File(tempDir, "dir1/file1.rell")).all {
                exists()
                transform { it.readText() }.isEqualTo("content1")
            }
            assertThat(File(tempDir, "dir2/file2.rell")).all {
                exists()
                transform { it.readText() }.isEqualTo("content2")
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `parse should handle multiple entities with complex relationships`() {
        val config = gtv(mapOf(
                "gtx" to gtv(mapOf(
                        "rell" to gtv(mapOf(
                                "version" to gtv(DefaultChromiaModelRellVersion),
                                "modules" to gtv(listOf(gtv("main"))),
                                "sources" to gtv(mapOf(
                                        "main.rell" to gtv("""
                            module;                
                            entity user {
                                name: text;
                                age: integer;
                            }
                            
                            entity post {
                                title: text;
                                content: text;
                                author: user;
                            }
                        """.trimIndent())
                                ))
                        ))
                ))
        ))

        val schema = parser.parse(config)

        assertThat(schema.entities).hasSize(2)
        val userEntity = schema.entities.find { it.name == "user" }
        assertThat(userEntity).isNotNull()
        assertThat(userEntity!!.fields).hasSize(2)

        val postEntity = schema.entities.find { it.name == "post" }
        assertThat(postEntity).isNotNull()
        assertThat(postEntity!!.fields).hasSize(3)

        val authorField = postEntity.fields.find { it.name == "author" }
        assertThat(authorField).isNotNull()
        assertThat(authorField!!.type).isEqualTo("main:user")
    }
}