package com.chromia.cli.rell

import net.postchain.rell.model.R_Param
import net.postchain.rell.module.GtvToRtContext
import net.postchain.rell.runtime.Rt_Value
import net.postchain.rell.utils.PostchainUtils
import net.postchain.rell.utils.RellCliErr
import kotlin.system.exitProcess

fun parseArgs(entryPoint: RellEntryPoint, gtvCtx: GtvToRtContext, args: List<String>, json: Boolean): List<Rt_Value> {
    val params = entryPoint.routine().params()
    if (args.size != params.size) {
        System.err.println("Wrong number of arguments: ${args.size} instead of ${params.size}")
        exitProcess(1)
    }
    return args.withIndex().map { (idx, arg) -> parseArg(gtvCtx, params[idx], arg, json) }
}

fun parseArg(gtvCtx: GtvToRtContext, param: R_Param, arg: String, json: Boolean): Rt_Value {
    val type = param.type

    if (json) {
        if (!type.completeFlags().gtv.fromGtv) {
            throw RellCliErr("Parameter '${param.name}' of type ${type.strCode()} cannot be converted from Gtv")
        }
        val gtv = PostchainUtils.jsonToGtv(arg)
        return type.gtvToRt(gtvCtx, gtv)
    }

    try {
        return type.fromCli(arg)
    } catch (e: UnsupportedOperationException) {
        throw RellCliErr("Parameter '${param.name}' has unsupported type: ${type.strCode()}")
    } catch (e: Exception) {
        throw RellCliErr("Invalid value for type ${type.strCode()}: '$arg'")
    }
}