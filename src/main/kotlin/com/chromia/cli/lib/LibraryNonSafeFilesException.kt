package com.chromia.cli.lib

import java.io.File

class LibraryNonSafeFilesException(lib: String, files: List<File>) :
        RuntimeException("The library $lib contains files that has non rell type files. Can not verify integrity." +
                "\nAffected files are:${files.map { "\n" + it.path }.toString().replace("[", "").replace("]", "")}"
        )