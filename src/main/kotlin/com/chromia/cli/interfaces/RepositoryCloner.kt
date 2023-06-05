package com.chromia.cli.interfaces

import java.io.File

interface RepositoryCloner {
    fun clone(registry: String, target: File)
}