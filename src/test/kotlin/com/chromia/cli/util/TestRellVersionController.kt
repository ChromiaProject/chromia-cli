package com.chromia.cli.util

import com.chromia.cli.interfaces.RellVersionControllerInterface

class TestRellVersionController : RellVersionControllerInterface {

    override fun getTargetVersion(settings: Settings): String {
        return when (settings.compile.rellVersion) {
            "0.11.0" -> "NO MATCH VERSION"
            else -> {
                return settings.compile.rellVersion
            }
        }
    }
}