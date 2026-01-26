# Technical Architecture & Codebase

## High-Level Architecture

Chromia CLI is a Kotlin-based command-line application that acts as the primary developer interface for the Chromia blockchain ecosystem. The architecture follows a **command-based design pattern** where functionality is organized into discrete, composable commands that can be combined through subcommands.

The CLI operates as a **bridge layer** between developers and multiple upstream Chromia services:
- Postchain Client
- Rell
- Postchain Base
- Directory Chain
- Library Chain
- And more
---

## Major Components/Modules

### 1. Command Layer (`com.chromia.cli.command/`)

The command layer contains all CLI commands, organized hierarchically. Commands extend either `ChromiaCommand` or `NoOpChromiaCommand` (for parent commands that only group subcommands).

| Command Group | Responsibility |
|--------------|----------------|
| `deployment/` | Blockchain deployment lifecycle: create, update, pause, resume, remove, proposals, voter sets |
| `library/` | Library management: install, publish, versioning, developer/organization management |
| `node/` | Local node operations: start test nodes, update running nodes |
| `generate/` | Code generation: client stubs (TypeScript, Kotlin, Python, JavaScript), Mermaid diagrams, documentation |
| `multisignature/` | Multi-signature transaction workflow: create, sign, view, send |
| `code/` | Code quality: linting, formatting, checking |
| `tools/` | Miscellaneous utilities: GTV decoding, model validation |
| `seeder/` | Test data generation for development and testing |
| `eif/` | Ethereum Integration Framework: event configuration generation |


### 2. Utility Layer (`com.chromia.cli.util/`)

Provides shared functionality used across multiple commands:

| Module | Responsibility |
|--------|----------------|
| `Options.kt` | Reusable CLI option definitions (output formats, blockchain selection, public keys) |
| `BuildCliEnv.kt` | Environment wrapper for Rell compilation with warning categorization and error tracking |
| `blockchain.kt` | Blockchain configuration manipulation (GTX module filtering, config file parsing) |
| `client.kt` | Postchain client utilities and FT4 authentication initialization |
| `deployTargetOptions.kt` | Network targeting options for deployment commands |


### 3. Renderer Layer (`com.chromia.cli.renderer/`)

Handles output formatting with strategy pattern:

- `RendererFactory`: Creates appropriate renderer based on output format and command context
- Supports both table (interactive terminals) and JSON (scripting/pipelines) output formats
- Auto-detects terminal capabilities to choose optimal format

### 4. Schema Layer (`com.chromia.cli.schema/`)

Provides blockchain schema analysis and comparison:

- `SchemaComparator`: Compares entity and enum schemas between versions to detect breaking changes
- `BlockchainConfigSchemaParser`: Parses blockchain configuration to extract schema definitions
- `ReportGenerator`: Generates human-readable reports of schema differences

**Use Case**: During deployment updates, the CLI uses schema comparison to warn developers about potentially dangerous changes (e.g., enum ordinal shifts, field removals).

### 5. SQL Statistics (`com.chromia.cli.sql/`)

Collects and renders SQL query statistics during test execution:

- `SqlStatisticsCollector`: Aggregates SQL execution metrics per test case
- `SqlStatisticsRenderer`: Formats statistics for terminal output
- Supports filtering by query type (USER, SYSTEM, BOTH)

---

## Component Communication

### Command Execution Flow

```
main.kt (entry point)
    │
    ▼
CliLauncher (from chromia-cli-tools)
    │
    ├─▶ Parse command line arguments (Clikt library)
    │
    ├─▶ Resolve command hierarchy
    │
    └─▶ Execute selected command's run() method
            │
            ├─▶ Load chromia.yml configuration (chromia-build-tools)
            │
            ├─▶ Initialize Postchain client (if network operations needed)
            │
            └─▶ Perform command-specific logic
```

### Deployment Flow (Example)

```
DeployCreateCommand.run()
    │
    ├─▶ chromiaModelConfigOption() → Load/validate chromia.yml
    │
    ├─▶ DeployedNetworkOption → Resolve target network from deployments config
    │
    ├─▶ keyPairSourceOption() → Load signing keys
    │
    ├─▶ ChromiaCompileApi.build() → Compile Rell code
    │       │
    │       └─▶ LibraryRidResolver → Resolve library dependencies
    │
    ├─▶ validateRellVersion() → Query nodes for version compatibility
    │       │
    │       └─▶ PostchainRellVersionFinder → Connect to cluster nodes
    │
    └─▶ performDeploymentOperation() → Submit deployment transaction
            │
            └─▶ PostchainClient → Send TX to Directory Chain
```

---


## Key Frameworks, Libraries, and Versions

### Core Dependencies

| Library | Purpose |
|---------|---------|
| Kotlin | Primary language |
| JVM Target | Runtime platform |
| Clikt | Command-line parsing and help generation |
| Mordant | Terminal output formatting and colors |

### Chromia Ecosystem

| Library | Purpose |
|---------|---------|
| Postchain | Blockchain infrastructure |
| Postchain-Chromia | Chromia-specific Postchain extensions |
| Postchain Client | Network communication |
| Rell | Rell language compiler and runtime |
| Rell Toolbox | Code quality, seeding, indexing |
| Codegen | Client code generation |
| EIF | Ethereum Integration Framework |
| Directory Chain | Network directory service |
| Chromia CLI Tools | Shared build and configuration utilities |

### Supporting Libraries

| Library | Purpose |
|---------|---------|
| JGit | Git operations for library installation |
| Jackson | JSON/YAML serialization |
| http4k | HTTP client and server |
| Result4k | Functional error handling |
| Log4j2 | Logging |

### Testing

| Library | Purpose |
|---------|---------|
| JUnit 5 | Test framework |
| MockK 1.13.13 | Kotlin mocking |
| AssertK 0.26.1 | Fluent assertions |
| System Stubs Jupiter | Environment variable mocking |

### Build Tools

| Tool | Purpose |
|------|---------|
| Maven | Build system |
| JaCoCo | Code coverage |
| Jib | Docker image building |
| OWASP Dependency Check | Security vulnerability scanning |

---

## Generated Sources

During build, the `rell-maven-plugin` generates Kotlin client code from:
1. **Directory Chain** (`directory-chain/src`) → `com.chromia.directory1`
2. **Library Chain** (`library-chain/src`) → `com.chromia.library.chain`

These generated clients provide type-safe access to D1 and Library Chain queries/operations.
