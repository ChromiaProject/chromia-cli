package com.chromia.cli.model

import com.chromia.build.tools.model.ensureObject
import com.chromia.build.tools.model.ensureType
import java.io.File
import java.time.Year

data class DocsModel(
        val title: String = "My Rell Dapp",
        val customStyleSheets: List<String>? = null,
        val customAssets: List<String>? = null,
        private val additionalContent: List<String>? = null,
        val footerMessage: String = FOOTER_DEFAULT,
        val sourceLink: SourceLink? = null,
) {
    val additionalContentFiles get() = additionalContent?.map { File(it) } ?: listOf()

    companion object {
        val FOOTER_DEFAULT = "© ${Year.now()} Copyright"
        fun load(data: Map<String, Any>) = DocsModel(
                title = ensureDocsType<String>(data, "title"),
                customStyleSheets = ensureDocsType<List<String>?>(data, "customStyleSheets"),
                customAssets = ensureDocsType<List<String>?>(data, "customAssets"),
                additionalContent = ensureDocsType<List<String>?>(data, "additionalContent"),
                footerMessage = ensureDocsType<String?>(data, "footerMessage") ?: FOOTER_DEFAULT,
                sourceLink = ensureObject<SourceLink?>(data["sourceLink"], SourceLink::load, null, "sourceLink"),
        )

        private inline fun <reified T> ensureDocsType(data: Map<String, Any>, key: String) =
                ensureType<T>(data[key], "docs", key)
    }
}
