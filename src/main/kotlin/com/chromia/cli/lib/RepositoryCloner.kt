package com.chromia.cli.lib

import java.io.File

interface RepositoryCloner {
    fun clone(registry: String, target: File, branch: String?)
}