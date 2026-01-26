# Testing Guide

This document describes the testing infrastructure and patterns used in the Chromia CLI project, with a focus on how to mock blockchain responses for unit and integration tests.

## Table of Contents

- [Overview](#overview)
- [Test Types](#test-types)
- [Mocking Blockchain Responses](#mocking-blockchain-responses)
- [Test Data Creation](#test-data-creation)
- [Best Practices](#best-practices)

## Overview

The Chromia CLI test suite uses a mocking structure to simulate blockchain responses without requiring an actual running blockchain node. This enables fast, reliable unit tests that can verify CLI command behavior against controlled mock data.

The core testing pattern involves:
1. Creating mock `Model` implementations that return predefined responses
2. Using `withModel()` to temporarily route API calls to these mocks
3. Creating test data (source files, configurations) using helper utilities
4. Executing CLI commands and asserting on their output

## Test Types

The project contains several types of tests:

| Test Type | Location | Naming | Description |
|-----------|----------|--------|-------------|
| Unit Tests | `src/test/kotlin/.../command/` | `*Test.kt` | Fast tests using mock models |
| Integration Tests | `src/test/kotlin/.../it/` | `*IT.kt` | Tests that may use real processes |
| Slow Integration | `src/test/kotlin/` | `*SlowIntegrationTest.kt` | Long-running tests |

## Mocking Blockchain Responses

The mocking system is built around two key components:

1. **`Model` interface** (from Postchain): Defines the contract for blockchain API interactions
2. **`withModel()`**: A scoping function that routes API calls to mock models during test execution

Custom mock models use Kotlin's delegation pattern (`class MyModel(val model: Model) : Model by model`) to inherit default behavior while overriding specific methods. The `withModel()` function starts a local HTTP server that routes requests to your mock model's methods.

Here's a complete example showing how to create a mock model and use it in a test:

```kotlin
import com.chromia.build.tools.restapi.RestApiInstance.withModel

// Create a custom mock model using delegation
class MyCustomModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModel(blockchainRid))

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "api_version" -> gtv(33)
            "hello_world" -> gtv("Hello People!")
            "get_user" -> gtv(mapOf(
                "id" to gtv(1),
                "name" to gtv("Alice")
            ))
            else -> throw IllegalArgumentException("Unknown query: ${query.name}")
        }
    }
    
    override fun queryWithHeight(query: GtxQuery): Pair<Gtv, Long> = query(query) to 0
    
    override fun getCurrentBlockHeight(): BlockHeight = BlockHeight(569889)
    
    override fun getStatus(txRID: TxRid) = ApiStatus(TransactionStatus.CONFIRMED)
}

// Use the mock in a test
@Test
fun testSomeFeature() {
    withModel(MyCustomModel(BlockchainRid.ZERO_RID)) {
        val result = SomeCommand().test(listOf("--some-option"))
        assertThat(result.stdout).contains("expected output")
    }
}

// For multiple blockchains (e.g., Directory Chain + deployed dapp)
@Test
fun testWithMultipleChains() {
    withModel(
        DirectoryChainModel(directoryBrid),
        DeployedChainModel(targetChainBrid)
    ) {
        // Requests are routed based on blockchain RID
        DeployInfoCommand().test(listOf("-brid", targetChainBrid))
    }
}
```

## Test Data Creation

The `testData()` function from `chromia-build-tools` provides a DSL for creating test project structures:

```kotlin
import com.chromia.build.tools.testData

@Test
fun testWithCustomProjectStructure(@TempDir dir: Path) {
    testData(dir) {
        // Add source files
        addSourceFile("main.rell", """
            module;
            query hello() = "Hi!";
            operation call_op(value: integer) {}
        """.trimIndent())
        
        addSourceFile("lib/my_lib/module.rell", """
            module;
            query lib_hello() = "Hello from lib!";
        """.trimIndent())
        
        // Configure chromia.yml
        config {
            blockchains("""
                blockchains:
                  hello:
                    module: main
            """.trimIndent())
            
            deployments("""
                deployments:
                  $CHROMIA_PREDEFINED_TESTING_NETWORK:
            """.trimIndent())
        }
        
        // Create secret file
        secret {
            secretFile(dir)
        }
    }
    
    // Run tests with the created project structure
    withModel(SomeModel(BlockchainRid.ZERO_RID)) {
        val result = SomeCommand().test(listOf("-s", dir.resolve("chromia.yml").toString()))
        // assertions...
    }
}
```
