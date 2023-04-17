package com.chromia.cli.util

import com.chromia.cli.util.AnsiColorFormat.Companion.ANSI_RESET

interface ColorAware {
    val colorScheme: ColorScheme
}

fun ColorAware.red(msg: String) = colorScheme.printer.print(colorScheme.red.format(msg))
fun ColorAware.green(msg: String) = colorScheme.printer.print(colorScheme.green.format(msg))
fun ColorAware.blue(msg: String) = colorScheme.printer.print(colorScheme.blue.format(msg))
fun ColorAware.line() = colorScheme.printer.print(PRINT_SEPARATOR)
fun ColorAware.space() = colorScheme.printer.print("")

fun interface PrintFunction {
    fun print(msg: String)
}

interface ColorScheme {
    val printer: PrintFunction
    val red: ColorFormat
    val green: ColorFormat
    val blue: ColorFormat
}

interface ColorFormat {
    fun format(str: Any): String

    companion object {
        val noColorFormat = object : ColorFormat {
            override fun format(str: Any) = str.toString()
        }
    }
}

class AnsiColorFormat(private val code: String) : ColorFormat {
    override fun format(str: Any) = "$code$str$ANSI_RESET"

    companion object {
        const val ANSI_RESET = "\u001B[0m"
    }
}

class AnsiColorScheme(override val printer: PrintFunction) : ColorScheme {
    override val red = AnsiColor.Red.colorFormat
    override val green = AnsiColor.Green.colorFormat
    override val blue = AnsiColor.Blue.colorFormat
}

class NoColorScheme(override val printer: PrintFunction) : ColorScheme {
    override val red = ColorFormat.noColorFormat
    override val green = ColorFormat.noColorFormat
    override val blue = ColorFormat.noColorFormat
}

enum class AnsiColor(private val code: String) : ColorFormat {
    Red("\u001B[31m"),
    Green("\u001B[32m"),
    Blue("\u001B[34m");

    val colorFormat = AnsiColorFormat(code)
    override fun format(str: Any): String = "${code}$str${ANSI_RESET}"
}

private val PRINT_SEPARATOR = "-".repeat(72)
