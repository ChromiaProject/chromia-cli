/*
This code is copied from [Mordant](https://github.com/ajalt/mordant)
since it is internal there.
*/

/*
Copyright 2018 AJ Alt

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.chromia.cli.tools.mordant

import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.VerticalAlign
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.CellBuilder
import com.github.ajalt.mordant.table.CellStyleBuilder
import com.github.ajalt.mordant.table.ColumnBuilder
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.MordantDsl
import com.github.ajalt.mordant.table.RowBuilder
import com.github.ajalt.mordant.table.SectionBuilder
import com.github.ajalt.mordant.table.TableBuilder
import com.github.ajalt.mordant.widgets.Padding
import com.github.ajalt.mordant.widgets.Text

internal val DEFAULT_STYLE = TextStyle(
    color = null,
    bgColor = null,
    bold = false,
    italic = false,
    underline = false,
    dim = false,
    inverse = false,
    strikethrough = false,
    hyperlink = null,
)

private class CellStyleBuilderMixin : CellStyleBuilder {
    override var padding: Padding? = null
    override var style: TextStyle? = null
    override var cellBorders: Borders? = null
    override var whitespace: Whitespace? = null
    override var align: TextAlign? = null
    override var verticalAlign: VerticalAlign? = null
    override var overflowWrap: OverflowWrap? = null
}

internal class ColumnBuilderInstance : ColumnBuilder, CellStyleBuilder by CellStyleBuilderMixin() {
    override var width: ColumnWidth = ColumnWidth.Auto
}

@MordantDsl
internal class TableBuilderInstance : TableBuilder, CellStyleBuilder by CellStyleBuilderMixin() {
    override var borderType: BorderType = BorderType.SQUARE
    override var borderStyle: TextStyle = DEFAULT_STYLE
    override var tableBorders: Borders? = null
    override var addPaddingWidthToFixedWidth: Boolean = true

    val columns = mutableMapOf<Int, ColumnBuilder>()
    val headerSection = SectionBuilderInstance()
    val bodySection = SectionBuilderInstance()
    val footerSection = SectionBuilderInstance()
    var captionTop: Widget? = null
        private set
    var captionBottom: Widget? = null
        private set

    override fun captionTop(widget: Widget) {
        captionTop = widget
    }

    override fun captionTop(text: String, align: TextAlign) {
        captionTop(Text(text, align = align))
    }

    override fun captionBottom(widget: Widget) {
        captionBottom = widget
    }

    override fun captionBottom(text: String, align: TextAlign) {
        captionBottom(Text(text, align = align))
    }

    override fun column(i: Int, init: ColumnBuilder.() -> Unit) {
        initColumn(columns, i, ColumnBuilderInstance(), init)
    }

    override fun header(init: SectionBuilder.() -> Unit) {
        headerSection.init()
    }

    override fun body(init: SectionBuilder.() -> Unit) {
        bodySection.init()
    }

    override fun footer(init: SectionBuilder.() -> Unit) {
        footerSection.init()
    }
}

@MordantDsl
internal class SectionBuilderInstance : SectionBuilder,
        CellStyleBuilder by CellStyleBuilderMixin() {
    val rows = mutableListOf<RowBuilderInstance>()
    val columns = mutableMapOf<Int, CellStyleBuilder>()
    var rowStyles = listOf<TextStyle>()

    override fun column(i: Int, init: CellStyleBuilder.() -> Unit) =
            initColumn(columns, i, ColumnBuilderInstance(), init)

    override fun rowStyles(style1: TextStyle, style2: TextStyle, vararg styles: TextStyle) {
        rowStyles = listOf(style1, style2) + styles.asList()
    }

    override fun rowFrom(cells: Iterable<Any?>, init: RowBuilder.() -> Unit) {
        val cellBuilders = cells.mapTo(mutableListOf()) { CellBuilderInstance(getCellContent(it)) }
        rows += RowBuilderInstance(cellBuilders).apply(init)
    }

    override fun row(vararg cells: Any?, init: RowBuilder.() -> Unit) {
        rowFrom(cells.asList(), init)
    }

    override fun row(init: RowBuilder.() -> Unit) {
        rows += RowBuilderInstance(mutableListOf()).apply(init)
    }
}

@MordantDsl
internal class RowBuilderInstance(
        val cells: MutableList<CellBuilderInstance>,
) : RowBuilder, CellStyleBuilder by CellStyleBuilderMixin() {
    override fun cells(cell1: Any?, cell2: Any?, vararg cells: Any?, init: CellBuilder.() -> Unit) {
        cell(cell1, init)
        cell(cell2, init)
        cellsFrom(cells.asList(), init)
    }

    override fun cellsFrom(cells: Iterable<Any?>, init: CellBuilder.() -> Unit) {
        cells.mapTo(this.cells) { CellBuilderInstance(getCellContent(it)).apply(init) }
    }

    override fun cell(content: Any?, init: CellBuilder.() -> Unit) {
        cells += CellBuilderInstance(getCellContent(content)).apply(init)
    }
}

internal sealed class CellContent {
    data class WidgetContent(val widget: Widget) : CellContent()
    data class TextContent(val text: String) : CellContent()
}

@MordantDsl
internal class CellBuilderInstance(
        val content: CellContent,
) : CellBuilder, CellStyleBuilder by CellStyleBuilderMixin() {

    override var columnSpan = 1
        set(value) {
            require(value > 0) { "Column span must be greater than 0" }
            field = value
        }

    override var rowSpan = 1
        set(value) {
            require(value > 0) { "Row span must be greater than 0" }
            field = value
        }
}

private fun getCellContent(content: Any?): CellContent {
    if (content is Widget) return CellContent.WidgetContent(content)
    return CellContent.TextContent(content.toString())
}

private fun <T : CellStyleBuilder> initColumn(
        columns: MutableMap<Int, T>,
        i: Int,
        def: T,
        init: T.() -> Unit,
) {
    require(i >= 0) { "column index cannot be negative" }
    var v = columns[i]
    if (v == null) {
        v = def
        columns[i] = v
    }
    v.init()
}
