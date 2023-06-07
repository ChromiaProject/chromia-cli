package com.chromia.cli.exception

class RellDeployVersionException(compileVersion: String, targetVersion: String) :
        RuntimeException("The local compile version $compileVersion does not match the network version $targetVersion you are deploying towards.\n" +
                "The deployment is aborted.")