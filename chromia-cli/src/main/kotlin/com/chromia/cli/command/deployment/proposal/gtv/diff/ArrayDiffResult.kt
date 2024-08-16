package com.chromia.cli.command.deployment.proposal.gtv.diff

class ArrayDiffResult(val res: List<DiffElement>) : DiffElement {
    override val equals: Boolean
        get() = res.isEmpty()
    override val diff: String
        get() = res.joinToString("\n") { it.diff }
}
