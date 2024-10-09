package com.chromia.cli.tools.formatter

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.table.TableBuilder
import com.github.ajalt.mordant.table.table

val chromiaTheme = Theme {
    styles["success"] = TextColors.green
    styles["danger"] = TextColors.rgb("#FF405E")
    styles["warning"] = TextColors.rgb("#FFB600")
    styles["info"] = TextColors.rgb("#CB92F0")
    styles["muted"] = TextColors.rgb("#827382")

    styles["header"] = TextColors.rgb("#CB92F0") + TextStyles.bold

    // Remove the border around code blocks
    flags["markdown.code.block.border"] = false
}

val Theme.header get(): TextStyle = style("header")

val CliktCommand.success get() = currentContext.terminal.theme.success
val CliktCommand.danger get() = currentContext.terminal.theme.danger
val CliktCommand.warning get() = currentContext.terminal.theme.warning
val CliktCommand.info get() = currentContext.terminal.theme.info
val CliktCommand.muted get() = currentContext.terminal.theme.muted

fun CliktCommand.defaultTable(init: TableBuilder.() -> Unit) = currentContext.terminal.theme.defaultTable(init)

fun Theme.defaultTable(init: TableBuilder.() -> Unit) = table {
    align = TextAlign.LEFT
    borderType = BorderType.ROUNDED
    borderStyle = muted
    header {
        style = header
    }
    init()
}
