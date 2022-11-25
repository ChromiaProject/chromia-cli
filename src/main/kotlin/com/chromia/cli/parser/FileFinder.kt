package com.chromia.cli.parser

import net.postchain.rell.model.R_ModuleName
import java.io.File

// TODO: Make sure this works in multi-module context
fun findRellFilesInDir(sourceFolder: File): MutableList<R_ModuleName> {
    val tempModules = mutableListOf<R_ModuleName>()
    sourceFolder.walk().forEach {
        if (it.extension == "rell" && !it.isDirectory){
            tempModules.add(R_ModuleName.of(it.nameWithoutExtension))
        }
    }
    return tempModules
}
