package com.chromia.cli.interfaces

import com.chromia.cli.util.Settings

interface RellVersionControllerInterface {
    fun getTargetVersion(settings: Settings): String
}