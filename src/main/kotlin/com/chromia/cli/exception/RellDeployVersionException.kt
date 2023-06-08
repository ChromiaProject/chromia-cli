package com.chromia.cli.exception

class RellDeployVersionException(compileVersion: String, targetVersion: String) :
        RuntimeException("The local compile version $compileVersion is not supported on the target network. Maximum version allowed is $targetVersion.\n" +
                "The deployment is aborted.")