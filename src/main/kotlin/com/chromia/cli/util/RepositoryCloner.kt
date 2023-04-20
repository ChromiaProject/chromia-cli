package com.chromia.cli.util

import java.io.File

interface RepositoryCloner {
    fun clone(registry: String, target: File)
}