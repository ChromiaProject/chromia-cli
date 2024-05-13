package com.chromia.build.tools.lib

import java.nio.file.Path

interface RepositoryCloner {
    fun clone(registry: String, target: Path, tagOrBranch: String?)
}