package com.chromia.build.tools.lib

import java.io.File

interface RepositoryCloner {
    fun clone(registry: String, target: File, tagOrBranch: String?)
}