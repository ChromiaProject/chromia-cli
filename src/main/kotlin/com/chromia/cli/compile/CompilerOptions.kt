package com.example.rell

import net.postchain.rell.compiler.base.core.C_AtAttrShadowing
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.compiler.base.utils.C_SourcePath
import net.postchain.rell.model.R_LangVersion
import java.util.*

class CompilerOptions(properties: Properties, rellVersion: String) {
    var rellVersion: String = rellVersion
    var gtv: Boolean = "true" == properties["gtv"]
    var deprecatedError: Boolean = "true" ==  properties["deprecatedError"]
    var ide: Boolean = "true" == properties["ide"]
    var blockCheck: Boolean = "true" == properties["blockCheck"]
    var testLib: Boolean = "true" == properties["testLib"]
    var hiddenLib: Boolean = "true" == properties["hiddenLib"]
    var allowDbModificationsInObjectExprs: Boolean = "true" == properties["allowDbModificationsInObjectExprs"]

    fun getCompilerOptions (): C_CompilerOptions {
        return C_CompilerOptions(
            R_LangVersion.of(rellVersion),
            gtv = gtv,
            deprecatedError = deprecatedError,
            ide = ide,
            blockCheck =blockCheck,
            C_AtAttrShadowing.DEFAULT,
            testLib = testLib,
            hiddenLib = hiddenLib,
            allowDbModificationsInObjectExprs = allowDbModificationsInObjectExprs,
            symbolInfoFile = null
        )
    }
}
