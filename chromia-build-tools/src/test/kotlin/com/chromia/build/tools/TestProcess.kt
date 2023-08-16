package com.chromia.build.tools

import assertk.assertThat
import assertk.assertions.isTrue
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.Duration
import java.util.concurrent.TimeUnit

class TestProcess private constructor(private val process: Process, val verbose: Boolean) : AutoCloseable {

    val reader = BufferedReader(InputStreamReader(process.inputStream))
    override fun close() = process.destroy()

    fun readLine(): String? = reader.readLine()
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

    class Builder(vararg val args: String) {
        private var config: File? = null
        private var shouldFinish = true
        private var timeout = Duration.ofSeconds(10)
        private var verbose = false
        private var workingDir: File? = null
        fun setConfig(file: File) = apply { config = file }
        fun setWorkingDir(file: File) = apply { workingDir = file }
        fun awaitCompletion(value: Boolean) = apply { shouldFinish = value }
        fun timeout(value: Duration) = apply { timeout = value }
        fun verbose() = apply { verbose = true }


        fun <R> start(onCompleted: (TestProcess) -> R): R {
            val executable = System.getenv("DIST_EXECUTABLE")
            require(executable.isNotBlank()) { "DIST_EXECUTABLE not set" }
            require(File(executable).exists()) { "Executable $executable not found" }
            val processArgs = buildList<String> {
                add(executable)
                addAll(args)
                config?.let { addAll(listOf("-s", it.absolutePath)) }
            }
            val process = ProcessBuilder(*processArgs.toTypedArray())
                    .apply {
                        redirectErrorStream(true)
                        workingDir?.let { directory(it) }
                    }.start()
            if (shouldFinish) process.waitFor(timeout.seconds, TimeUnit.SECONDS)
            return TestProcess(process, verbose).use(onCompleted)
        }
    }
}
