package com.chromia.cli.tools.formatter

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.table.TableBuilder
import com.github.ajalt.mordant.table.table

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


fun CliktCommand.defaultTable(init: TableBuilder.() -> Unit) = currentContext.terminal.theme.defaultTable(init)

fun Theme.defaultTable(init: TableBuilder.() -> Unit) = table {
    align = TextAlign.LEFT
    borderType = BorderType.ROUNDED
    header {
        style = info + TextStyles.bold
    }
    init()
}
