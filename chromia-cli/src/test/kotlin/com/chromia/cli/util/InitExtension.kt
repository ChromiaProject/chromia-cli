package com.chromia.cli.util

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.nio.file.Files

class InitExtension : BeforeEachCallback, AfterEachCallback {

    lateinit var dir: File
    lateinit var projectDir: File

    override fun beforeEach(p0: ExtensionContext) {
        dir = Files.createTempDirectory(p0.displayName).toFile()
        projectDir = dir.resolve("hello")
        testData(projectDir.toPath())
    }

    override fun afterEach(p0: ExtensionContext) {
        dir.deleteRecursively()
    }
}
