package com.chromia.cli.error

class LibraryMismatchException(rid: String, calculatedRid: String, lib: String) :
        RuntimeException("The rid for library $lib does not match the configured value.\nShould be: $rid\nWas: $calculatedRid\nDo not blindly copy the calculated rid as it might be tampered with.")