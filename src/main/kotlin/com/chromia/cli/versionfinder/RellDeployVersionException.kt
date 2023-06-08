package com.chromia.cli.versionfinder

import net.postchain.rell.base.model.R_LangVersion

class RellDeployVersionException(compileVersion: String, targetVersion: R_LangVersion) :
        RuntimeException("The local compile version $compileVersion is not supported on the target network. Maximum version allowed is $targetVersion.\n" +
                "The deployment is aborted.")