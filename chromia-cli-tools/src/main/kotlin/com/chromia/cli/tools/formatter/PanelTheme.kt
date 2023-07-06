package com.chromia.cli.tools.formatter

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.Theme

val theme = Theme {
    // Use ANSI-16 codes for help colors
    styles["success"] = TextColors.green
    styles["info"] = TextColors.brightBlue
    styles["warning"] = TextColors.red
    styles["danger"] = TextColors.brightYellow
    styles["muted"] = TextColors.gray

    // Remove the border around code blocks
    flags["markdown.code.block.border"] = false
}