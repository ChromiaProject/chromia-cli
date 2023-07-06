package com.chromia.cli.tools.formatter

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.Theme

val theme = Theme {
    // Use ANSI-16 codes for help colors
    styles["info"] = TextColors.green
    styles["warning"] = TextColors.blue
    styles["danger"] = TextColors.magenta
    styles["muted"] = TextColors.gray

    // Remove the border around code blocks
    flags["markdown.code.block.border"] = false
}