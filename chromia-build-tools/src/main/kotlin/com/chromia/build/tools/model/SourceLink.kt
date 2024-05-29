package com.chromia.cli.model

import com.chromia.build.tools.model.ensureType

data class SourceLink(val remoteUrl: String, val remoteLineSuffix: String? = null) {
    companion object {
        fun load(data: Map<String, Any>) = SourceLink(
                remoteUrl = ensureType<String>(data["remoteUrl"], "docs", "sourceLink"),
                remoteLineSuffix = ensureType<String?>(data["remoteLineSuffix"], "docs", "sourceLink"),
        )
    }
}
