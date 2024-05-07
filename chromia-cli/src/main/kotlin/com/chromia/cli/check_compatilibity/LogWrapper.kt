package com.chromia.cli.check_compatilibity

/**
 * This is used to delegate the echo function from Clikt, use it together with Rells echo function and control
 * verbose output.
 */
class LogWrapper(
        val verbose: Boolean = true,
        private val mainEcho: (
                message: Any?,
                trailingNewline: Boolean,
                err: Boolean,
        ) -> Unit = { m, _, _ -> println(m.toString()) }
) {
    fun echo(
            message: Any?,
            trailingNewline: Boolean = true,
            err: Boolean = false
    ) {
        mainEcho(message, trailingNewline, err)
    }

    fun echo(message: String, err: Boolean = false, verbose: Boolean = false) {
        if (!verbose || this.verbose) {
            mainEcho(message, true, err)
        }
    }

    fun phase(message: String) {
        mainEcho("# $message", true, false)
    }

    fun verbose(message: String) {
        echo(message, verbose = true)
    }
}