package com.chromia.build.tools.icmf

import net.postchain.base.BaseBlockBuilderExtension
import net.postchain.common.exception.UserMistake
import net.postchain.core.EContext
import net.postchain.core.Transactor
import net.postchain.core.TxEContext
import net.postchain.gtv.Gtv
import net.postchain.gtx.GTXModule
import net.postchain.gtx.GTXOperation
import net.postchain.gtx.OperationWrapper
import net.postchain.gtx.TransactorMaker
import net.postchain.gtx.data.ExtOpData

class InMemoryIcmfReceiverGtxModule: GTXModule, OperationWrapper {
    private lateinit var delegateTransactorMaker: TransactorMaker

    override fun initializeDB(ctx: EContext) { }
    override fun makeBlockBuilderExtensions() = listOf<BaseBlockBuilderExtension>()
    override fun getQueries() = setOf<String>()
    override fun query(ctxt: EContext, name: String, args: Gtv) = throw UserMistake("Unknown query: $name")

    override fun getOperations() = setOf(IcmfMessageOp.OP_NAME)
    override fun getWrappingOperations() = setOf(IcmfMessageOp.OP_NAME)
    override fun getSpecialTxExtensions() = listOf(InMemoryIcmfReceiverSpecialTxExtension())

    override fun makeTransactor(opData: ExtOpData): Transactor {
        if (opData.opName != IcmfMessageOp.OP_NAME) throw UserMistake("Unknown operation: ${opData.opName}")
        val delegate = delegateTransactorMaker.makeTransactor(opData)
        return InMemoryIcmfReceiveGTXOperation(opData, delegate)
    }

    override fun injectDelegateTransactorMaker(transactorMaker: TransactorMaker) {
        delegateTransactorMaker = transactorMaker
    }

    class InMemoryIcmfReceiveGTXOperation(extOpData: ExtOpData, private val delegate: Transactor): GTXOperation(extOpData) {
        private lateinit var parsedOp: IcmfMessageOp
        override fun checkCorrectness() {
            parsedOp = IcmfMessageOp.fromOpNameAndArgs(data.opName, data.args)!!
            delegate.checkCorrectness()
        }
        override fun apply(ctx: TxEContext): Boolean {
            return delegate.apply(ctx)
        }
    }
}
