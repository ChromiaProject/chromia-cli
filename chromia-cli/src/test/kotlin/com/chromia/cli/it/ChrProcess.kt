package com.chromia.cli.it

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.Duration
import java.util.concurrent.TimeUnit

class ChrProcess private constructor(private val process: Process) : AutoCloseable {

    val reader = BufferedReader(InputStreamReader(process.inputStream))
    override fun close() = process.destroy()

    fun readLine(): String? = reader.readLine()
    fun readLines() = reader.readLines()

    class Builder(vararg val args: String) {
        private var config: File? = null
        private var shouldFinish: Boolean = true
        private var timeout = Duration.ofSeconds(10)
        fun setConfig(file: File) = apply { config = file }
        fun awaitCompletion(value: Boolean) = apply { shouldFinish = value }
        fun timeout(value: Duration) = apply { timeout = value }

        fun <R> start(f: (ChrProcess) -> R): R {
            val processArgs = buildList<String> {
                add("${System.getenv("DIST_DIR")}/chr")
                addAll(args)
                config?.let { addAll(listOf("-s", it.absolutePath)) }
            }
            val process = ProcessBuilder(*processArgs.toTypedArray())
                    .apply {
                        redirectErrorStream(true)
                    }.start()
            if (shouldFinish) process.waitFor(timeout.seconds, TimeUnit.SECONDS)
            return ChrProcess(process).use(f)
        }
    }
}
