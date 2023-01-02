package com.chromia.cli

import com.github.ajalt.clikt.output.CliktConsole
import java.io.IOException
import assertk.assertThat
import assertk.assertions.contains
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

open class TestConsole() : CliktConsole, AfterEachCallback {
    val out = mutableListOf<Pair<String, Boolean>>()

    override fun promptForLine(prompt: String, hideInput: Boolean) = try {
        print(prompt, false)
        readLine() ?: throw RuntimeException("EOF")
    } catch (err: IOException) {
        throw err
    }

    override fun print(text: String, error: Boolean) {
        out.add(text to error)
    }

    override val lineSeparator: String get() = System.lineSeparator()

    fun assertContains(text: String) = assertThat(out.map { it.first }).contains(text)

    override fun afterEach(p0: ExtensionContext?) {
        out.clear()
    }
}
