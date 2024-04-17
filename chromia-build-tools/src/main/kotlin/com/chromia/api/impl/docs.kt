package com.chromia.api.impl

import com.chromia.cli.model.ChromiaModel
import com.chromia.rell.dokka.RellDokkaGenerator
import com.chromia.rell.dokka.config.RellDokkaPluginConfigurationBuilder
import net.postchain.rell.api.base.RellCliEnv
import java.nio.file.Path

fun docsSite(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path, targetDir: Path) {
    val docsModel = model.docs
    val configBuilder = RellDokkaPluginConfigurationBuilder(
            title = docsModel.title,
            modules = model.blockchains.values.map { it.module },
            projectRoot = model.compile.sourceFile(projectDir.toFile())
    )
            .customStyleSheets(docsModel.customStyleSheets)
            .customAssets(docsModel.customAssets)
            .includes(docsModel.additionalContentFiles)
            .footerMessage(docsModel.footerMessage)
            .targetFolder(targetDir.toFile())
    RellDokkaGenerator(configBuilder).generate()
    cliEnv.print("Documentation generated at $targetDir")
}
