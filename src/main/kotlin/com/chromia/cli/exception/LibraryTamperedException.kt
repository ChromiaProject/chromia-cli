package com.chromia.cli.exception

import com.github.ajalt.clikt.core.PrintMessage

class LibraryTamperedException(rid: String, lib: String) :
        PrintMessage("The rid $rid for library $lib does not match the calculated rid from the downloaded library, can not verify it has not be tampered with")