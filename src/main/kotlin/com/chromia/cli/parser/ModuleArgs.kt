package com.chromia.cli.parser

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDictionary
import net.postchain.rell.model.R_App
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.module.GtvToRtContext
import net.postchain.rell.runtime.Rt_Value
import net.postchain.rell.utils.toImmMap

object ModuleArgs {
    fun getModuleArgsValues(app: R_App, moduleArgs: Map<String, Map<String, Gtv>>): Map<R_ModuleName, Rt_Value> {
        return moduleArgs.map {
            val modName = R_ModuleName.of(it.key)
            val module = app.moduleMap.getValue(modName)
            val struct = module.moduleArgs ?: throw IllegalArgumentException("$module does not have any arguments")
            val gtv = GtvDictionary.build(it.value)
            val value = struct.type.gtvToRt(GtvToRtContext.make(pretty = true), gtv)
            modName to value
        }.toMap().toImmMap()
    }

}