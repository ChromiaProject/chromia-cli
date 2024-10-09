package com.chromia.build.tools.gtv

import net.postchain.gtv.GtvFactory.gtv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TransformTest {
    @Test
    fun testRemoveFromDict1() {
        val original = gtv(mapOf("second" to gtv("here"), "stuff" to gtv("there")))
        val modified = original.remove("stuff")
        assertEquals(
                gtv(mapOf("second" to gtv("here"))),
                modified
        )
    }

    @Test
    fun testRemoveFromDict2() {
        val original = gtv(mapOf("first" to gtv(mapOf("second" to gtv("here"), "stuff" to gtv("there")))))
        val modified = original.remove("first", "stuff")
        assertEquals(
                gtv(mapOf("first" to gtv(mapOf("second" to gtv("here"))))),
                modified
        )
    }

    @Test
    fun testRemoveFromDictMixed() {
        val original = gtv(mapOf("first" to gtv("text")))
        val modified = original.remove("first", "stuff")
        assertEquals(
                original,
                modified
        )
    }

    @Test
    fun testRemoveFromDictNotFound1() {
        val original = gtv(mapOf("second" to gtv("here"), "stuff" to gtv("there")))
        val modified = original.remove("bogus")
        assertEquals(
                original,
                modified
        )
    }

    @Test
    fun testRemoveFromDictNotFound2() {
        val original = gtv(mapOf("second" to gtv("here"), "stuff" to gtv("there")))
        val modified = original.remove("bogus1", "bogus2")
        assertEquals(
                original,
                modified
        )
    }
}
