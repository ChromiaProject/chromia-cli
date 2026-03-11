# Functional Overview

This document explains what Chromia CLI does from a behavior perspective, including features, user flows, and system interactions with references to implementation locations in the codebase.

## High-Level Feature List

### 1. Project Scaffolding
- **Create template projects** with different structures (minimal, plain, multi-module, library, asset-management)
- Implementation: `src/main/kotlin/com/chromia/cli/command/CreateRellDappCommand.kt`

### 2. Code Compilation and Building
- **Build blockchain configurations** from Rell source code
- **Validate Rell code** for compilation errors
- Implementation: `src/main/kotlin/com/chromia/cli/command/BuildCommand.kt`, `CheckCommand.kt`

### 3. Local Development
- **Start a local test node** for development and testing
- **Update running node** with new blockchain configurations
- **REPL** for interactive Rell code execution
- Implementation: `src/main/kotlin/com/chromia/cli/command/node/StartCommand.kt`, `UpdateCommand.kt`, `ReplCommand.kt`

### 4. Testing
- **Run Rell unit tests** and blockchain integration tests
- **SQL query logging** during test execution
- Implementation: `src/main/kotlin/com/chromia/cli/command/TestCommand.kt`

### 5. Deployment Management
- **Deploy blockchains** to Chromia networks (mainnet, testnet, custom)
- **Update deployed blockchains** with schema change detection
- **Manage containers** (pause, resume, remove)
- **Voterset and proposal management** for governance
- Implementation: `src/main/kotlin/com/chromia/cli/command/deployment/`

### 6. Blockchain Interaction
- **Execute queries** against running nodes
- **Submit transactions** with various authentication methods
- **Multi-signature transaction** support for governance operations
- Implementation: `src/main/kotlin/com/chromia/cli/command/QueryCommand.kt`, `TxCommand.kt`, `multisignature/`

### 7. Library Management
- **Install dependencies** from Git repositories or Library Chain
- **Publish libraries** to the Chromia library ecosystem
- **Manage organizations** for library ownership
- Implementation: `src/main/kotlin/com/chromia/cli/command/InstallCommand.kt`, `library/`

### 8. Code Quality
- **Lint Rell code** for potential issues and style violations
- **Format Rell code** automatically
- Implementation: `src/main/kotlin/com/chromia/cli/command/LintCommand.kt`, `FormatCommand.kt`

### 9. Key Management
- **Generate cryptographic key pairs** with mnemonic backup
- **Recover keys** from mnemonic phrases
- **Store keys** with named identifiers
- Implementation: `src/main/kotlin/com/chromia/cli/command/KeygenCommand.kt`

### 10. Code Generation
- **Generate client stubs** for TypeScript, Kotlin, and Python
- **Generate documentation sites** from Rell source code
- **Generate entity relationship diagrams** (Mermaid format)
- Implementation: `src/main/kotlin/com/chromia/cli/command/generate/`

### 11. Test Data Generation (Seeder)
- **Generate fake data** for local testing
- **Configure data generation** rules
- Implementation: `src/main/kotlin/com/chromia/cli/command/seeder/`

---

## Primary User/System Flows

### Project Creation Flow

If a user runs `chr create-rell-dapp myproject --template minimal`:
1. The system validates the project name (no spaces allowed)
2. The system checks if a directory with that name already exists (fails if it does)
3. The system creates the project directory
4. The system generates template files based on the selected template
5. Optionally includes devcontainer configuration if `--devcontainer` flag is used


### Build Flow

If a user runs `chr build`:
1. The system reads `chromia.yml` configuration file
2. The system resolves library RIDs for any installed dependencies
3. The system compiles Rell source code for each configured blockchain
4. The system generates blockchain configuration files (XML or GTV format)
5. Output is written to the `build/` target directory


### Local Node Start Flow

If a user runs `chr node start`:
1. The system reads the node configuration (`.chromia/config`) and blockchain definitions (`chromia.yml`)
2. If `--wipe` is specified, the database schema is cleared
3. For each blockchain:
   - If the blockchain exists in the database, configuration is added at the next height
   - If the blockchain is new, it is initialized from height 0
4. The node starts and begins processing blocks
5. The system outputs "Node is initialized" when all blockchains are started

>**Note**: For `chr node start` a postgres instance is required to be running. See in `Setup.md`

### Test Execution Flow

If a user runs `chr test`:
1. The system identifies test modules from `chromia.yml` (under `test.modules` or `blockchains.<name>.test.modules`)
2. For blockchain tests, the system compiles the main module alongside test modules
3. For unit tests (no blockchain), only test modules are compiled
4. Tests execute against the configured database
5. Results are printed with pass/fail status and duration
6. If `--test-report` is specified, JUnit XML reports are generated
7. For blockchain tests, a running node with `chr node start` needs to be active


### Transaction Submission Flow

If a user runs `chr tx operation_name arg1 arg2`:
1. The system determines the target blockchain from deployment config (`chromia.yml`) or explicit URL/BRID
2. The system parses arguments into GTV format
3. If FT4 authentication is enabled (`--ft-auth`):
   - The system queries for FT4 accounts linked to the signer
   - If multiple accounts exist, an interactive picker is shown
   - The `ft_auth` operation is prepended to the transaction
4. If ICCF is enabled (`--iccf-tx`):
   - The system fetches the proof from the source chain
   - The `iccf_proof` operation is prepended
5. The transaction is posted to the node
6. If `--await` (default), the system polls until confirmation or rejection


### Query Execution Flow

If a user runs `chr query query_name arg1=value`:
1. The system determines the target blockchain from deployment config (`chromia.yml`) or explicit URL/BRID
2. Arguments are parsed as key-value pairs into a GTV dictionary
3. The query is executed against the node
4. Results are formatted according to `--output-format` (pretty, raw, JSON, XML, YAML)


### Deployment Create Flow

If a user runs `chr deployment create`:
1. The system compiles all blockchains defined in `chromia.yml`
2. For each blockchain:
   - If already defined in deployment config, the command fails
   - The system prompts for confirmation (unless `-y` flag is used)
3. The deployment proposal is submitted to the Directory Chain
4. On success, the system outputs configuration to add to `chromia.yml`


### Deployment Update Flow

If a user runs `chr deployment update`:
1. The system compiles the blockchain configuration
2. The system performs schema change detection:
   - Compares entities, objects, and attributes between deployed and new versions
   - Warns on attribute/object/entity removal
   - Shows new attributes and indexes
   - Blocks dangerous changes until approved
3. If validation passes, the update proposal is submitted to the Directory Chain

### Library Installation Flow

If a user runs `chr install`:
1. The system reads library definitions from `chromia.yml`
2. For each library:
   - If from Git: clones the repository at the specified version
   - If from Library Chain: queries and downloads from the chain
3. Libraries are placed in the `lib/` directory


### Multi-Signature Transaction Flow

If a user needs a transaction signed by multiple parties:
1. Creator runs `chr multi-signature create operation_name args... --signers pubkey1,pubkey2`
   - A transaction file is created with the operation and required signers
2. Each signer runs `chr multi-signature sign transaction_file`
   - Adds their signature to the transaction file
3. Final party runs `chr multi-signature send transaction_file`
   - Submits the fully-signed transaction to the blockchain

### REPL Execution Flow

If a user runs `chr repl`:
1. The system starts an interactive Rell shell
2. If `--module` is specified, that module is loaded
3. Users can:
   - Define local variables
   - Execute queries (requires module definition)
   - Execute operations (wrapped in `rell.test.tx(...).run()`)
4. If `--use-db` is specified, queries can access database entities

---

## Important Assumptions

### Configuration File
- The system assumes `chromia.yml` exists in the project root (or specified via `--settings` flag)
- Configuration follows the Chromia model JSON schema
- Read more here: https://docs.chromia.com/build/configuration/project-config

### Database Requirements
- PostgreSQL 16+ is required for most operations involving database persistence
- Database must have `en_US.UTF-8` collation
- Default connection is `jdbc:postgresql://localhost:5432/postchain`
- Read more here: https://docs.chromia.com/get-started/installation#set-up-postgresql-database

### Java Runtime
- Java 21+ is required for all CLI operations
- The system searches standard installation paths if `RELL_JAVA` is not set

### Key Management
- Commands requiring signatures need either:
  - A `--secret` file with private key
  - A configured `key.id` in `.chromia/config`
  - Read more here: https://docs.chromia.com/build/cli/key-pair-management

---

## Business or Technical Constraints

### Rell Version Compatibility
- Deployed blockchains must use Rell versions supported by the target cluster
- The CLI validates version compatibility before deployment

### Authentication Constraints
- FT4 authentication requires the signer to have a linked FT4 account
- EVM authentication opens a browser window for wallet signing
- Multi-signature transactions require all specified signers

### ICCF Constraints
- ICCF operations require the source chain to be accessible
- When using ICCF against a local node, `--directory-chain-mock` must be enabled

---

## Non-Obvious Behavior

### Transaction Await Default
`chr tx` waits for transaction confirmation by default. Use `--no-await` to post asynchronously.

### Configuration Merging
When `chr node start` is run on an existing database, the new configuration is added at the next block height rather than replacing the existing configuration.

### Test Database Schema
Tests run in a separate database schema (`<schema>_tests`) to isolate test data from development data.

### Build Output Formats
- XML format may fail if blockchain configuration contains invalid XML characters
- GTV (binary) format is always safe and recommended for complex configurations

### Library Warnings
By default, library compilation warnings are shown. Use `--hide-lib-warnings` to suppress them. This option is available on multiple commands.

### Deployment Configuration Updates (inferred)
After `chr deployment create` succeeds, the user must manually add the output to `chromia.yml` to track the deployment. The CLI does not auto-update the configuration file.

### Schema Change Detection
`chr deployment update` performs automatic schema change detection and may block deployment if dangerous changes are detected (entity/attribute removal).

### Auto-Completion Regeneration
Shell auto-completion scripts must be regenerated after each CLI version update.

---

## Known Functional Limitations

### Interactive Terminal Requirements
- Some commands require interactive terminals for prompts (deployment confirmations, FT4 account selection)
- Non-interactive mode requires explicit flags (`-y` for deployments)

### Library Chain Operations
- Library publishing requires organization membership on Library Chain
- Some library operations are limited to mainnet/testnet Library Chain instances
- Read more here: https://bitbucket.org/chromawallet/library-chain/

### Windows Support
- Windows is supported for running the CLI but development environment is primarily Unix-based
- Extra setup may be required for Windows development

----