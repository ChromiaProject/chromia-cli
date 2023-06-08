package com.chromia.cli.lib

class LibraryNonSafeFilesException(lib: String) :
        RuntimeException("The library $lib contains files that has non rell type files. Can not verify integrity")