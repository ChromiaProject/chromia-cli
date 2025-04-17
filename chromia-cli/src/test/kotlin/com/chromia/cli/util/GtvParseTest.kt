package com.chromia.cli.util

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.github.ajalt.clikt.core.CliktError
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvInteger
import net.postchain.gtv.GtvNull
import net.postchain.gtv.GtvString
import net.postchain.gtv.parse.GtvParser
import org.junit.jupiter.api.Test

/**
 * Tests for the `parseArgAsGtv` function.
 * The function is responsible for parsing a given string into a Gtv object, distinguishing
 * between complex GTV structures and simple GTV strings based on predefined rules.
 */
class GtvParseTest {

    @Test
    fun `should return GtvString when input is a simple non-blank string without special starting characters`() {
        // Arrange
        val input = "simpleString"

        // Act
        val result: Gtv = parseArgAsGtv(input)

        // Assert
        assertThat(result).isEqualTo(GtvString(input))
    }

    @Test
    fun `should return parsed Gtv object when input starts with double-quote`() {
        // Arrange
        val input = "\"test string\""

        // Act
        val result: Gtv = parseArgAsGtv(input)

        // Assert
        assertThat(result).isEqualTo(GtvString("test string"))
    }

    @Test
    fun `should return parsed Gtv object when input starts with single-quote`() {
        // Arrange
        val input = "'test string'"

        // Act
        val result: Gtv = parseArgAsGtv(input)

        // Assert
        assertThat(result).isEqualTo(GtvString("test string"))
    }

    @Test
    fun `should return parsed Gtv object when input starts with square bracket`() {
        // Arrange
        val input = "[1,2,3]"

        // Act
        val result: Gtv = parseArgAsGtv(input)

        // Assert
        assertThat(result).isEqualTo(GtvParser.parse(input))
    }

    @Test
    fun `should return parsed Gtv object when input starts with digit`() {
        // Arrange
        val input = "123"

        // Act
        val result: Gtv = parseArgAsGtv(input)

        // Assert
        assertThat(result).isEqualTo(GtvInteger(123))
    }

    @Test
    fun `should return parsed Gtv object when input starts with dash`() {
        // Arrange
        val input = "-123"

        // Act
        val result: Gtv = parseArgAsGtv(input)

        // Assert
        assertThat(result).isEqualTo(GtvInteger(-123))
    }

    @Test
    fun `should return parsed Gtv object when input starts with 'x' character`() {
        // Arrange
        val input = """x"123456""""

        // Act
        val result: Gtv = parseArgAsGtv(input)

        // Assert
        assertThat(result).isEqualTo(GtvByteArray("123456".hexStringToByteArray()))
    }

    @Test
    fun `should fail if input starts with 'x' character but is not valid GTV`() {
        // Arrange
        val input = "x123456"

        // Assert
        assertFailure { parseArgAsGtv(input) }
                .isInstanceOf(CliktError::class.java)
    }

    @Test
    fun `should return parsed Gtv object when input starts with 'null'`() {
        // Arrange
        val input = "null"

        // Act
        val result: Gtv = parseArgAsGtv(input)

        // Assert
        assertThat(result).isEqualTo(GtvNull)
    }

    @Test
    fun `should return parsed Gtv object when input starts with 'true'`() {
        // Arrange
        val input = "true"

        // Act
        val result: Gtv = parseArgAsGtv(input)

        // Assert
        assertThat(result).isEqualTo(GtvInteger(1))
    }

    @Test
    fun `should return parsed Gtv object when input starts with 'false'`() {
        // Arrange
        val input = "false"

        // Act
        val result: Gtv = parseArgAsGtv(input)

        // Assert
        assertThat(result).isEqualTo(GtvInteger(0))
    }

    @Test
    fun `should return GtvString when input is a blank string`() {
        // Arrange
        val input = " "

        // Act
        val result: Gtv = parseArgAsGtv(input)

        // Assert
        assertThat(result).isEqualTo(GtvString(input))
    }
}
