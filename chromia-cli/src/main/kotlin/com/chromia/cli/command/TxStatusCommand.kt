package com.chromia.cli.command

import com.chromia.cli.ft.addEvmAuthOperation
import com.chromia.cli.ft.addFtAuthOperation
import com.chromia.cli.ft.findFtAccountIdAndAuthDescriptorId
import com.chromia.cli.ft.initFtAuth
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.LocalDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.secretOption
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import net.postchain.client.core.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.GtvFactory.decodeGtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvString
import net.postchain.gtv.parse.GtvParser


class TxStatusCommand : ChromiaCommand(name = "status", help = "") {

    private val settings by optionalChromiaModelConfigOption()
    private val explicitTarget by LocalDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()
    private val txRid by option()
            .convert { TxRid(it) }
            .required()


    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val postchainClientConfig = settings.config.setApiUrls(target.url).setBrid(target.brid)
        val client = target.createClient(postchainClientConfig)
        val res = client.checkTxStatus(txRid = txRid)
        println(res)
    }
}
