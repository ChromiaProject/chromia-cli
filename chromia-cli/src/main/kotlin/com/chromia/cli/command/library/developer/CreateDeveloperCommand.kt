package com.chromia.cli.command.library.developer

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.lib.ft4.core.accounts.AuthDescriptor
import com.chromia.lib.ft4.core.accounts.AuthType
import com.chromia.lib.ft4.core.accounts.strategies.open.rasOpenOperation
import com.chromia.lib.ft4.core.accounts.strategies.transfer.open.rasTransferOpenOperation
import com.chromia.library.chain.users.external.createDeveloperOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvNull

enum class AccountStrategy {
    TRANSFER_OPEN,
    OPEN,
}

class CreateDeveloperCommand : AbstractLibraryCommand(name = "create", help = "Create a new library developer") {

    private val name by option("--name", "-n", help = "Name of the library developer").required()
    private val strategy by option(
        "--strategy",
        help = "Account creation strategy"
    )
        .enum<AccountStrategy>()
        .default(AccountStrategy.TRANSFER_OPEN)

    override fun run() {
        registerSingleSigAccount(txBuilder, pubkey, strategy = strategy)

        val res = txBuilder.createDeveloperOperation(name).run {
            addNop()
            postAwaitConfirmation(txListener())
        }

        when (res.status) {
            TransactionStatus.UNKNOWN ->
                echo("transaction with rid ${res.txRid.rid} was posted but has unknown status")

            TransactionStatus.WAITING ->
                echo("transaction with rid ${res.txRid.rid} was posted but is still pending")

            TransactionStatus.CONFIRMED ->
                echo("Developer[$name] created successfully in transaction ${res.txRid}")

            TransactionStatus.REJECTED ->
                throw PrintMessage(
                    "Transaction Failed with code ${res.httpStatusCode}: ${res.rejectReason}",
                    statusCode = 1
                )
        }
    }

    // TODO: validate correct usage of flags
    fun registerSingleSigAccount(
        txBuilder: TransactionBuilder,
        signer: ByteArray,
        flags: Set<String> = setOf("A", "T"),
        strategy: AccountStrategy = AccountStrategy.OPEN
    ) {
        val authDescriptor = AuthDescriptor(
            authType = AuthType.S,
            args = listOf(
                GtvFactory.gtv(flags.map { GtvFactory.gtv(it) }),
                GtvFactory.gtv(signer)
            ),
            rules = GtvNull
        )

        when (strategy) {
            AccountStrategy.TRANSFER_OPEN -> txBuilder.rasTransferOpenOperation(authDescriptor, null)
            AccountStrategy.OPEN -> txBuilder.rasOpenOperation(authDescriptor, disposable = null)
        }
    }
}
