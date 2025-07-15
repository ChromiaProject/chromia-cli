package com.chromia.cli.util

import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DependencyUpdatedMarkerTest {
    @TempDir
    private lateinit var tempDir: File
    private lateinit var dependencyUpdatedMarker: DependencyUpdatedMarker

    @BeforeEach
    fun setup() {
        dependencyUpdatedMarker = DependencyUpdatedMarker(tempDir)
    }

    @Test
    fun `should create marker file when marking dependencies as downloaded`() {
        assertThat(dependencyUpdatedMarker.areDependenciesInstalled()).isFalse()

        dependencyUpdatedMarker.markAsInstalled()

        assertThat(dependencyUpdatedMarker.areDependenciesInstalled()).isTrue()
        assertThat(dependencyUpdatedMarker.getMarkerFile()).exists()
    }

    @Test
    fun `should return false when marker file does not exist`() {
        assertThat(dependencyUpdatedMarker.areDependenciesInstalled()).isFalse()
    }

    @Test
    fun `should clear marker file`() {
        dependencyUpdatedMarker.markAsInstalled()
        assertThat(dependencyUpdatedMarker.areDependenciesInstalled()).isTrue()

        dependencyUpdatedMarker.clearMarker()

        assertThat(dependencyUpdatedMarker.areDependenciesInstalled()).isFalse()
        assertThat(dependencyUpdatedMarker.getMarkerFile().exists()).isFalse()
    }

    @Test
    fun `should return correct marker file path`() {
        val markerFilePath = tempDir.resolve(".deps")
        assertThat(markerFilePath).isEqualTo(dependencyUpdatedMarker.getMarkerFile())
    }

    @Test
    fun `should create parent directories if they do not exist`() {
        val nestedPath = tempDir.resolve("nested/dir")
        val marker = DependencyUpdatedMarker(nestedPath)
        assertThat(nestedPath.exists()).isFalse()

        marker.markAsInstalled()

        assertThat(marker.areDependenciesInstalled()).isTrue()
        assertThat(nestedPath).exists()
    }
}