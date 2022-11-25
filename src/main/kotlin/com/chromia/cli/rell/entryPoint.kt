package com.chromia.cli.rell

import net.postchain.gtv.Gtv
import net.postchain.rell.model.*
import net.postchain.rell.runtime.Rt_ExecutionContext
import net.postchain.rell.runtime.Rt_OpContext
import net.postchain.rell.runtime.Rt_TxContext
import net.postchain.rell.runtime.Rt_Value
import net.postchain.rell.runtime.utils.Rt_Utils
import net.postchain.rell.utils.RellCliErr
import kotlin.system.exitProcess

sealed class RellEntryPoint {
    abstract val kind: String
    abstract val transaction: Boolean
    abstract fun routine(): R_RoutineDefinition
    abstract fun opContext(): Rt_OpContext?
    abstract fun call(exeCtx: Rt_ExecutionContext, args: List<Rt_Value>): Rt_Value?
}

class Operation(private val o: R_OperationDefinition, private val opCtx: Rt_OpContext): RellEntryPoint() {
    override val kind = "operation"
    override val transaction = true
    override fun routine() = o
    override fun opContext() = opCtx

    override fun call(exeCtx: Rt_ExecutionContext, args: List<Rt_Value>): Rt_Value? {
        o.call(exeCtx, args)
        return null
    }
}

class Query(private val q: R_QueryDefinition): RellEntryPoint() {
    override val kind = "query"
    override val transaction = false
    override fun routine() = q
    override fun opContext() = null

    override fun call(exeCtx: Rt_ExecutionContext, args: List<Rt_Value>): Rt_Value {
        return q.call(exeCtx, args)
    }
}

class Function(private val f: R_FunctionDefinition): RellEntryPoint() {
    override val kind = "function"
    override val transaction = false
    override fun routine() = f
    override fun opContext() = null

    override fun call(exeCtx: Rt_ExecutionContext, args: List<Rt_Value>): Rt_Value {
        return f.callTop(exeCtx, args, true)
    }
}

fun findEntryPoint(app: R_App, moduleName: R_ModuleName, routineName: R_QualifiedName): RellEntryPoint {
    val module = app.modules.find { it.name == moduleName } ?: throw RellCliErr("Module not found: '$moduleName'")
    val name = routineName.str()
    val mountName = R_MountName(routineName.parts)
    val eps = mutableListOf<RellEntryPoint>()

    val op = module.operations[name] ?: app.operations[mountName]
    if (op != null) {
        val time = System.currentTimeMillis() / 1000
        val opCtx = Rt_OpContext(
                txCtx = CliTxContext,
                lastBlockTime = time,
                transactionIid = -1,
                blockHeight = -1,
                opIndex = -1,
                signers = listOf(),
                allOperations = listOf()
        )
        eps.add(Operation(op, opCtx))
    }

    val q = module.queries[name] ?: app.queries[mountName]
    if (q != null) eps.add(Query(q))

    val f = module.functions[name]
    if (f != null) eps.add(Function(f))

    if (eps.isEmpty()) {
        throw RellCliErr("Found no operation, query or function with name '$name'")
    } else if (eps.size > 1) {
        throw RellCliErr("Found more than one definition with name '$name': ${eps.joinToString { it.kind }}")
    }
    return eps[0]
}

private object CliTxContext: Rt_TxContext() {
    override fun emitEvent(type: String, data: Gtv) {
        throw Rt_Utils.errNotSupported("Function emit_event() not supported")
    }
}

fun parseEntryPoint(module: R_ModuleName?, entrypoint: String?): Pair<R_ModuleName?, R_QualifiedName?> {
    if (module == null) { return Pair(null, null) }

    var routineName: R_QualifiedName? = null
    if (entrypoint != null) {
        routineName = R_QualifiedName.ofOpt(entrypoint)
        if (routineName == null || routineName.isEmpty()){throw RellCliErr("Invalid entry point name: '$entrypoint'")}
    }
    return Pair(module, routineName)
}

fun callEntryPoint(exeCtx: Rt_ExecutionContext, rtArgs: List<Rt_Value>, entryPoint: RellEntryPoint): Rt_Value? {
    val res = try {
        entryPoint.call(exeCtx, rtArgs)
    } catch (e: Exception) {
        System.err.println(e)
        exitProcess(1)
    }
    return res
}

