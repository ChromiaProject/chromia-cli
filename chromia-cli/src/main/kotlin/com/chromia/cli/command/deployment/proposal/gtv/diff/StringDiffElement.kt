package com.chromia.cli.command.deployment.proposal.gtv.diff

data class StringDiffElement(override val equals: Boolean, override val diff: String) : DiffElement {
    companion object {
        fun equal() = StringDiffElement(true, "")
        fun diff(str: String) = StringDiffElement(false, str)
    }
}
