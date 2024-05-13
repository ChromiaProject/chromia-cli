package com.chromia.cli.util

const val DASH_DASH_DESCRIPTION = """
    If you have an argument name beginning with a underscore '_', it is then required to use '--' to signify the end of options and the start of arguments. 
    All commands after the '--' will then be parsed as arguments.
    """