package com.chromia.cli.exception

import com.github.ajalt.clikt.core.PrintMessage

class LibraryMisMatchException(rid: String, calculatedRid: String, lib: String) :
        PrintMessage("The rid for library $lib does not match the configured value.\nShould be: $rid\nWas: $calculatedRid\nDo not blindly copy the calculated rid as it might be tampered with.")