package com.chromia.cli.check_compatilibity

import net.postchain.rell.api.gtx.RellApiRunTests
import net.postchain.rell.base.compiler.base.core.C_CompilerOptions
import net.postchain.rell.base.model.R_App
import net.postchain.rell.base.model.R_ModuleName

class CompiledApp(val config: RellApiRunTests.Config,
                  val options: C_CompilerOptions,
                  val app: R_App,
                  val rAppModules: List<R_ModuleName>)
