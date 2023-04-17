package com.chromia.cli.util

fun interface PrintFunction {
    fun print(msg: String)
}

enum class Color(private val code: String) {
    Red("\u001B[31m"),
    Green("\u001B[32m"),
    Blue("\u001B[34m");

    fun format(msg: Any) = "${code}$msg${ANSI_RESET}"

    companion object {
        private const val ANSI_RESET = "\u001B[0m"
    }
}

fun withPrinter(printer: PrintFunction, printFunction: PrintFunction.() -> Unit) {
    printFunction(printer)
}

private val PRINT_SEPARATOR = "-".repeat(72)
internal fun PrintFunction.line() { print(PRINT_SEPARATOR) }
internal fun PrintFunction.space() { print("") }
internal fun PrintFunction.red(msg: String) { print(Color.Red.format(msg)) }
internal fun PrintFunction.green(msg: String) { print(Color.Green.format(msg)) }
internal fun PrintFunction.blue(msg: String) { print(Color.Blue.format(msg)) }
