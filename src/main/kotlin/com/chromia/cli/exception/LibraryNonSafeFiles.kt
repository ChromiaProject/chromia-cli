package com.chromia.cli.exception

import com.github.ajalt.clikt.core.PrintMessage

class LibraryNonSafeFiles(lib: String) :
        PrintMessage("The library $lib contains files that has non rell type files. Can not verify integrity")