package com.chromia.cli.tools.formatter

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.Theme

val theme = Theme {
    // Use ANSI-16 codes for help colors
    styles["success"] = TextColors.green
    styles["info"] = TextColors.brightBlue
    styles["warning"] = TextColors.brightYellow
    styles["danger"] = TextColors.red
    styles["muted"] = TextColors.gray

    // Remove the border around code blocks
    flags["markdown.code.block.border"] = false
}

val CliktCommand.danger get() = currentContext.terminal.theme.danger
val CliktCommand.success get() = currentContext.terminal.theme.success
val CliktCommand.info get() = currentContext.terminal.theme.info
val CliktCommand.warning get() = currentContext.terminal.theme.warning
val CliktCommand.muted get() = currentContext.terminal.theme.muted
