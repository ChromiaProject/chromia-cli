package com.chromia.cli.util

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import java.io.File


fun Terminal.printChromiaYmlDiff(chromiaYmlFile: File, diff: String) {
    if (diff.isEmpty()) return

    val coloredText = TextColors.brightYellow("Updated '${chromiaYmlFile.name}' file")
    val boldText = TextStyles.underline(coloredText)
    this.println("\n$boldText")

    // this is the output of diff, colorize it
    // so that the user can see what changed in 'chromia.yml' file
    diff.split("\n").forEach { line ->
        val coloredLine = when {
            line.startsWith("---") || line.startsWith("+++") -> {
                TextColors.brightMagenta(line)
            }
            line.startsWith("-") -> {
                TextColors.red(line)
            }
            line.startsWith("+") -> {
                TextColors.green(line)
            }
            line.startsWith("@@") -> {
                TextColors.cyan(line)
            }
            else -> line
        }
        this.println(coloredLine)
    }
}
