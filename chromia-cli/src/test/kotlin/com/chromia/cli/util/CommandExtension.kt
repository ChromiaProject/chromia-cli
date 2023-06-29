package com.chromia.cli.util

import com.github.ajalt.clikt.core.CliktCommand
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class CommandExtension(private val command: CliktCommand): BeforeEachCallback, AfterEachCallback {

    private val init = InitExtension()
    val dir get() = init.dir

    override fun beforeEach(p0: ExtensionContext) {
        init.beforeEach(p0)
    }

    override fun afterEach(p0: ExtensionContext) {
        init.afterEach(p0)
    }

    fun parse(argv: List<String> = listOf()) = command.parse(listOf("--settings", init.dir.absolutePath.plus("/config.yml")) + argv)
    fun emptyParse(argv: List<String> = listOf()) = command.parse(argv)
}
