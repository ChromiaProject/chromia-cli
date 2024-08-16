package com.chromia.cli.command.deployment.proposal

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.util.pubkey
import com.chromia.directory1.proposal.ProposalState
import com.chromia.directory1.proposal.ProposalType
import com.chromia.directory1.proposal.getProposalsRange
import com.chromia.directory1.proposal.getRelevantProposals
import com.chromia.directory1.proposal.voting.getProviderVotes
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import net.postchain.common.types.RowId

class ProposalListCommand : CliktCommand(
        name = "list",
        help = "List all proposals that you can vote on"
) {

    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }

    private val from by dateToTimestampOption("List proposals from date (YYYY-MM-DD)")
    private val to by dateToTimestampOption("List proposals to date (YYYY-MM-DD)", Long.MAX_VALUE, "9999-12-31", 1)
    private val all by option(help = "Include all proposals, including ones you can not vote on").flag()
    private val pending by option(help = "Only include proposals that are still pending").flag()

    override fun run() {
        val clientConfig = settings.config.setApiUrls(networkTarget.url).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)

        val proposals = if (all) {
            client.getProposalsRange(from, to, pending).map { ProposalInfo(it.rowid, it.proposalType, it.state) }
        } else {
            client.getRelevantProposals(from, to, pending, client.pubkey.data).map { ProposalInfo(it.rowid, it.proposalType, it.state) }
        }

        val votes = client.getProviderVotes(from, to, client.pubkey.data)

        echo(defaultTable {
            header { row("Type", "Id", "State", "Your vote") }
            body {
                proposals.map { info ->
                    val vote = votes.find { it.proposal == info.rowId }
                    val voteStatus = if (vote == null) "No vote registered" else if (vote.vote) "Accept" else "Reject"
                    row(info.proposalType.toString(), info.rowId.id.toString(), info.state.toString(), voteStatus)
                }
            }
        }
        )
    }

    private data class ProposalInfo(val rowId: RowId, val proposalType: ProposalType, val state: ProposalState)
}

fun CliktCommand.dateToTimestampOption(helpMessage: String, default: Long = 0, defaultString: String = "1970-01-01", daysOffset: Long = 0) =
        option(help = helpMessage).convert {
            val date = try {
                LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE).plusDays(daysOffset).atStartOfDay(ZoneOffset.systemDefault())
            } catch (e: Exception) {
                fail("$it is not a date on the valid format YYYY-MM-DD")
            }
            Instant.from(date).toEpochMilli()
        }.default(default, defaultString)
