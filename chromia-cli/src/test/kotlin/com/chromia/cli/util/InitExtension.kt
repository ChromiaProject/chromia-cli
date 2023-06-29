package com.chromia.cli.util

import com.chromia.cli.CreateRellDappCommand
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.nio.file.Files

class InitExtension: BeforeEachCallback, AfterEachCallback {

    lateinit var dir: File

    override fun beforeEach(p0: ExtensionContext) {
        dir = Files.createTempDirectory(p0.displayName).toFile()
        CreateRellDappCommand().parse(listOf("-d", dir.absolutePath))
    }

    override fun afterEach(p0: ExtensionContext) {
        dir.deleteRecursively()
    }
}
