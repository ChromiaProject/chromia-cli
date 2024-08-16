package com.chromia.cli.command.deployment.proposal.gtv.diff

interface DiffElement {
    val equals: Boolean
    val diff: String
}