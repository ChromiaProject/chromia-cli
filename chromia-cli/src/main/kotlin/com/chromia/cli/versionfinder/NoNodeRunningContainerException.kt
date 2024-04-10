package com.chromia.cli.versionfinder

class NoNodeRunningContainerException(container: String) : RuntimeException(
        "No nodes found running the container \"$container\""
)

