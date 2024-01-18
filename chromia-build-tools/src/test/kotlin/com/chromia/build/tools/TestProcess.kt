package com.chromia.build.tools

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isTrue
import assertk.assertions.support.expected
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.Duration
import java.util.concurrent.TimeUnit

class TestProcess private constructor(processBuilder: ProcessBuilder, startContition: String?, shouldFinish: Boolean, expectedExitCode: Int, timeout: Duration, val verbose: Boolean) : AutoCloseable {

    val process = processBuilder.start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))

    init {
        if (verbose) println("Starting command " +  processBuilder.command().subList(1, processBuilder.command().size))
        if (!startContition.isNullOrBlank()) {
            waitUntil(startContition, timeout)
        }
        if (shouldFinish) {
            process.waitFor(timeout.seconds, TimeUnit.SECONDS)
            assertThat(this).finished(expectedExitCode)
        }
    }
    override fun close() {
        process.destroy()
        process.waitFor(2, TimeUnit.SECONDS)
    }

    private fun readLine(): String? = reader.readLine()
    fun readLines() = reader.readLines()
    fun waitUntil(msg: String, timeout: Duration) {
        var found = false
        val output = mutableListOf<String>()
        val startTime = System.currentTimeMillis()
        while (!found && System.currentTimeMillis() - startTime < timeout.toMillis()) {
            readLine()?.let {
                output.add(it)
                if (verbose) println(it)
                if (it.contains(msg)) {
                    found = true
                }
            }
        }
        if (!found) {
            println("Timed out ($timeout) waiting for message: $msg")
            println("Process output:")
            println(output.joinToString("\n"))
        }
        assertThat(found).isTrue()
    }

    private fun Assert<TestProcess>.finished(exitCode: Int) = given { actual ->
        if (actual.process.exitValue() == exitCode) {
            if (verbose) actual.readLines().forEach { println(it) }
            return
        }
        expected("process to complete successfully, but exit code was ${actual.process.exitValue()} with logs: \n${actual.readLines().joinToString("\n")}")
    }

    class Builder(vararg val args: String) {
        private var config: File? = null
        private var shouldFinish = true
        private var exitCode = 0
        private var timeout = Duration.ofSeconds(30)
        private var startCondition: String? = null
        private var verbose = false
        private var workingDir: File? = null
        fun setConfig(file: File) = apply { config = file }
        fun setWorkingDir(file: File) = apply { workingDir = file }
        fun awaitCompletion(value: Boolean) = apply { shouldFinish = value }
        fun exitCode(value: Int) = apply { exitCode = value }
        fun timeout(value: Duration) = apply { timeout = value }
        fun startCondition(condition: String) = apply { startCondition = condition }
        fun verbose() = apply { verbose = true }


        fun start() = start {}

        fun <R> start(onCompleted: (TestProcess) -> R): R {
            val executable = System.getenv("DIST_EXECUTABLE")
            require(executable.isNotBlank()) { "DIST_EXECUTABLE not set" }
            require(File(executable).exists()) { "Executable $executable not found" }
            val processArgs = buildList<String> {
                add(executable)
                addAll(args)
                config?.let { addAll(listOf("-s", it.absolutePath)) }
            }
            val pb = ProcessBuilder(*processArgs.toTypedArray())
                    .apply {
                        redirectErrorStream(true)
                        workingDir?.let { directory(it) }
                    }
            return TestProcess(pb, startCondition, shouldFinish, exitCode, timeout, verbose).use(onCompleted)
        }
    }
}
