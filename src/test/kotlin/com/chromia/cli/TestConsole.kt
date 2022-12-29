package com.chromia.cli

import com.github.ajalt.clikt.output.CliktConsole
import java.io.IOException

open class TestConsole : CliktConsole {
    override fun promptForLine(prompt: String, hideInput: Boolean) = try {
        print(prompt, false)
        readLine() ?: throw RuntimeException("EOF")
    } catch (err: IOException) {
        throw err
    }

    override fun print(text: String, error: Boolean) {
        if (error) {
            System.err
        } else {
            System.out
        }.print(text)
    }

    override val lineSeparator: String get() = System.lineSeparator()
}