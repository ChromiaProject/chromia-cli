package com.chromia.cli.tools.formatter

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

private const val ExpectedTable = """[
  {
    "first_col": "1 1",
    "second_col": "1 2"
  },
  {
    "first_col": "2 1",
    "second_col": "2 2"
  }
]"""

internal class JsonTableTest {
    @Test
    fun jsonTableWithMordantTable() {
        assertThat(
                jsonTable() {
                    header { row("first col", "second col") }
                    body {
                        row("1 1", "1 2")
                        row("2 1", "2 2")
                    }
                }
        ).isEqualTo(ExpectedTable)
    }

    @Test
    fun jsonTableWithListOfLists() {
        assertThat(
                jsonTable(
                        listOf("first col", "second col"),
                        listOf(listOf("1 1", "1 2"), listOf("2 1", "2 2"))
                )).isEqualTo(
                ExpectedTable)
    }
}
