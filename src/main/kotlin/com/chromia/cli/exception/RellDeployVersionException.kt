package com.chromia.cli.exception

import com.github.ajalt.clikt.core.PrintMessage

class RellDeployVersionException(compileVersion: String, targetVersion: String) :
        PrintMessage("The local compile version $compileVersion does not match the network version $targetVersion you are deploying towards.\n" +
                "The deployment is aborted.")