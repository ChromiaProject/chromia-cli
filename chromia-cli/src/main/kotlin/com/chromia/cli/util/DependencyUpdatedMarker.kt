package com.chromia.cli.util

import java.io.File
import java.io.IOException

class DependencyUpdatedMarker(targetDir: File) {
    private val markerFile = targetDir.resolve(".deps")

    fun markAsInstalled() {
        touch(markerFile)
    }

    fun areDependenciesInstalled(): Boolean {
        return markerFile.exists()
    }

    fun clearMarker() {
        markerFile.delete()
    }

    fun getMarkerFile(): File {
        return markerFile
    }

    @Throws(IOException::class)
    private fun touch(file: File) {
        val now = System.currentTimeMillis()
        if (file.exists()) {
            if (!file.setLastModified(now)) {
                throw IOException("Failed to update modification time: ${file.absolutePath}")
            }
        } else {
            file.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    throw IOException("Failed to create parent directories: ${parent.absolutePath}")
                }
            }
            if (!file.createNewFile()) {
                throw IOException("Failed to create file: ${file.absolutePath}")
            }
        }
    }
}
