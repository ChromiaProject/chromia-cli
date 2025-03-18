package com.chromia.cli.command.deployment.proposal

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.renderer.AbstractRenderData
import com.chromia.cli.renderer.RendererFactory
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.util.pubkey
import com.chromia.cli.util.tableOutputFormat
import com.chromia.directory1.proposal.ProposalState
import com.chromia.directory1.proposal.ProposalType
import com.chromia.directory1.proposal.getProposalsRange
import com.chromia.directory1.proposal.getRelevantProposals
import com.chromia.directory1.proposal.voting.GetProviderVotesResult
import com.chromia.directory1.proposal.voting.getProviderVotes
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.common.types.RowId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ProposalListCommand : ChromiaCommand(
        name = "list",
        help = "List all proposals that you can vote on"
) {

    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }

    private val from by dateToTimestampOption("List proposals from date (YYYY-MM-DD)")
    private val to by dateToTimestampOption("List proposals to date (YYYY-MM-DD)", Long.MAX_VALUE, "9999-12-31", 1)
    private val all by option(help = "Include all proposals, including ones you can not vote on").flag()
    private val pending by option(help = "Only include proposals that are still pending").flag()
    private val outputFormat by tableOutputFormat()

    override fun run() {
        val clientConfig = settings.config.setApiUrls(networkTarget.urls).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)
        val render = RendererFactory.createRenderer<ProposalListRenderData>(outputFormat, this)


        val proposals = if (all) {
            client.getProposalsRange(from, to, pending).map {
                ProposalInfo(it.rowid, it.proposalType, it.state)
            }
        } else {
            val pubkey: ByteArray
            try {
                pubkey = client.pubkey.data
            } catch (e: NoSuchElementException) {
                throw CanNotFindPubkeyException()
            }

            client.getRelevantProposals(from, to, pending, pubkey)
                    .map {
                        ProposalInfo(it.rowid, it.proposalType, it.state)
                    }
        }

        val votes = client.getProviderVotes(from, to, client.pubkey.data)
        val renderData = ProposalListRenderData(proposals, votes)
        render.display(renderData)
    }
}

data class ProposalListRenderData(val proposals: List<ProposalInfo>, val votes: List<GetProviderVotesResult>) : AbstractRenderData()
data class ProposalInfo(val rowId: RowId, val proposalType: ProposalType, val state: ProposalState)

fun CliktCommand.dateToTimestampOption(helpMessage: String, default: Long = 0, defaultString: String = "1970-01-01", daysOffset: Long = 0) =
        option(help = helpMessage).convert {
            val date = try {
                LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE).plusDays(daysOffset).atStartOfDay(ZoneOffset.systemDefault())
            } catch (e: Exception) {
                fail("$it is not a date on the valid format YYYY-MM-DD")
            }
            Instant.from(date).toEpochMilli()
        }.default(default, defaultString)
