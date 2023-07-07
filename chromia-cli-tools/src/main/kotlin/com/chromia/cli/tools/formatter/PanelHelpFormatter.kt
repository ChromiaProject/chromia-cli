package com.chromia.cli.tools.formatter

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.output.HelpFormatter
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.widgets.Panel

class PanelHelpFormatter(context: Context) : MordantHelpFormatter(context, showDefaultValues = true, requiredOptionMarker = "*") {
    // You can override which styles are used for each part of the output.
    // If you want to change the color of the styles themselves, you can set them in the terminal's
    // theme (see the main function below).
    override fun styleSectionTitle(title: String): String = title

    // Print section titles like "Options" instead of "Options:"
    override fun renderSectionTitle(title: String): String = title

    // Print metavars like INT instead of <int>
    override fun normalizeParameter(name: String): String = name.uppercase()

    // Put each parameter section in its own panel
    override fun renderParameters(
            parameters: List<HelpFormatter.ParameterHelp>,
    ): Widget = verticalLayout {
        width = ColumnWidth.Expand()
        for (section in collectParameterSections(parameters)) {
            cell(
                    Panel(
                            section.content,
                            section.title,
                            expand = true,
                            titleAlign = TextAlign.LEFT,
                            borderStyle = theme.style("muted")
                    )
            )
        }
    }
}