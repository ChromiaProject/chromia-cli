package com.chromia.cli.parser

import net.postchain.rell.model.R_ModuleName
import java.io.File

fun removeExtension(fileName: String): String {
    val lastIndex = fileName.lastIndexOf('.')
    if (lastIndex != -1) {
        return fileName.substring(0, lastIndex)
    }
    return fileName
}
fun findRellFilesInDir(sourceFolder: File): MutableList<R_ModuleName> {
    val tempModules = mutableListOf<R_ModuleName>()
    sourceFolder.walk().maxDepth(1).forEach {
        if (it.extension == "rell" && !it.isDirectory){
            tempModules.add(R_ModuleName.of(removeExtension(it.name)))
        }
    }
    return tempModules
}
