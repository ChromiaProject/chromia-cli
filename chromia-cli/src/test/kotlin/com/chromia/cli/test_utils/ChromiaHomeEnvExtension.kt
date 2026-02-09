package com.chromia.cli.test_utils

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

// Sets CHROMIA_HOME to empty path to isolate test from local key.id configuration in `.chromia/config`
class ChromiaHomeEnvExtension : BeforeAllCallback, AfterAllCallback {
    private lateinit var environmentVariables: EnvironmentVariables

    override fun beforeAll(context: ExtensionContext?) {
            environmentVariables = EnvironmentVariables("CHROMIA_HOME", "empty_path")
            environmentVariables.setup()
    }

    override fun afterAll(context: ExtensionContext?) {
        environmentVariables.teardown()
    }
}