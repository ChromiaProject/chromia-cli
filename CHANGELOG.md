# Changelog

Structure to follow for writing a new entry:
### Added
### Fixed
### Breaking Changes

Only add section if something can be written under it.
Changelogs will be automatically be generated from this file up to https://docs.chromia.com/cli/cli-release-notes
Which will also take bold or italic text styling.
Keep this in mind when writing and styling any note.

## [0.33.1] - 2026-06-24
### Version Bumps
- postchain 3.49.16
- postchain-chromia 3.39.11
- postchain-client 3.39.5
- eif 0.32.6
- chromia-cli-tools 0.12.1

## [0.33.0] - 2026-05-29
### Version Bumps
- rell 0.16.0

## [0.32.1] - 2026-05-06
### Fixed
- Fix client initialization in chr install when no libraries to install.

## [0.32.0] - 2026-04-19
### Fixed
- Deployment commands now handle all exception types during Rell version validation, resolving a crash that occurred when a node returned invalid GTV data.
### Version Bumps
- rell 0.15.3
- chromia-cli-tools 0.11.3
- rell-maven-plugin 1.2.1
- postchain-client 3.39.1

## [0.31.0] - 2026-03-04
### Added
- `chr deployment proposal retract-vote`: New command to retract a previously cast vote on a deployment proposal.
- `chr test` now generates SQL query statistics as an **HTML report file** alongside other report files, instead of printing to the terminal. The `--sql-log` flag has been removed.
### Fixed
- Improved warning message when an attribute is removed during `chr deployment update`: clearly states that the database column **will be DROPPED** on next initialization and all data will be permanently deleted.
### Version Bumps
- chromia-parent 0.2.4

## [0.30.0] - 2026-02-27
### Added
- `chr deployment create` now writes the deployment result back to `chromia.yml` and prints the changes made.
- `chr build`: Added `--skip-lib-check` flag to skip library verification during build.
- **Enum Change Detection on Deployment Update**
  - Schema comparison during `chr deployment update` now detects enum changes in addition to entity changes.
  - Warns on enum value additions, removals, and reorderings.
  - Blocks deployment on dangerous enum changes (e.g., reordered or removed values) until approved.
- **Version Bumps**
  - rell 0.15.2
  - postchain 3.49.2
  - postchain-chromia 3.39.3
  - eif 0.32.0
  - chromia-cli-tools 0.10.0
### Fixed
- `chr install`: `--brid` and `--url` options now correctly override the target defined in `chromia.yml`.
- Fixed duplicate progress rendering when installing libraries.

## [0.29.10] - 2026-01-20
### Added
- **Version Bumps**
- - Bump EIF to 0.27.3

## [0.29.9] - 2026-01-19
### Added
- **Version Bumps**
- - postchain.version 3.47.6
- - postchain-chromia.version3.38.1


## [0.29.8] - 2026-01-16
### Fixed
- fixed a bug in `chr install` where offset was not being used correctly
### Added
- Schema validation for Enum types
- Dynamically load provider urls instead of hardcoding them
- Remove `url` and `brid` requirement in chromia.yml for deploymentActionCommands
- **Version Bumps**
  - chromia-cli-tools 0.8.14

## [0.29.7] - 2025-12-19
### Fixed
- `chr install` now exits gracefully when no libraries are found in chromia.yml instead of throwing an error

## [0.29.6] - 2025-12-18
### Added
- Library install: Introduced real-time visual feedback for library downloads and installations. 
- **Version Bumps**
    - chromia-cli-tools 0.8.12

## [0.29.5] - 2025-12-10
### Added
- Library install: Options --url and --brid can now be used to override default mainnet option when installing for
libraries defined in `chromia.yml`
- **Version Bumps**
    - rell 0.15.0

## [0.29.4] - 2025-12-01
### Fixed
- Resolved minor issue with library install command

## [0.29.3] - 2025-12-01
### Fixed
- Improved error message for `chr node start` when no blockchain configurations is found


## [0.29.2] - 2025-11-26
### Fixed
- Fix a bug in `chr build` when building a project with Chromia libraries
- Downgrade Rell version to 0.14.15 due to a bug in 0.14.16

## [0.29.1] - 2025-11-24
### Added
- `chr install` will update `libs` tag in chromia.yml
- Add --signer and --signers option to multi-signature create command
- Rell 0.14.16


NOTE: the original YAML structure will stay the same except for indentation

## [0.29.0] - 2025-11-11
### Added
- `chr generate client-stubs` command can now generate stubs for python
- `chr multi-signature create` command accepts no initial signer
- `chr multi-signature create` command accepts signer file to be written without properties 
- Version bumps:
  - chromia-cli.tools 0.8.1

## [0.28.3] - 2025-10-24
### Added
- Add `deployment container configuration` command for toggling logs on container
  - Reorganize container command while keeping aliases for backward compatibility:
    - deployment pause-container -> deployment container pause
    - deployment resume-container -> deployment container resume
- **Version Bumps**
    - directory-chain 1.101.15
    - chromia-cli-tools 0.7.5
### Fixed
- Fix `signers` not set in deployTagetOption

## [0.28.2] - 2025-10-14
### Fixed
- Adds error handling when no urls found for dApp brid from directory chain
- Fix NPE when generating stubs of queries/functions that return unnamed tuples

## [0.28.1] - 2025-10-08
### Fixed
- Predefined urls for create statement

## [0.28.0] - 2025-09-22
### Added
- Automatic blockchain configuration inference
- - When only one blockchain is defined, the configuration is automatically inferred from `<deployments.network>` in `chromia.yml`.
    It is no longer necessary to specify the blockchain explicitly.
    Example: `chr query hello_world --network testnet`. This behavior applies to all relevant commands.

- Default configurations for Chromia networks:
- - Introduced `--mainnet` and `--testnet` flags as alternatives to `--api-url`.
- - When using `--network mainnet` or `--network testnet`, default Chromia configurations are applied automatically.
    Configurations defined under chromia.yml `<deployments.mainnet/testnet>` take precedence if present.
    This removes the need to specify url or brid in `chromia.yml` when targeting Chromia networks.

- **Version Bumps**
    - rell 0.14.15
    - postchain 3.44.0
    - postchain-client 3.38.0

### Fixed
- Will not try to load keys by `key.id` if `--secret` option is used in commands 

## [0.27.11] - 2025-09-22
### Added
- Option to specify argument position to insert ICCF transaction in `chr tx` command
- Add confirmation check when deleting a blockchain from a container in `chr deployment remove` with `-y` flag to auto-confirm
- `--verify-rid` option in `chr library deploy` to verify if the version to be deployed introduces code changes
- **Version Bumps**
  - postchain 3.42.4
  - postchain-client 3.37.1
  - chromia-cli-tools 0.6.5
### Fixed
- Enhanced error output for chr tx --ft-auth failures to include more context.
- Adds validation to check if the 'BRID' provided is valid before creating directoryChain client

## [0.27.10] - 2025-09-08
### Added
- Add `--hide-lib-warnings` option to node commands
- **Version Bumps**
  - chromia-cli-tools 0.6.4

## [0.27.9] - 2025-09-03
### Added
- `--file` option to `chr test` to run all tests in a single Rell file
- `chr code check` command to check Rell code for compilation errors
- `chr install` supports library chains
- `chr library list` command to list available libraries on library chain
- `chr library view` command to view details about specific library on library chain
### Fixed
- Improve how test command deals with --modules option

## [0.27.8] - 2025-08-29
### Added
- Adds marker file after dependency installation to enable IDE reference resolution.
- Print progress while posting transaction and awaiting confirmation
- Support adding timeb operation in tx and multi-signature create commands
- **Version Bumps**
    - chromia-cli-tools 0.6.2
### Fixed
- Inconsistent library warning count and other minor bug fixes

## [0.27.7] - 2025-07-03
### Added
- Adds iccf source url option to `chr tx` command to support iccf tx across different clusters
- Adds `--devcontainer` flag to `chr create-rell-dapp` command to create a project with devcontainer environment
- **Version Bumps**
  - chromia-cli-tools 0.5.16

### Fixed
- NPE when message is null in deploy command
- Update testnet BRID for library commands

## [0.27.6] - 2025-06-23
### Added
- **Version Bumps**
  - rell 0.14.12
  - postchain 3.40.0
  - postchain-client 3.34.0
  - chromia-cli-tools 0.5.15
  - rell-toolbox 0.8.4
  - rell-codegen 0.16.8
  - rell-dokka 0.2.18
  - directory-chain 1.95.0
  - eif 0.16.3

### Fixed
- NPE when message is null in deploy command
- Update testnet BRID for library commands

## [0.27.5] - 2025-06-23
### Fixed
- Remove validation of `registry` in chromia model json schema and other small fixes

## [0.27.4] - 2025-06-20
### Fixed
- Fixed Windows-specific issue with GitCloner in `chr install` and other small improvements
 
## [0.27.3] - 2025-06-19
### Fixed
- Fix `chr install` on Windows. Uniform library RID calculation

## [0.27.2] - 2025-06-11
### Added
- **Version Bumps**
  - rell-codegen 0.16.7
  - chromia-cli-tools 0.5.8
### Fixed
- Fix `chr deployment proposal rename` to target correct blockchain-rid
- Refactor FT auth to use new functionality in ft4-client
- Update jgit to mitigate a vulnerability that affects it

## [0.27.1] - 2025-05-28
### Fixed
- `multi-signature`: Make reading of saved transactions more robust and show an error message if it fails.

### Added
- `--hide-lib-warnings` option to commands that involve compilation
- `--get-pubkey` option to `keygen` command to retrieve pubkey from local/global config or keyid
- **Version Bumps**
  - rell 0.14.11
  - postchain 3.37.0
  - postchain-client 3.30.0
  - chromia-cli-tools 0.5.6
  - rell-toolbox 0.8.3
  - rell-codegen 0.16.6
  - rell-dokka 0.2.17
  - directory-chain 1.94.1
  - eif 0.16.0
  - postchain-chromia 3.30.2

## [0.27.0] - 2025-05-15
### Added
- Seeder tool for generating realistic data for testing Dapps.
- Additional modules support for documentation generation.
- Pause and resume container

## [0.26.4] - 2025-05-12
### Fixed
- Displaying wrong blockchain config merkel hash

## [0.26.3]
### Added
- `chr node start`: More production like ICMF reception error handling
- **Version Bumps**
  - rell 0.14.10
  - postchain 3.34.0
  - postchain-client 3.29.2
  - chromia-cli-tools 0.5.2
  - rell-toolbox 0.7.1
  - rell-codegen 0.16.4
  - rell-dokka 0.2.12

## [0.26.2] - 2025-04-23
### Fixed
- `chr query`: Accept plain RID and decimal as parameter to query command

## [0.26.1] - 2025-04-22
### Added
- `chr kegen`: Set file permissions on private key and mnemonic files
- `chr tx`:  Improve output from tx command
- Improve parsing of GTV expressions passed to commands. Support keywords like `null`, `true`, `false`
- Improve logging. Include `com.chromia` package

### Fixed
- `chr node start` Display correct blockchain BRID when using old configurations
- `chr deployment create` Correct indentation for print statement with multiple successful deployed chains
  
## [0.26.0] - 2025-04-04
### Added
- `chr build`: Added `--hide-lib-warnings` flag to suppress library warnings
- `chr tx`: Added support for registering FT4 accounts with the `--ft-register-account` option
- `chr tx`: Added support for sending intra-network ICCF proofs with the `--iccf-force-intra-network` option

### Breaking Changes
- When using `chr tx` with any ICCF options against a local node (started with `chr node start`), the node must now be running with the directory chain mock enabled via `--directory-chain-mock`

## [0.25.0] - 2025-04-02
### Added
- Integrated merkle hash calculator v2
- Revised multi signature workflow to new merkle hash calculator
- Implemented an automatic fetch for brid in commands where it is required and not defined in `chromia.yml`
- **Version Bumps**
  - rell 0.14.8
  - postchain 3.29.0
  - postchain-client 3.27.0
  - chromia-cli-tools 0.4.15
  - rell-toolbox 0.6.0
  - rell-codegen 0.16.3
  - rell-dokka 0.2.11
  - eif 0.13.1
  - directory-chain 1.83.0


## [0.24.3] - 2025-03-06
### Added
- Adds --evm-auth option to `chr deployment voterset add-dapp-provider` command to support signing transaction with metamask
- Adds the ability to filter on system or user sql queries when logging sql queries while running tests
- ChromiaClientConfig.apiUrls now take list of urls instead of single url to sync with chromia-cli-tools
- **Version Bumps**
  - postchain-client 3.26.2
  - chromia-cli-tools 0.4.7


## [0.24.2] - 2025-03-06
### Added
- Chromia-cli-tools version 0.4.2

## [0.24.1] - 2025-03-04
### Added
- **Streamline usage of key IDs**
  - Option `--key-id` was added to all commands that required keys to be used, making it easier to manage keys.
- **Calculate Merkle hash of the blockchain configuration**
  - Option `--hash` was added to fetch config command to calculate the Merkle hash of the blockchain configuration. 

## [0.24.0] - 2025-02-25
### Added
- **Utility Commands**
  - New command `chr tools validate-config --file <path>` to validate and parse Chromia configuration files.
  - New command `chr tools lib-model` to help library developers get the library configuration, including the RID.
- **Enhanced ICCF Support**
  - Added support for ICCF in directory chain mock chain.
- **Code Formatting Improvements**
  - Added trailing argument to `chr code format` command to be able to add file patterns on what files it should be acted on the filter is a glob matcher.
- **Test Output Enhancement**
  - New output format for SQL logs when running `chr test --sql-log`, utilizing new API from Rell.
- **Version Bumps**
  - Rell version 0.14.7.

### Fixed
- New version of Rell includes bugfix of inconsistent use of hash calculator between operations code and test code
- Renamed option `pubkey` to `account-id` in DeployInspectLeaseCommand.
- Added support for Postchain older than 3.23.2.
- Updated help text for voterset threshold option to clarify how the integer transforms into percentage.

## [0.23.0] - 2025-02-04
### Added
- **Entity/Object Change Detection on Deployment Update**
  - Automatically detects schema changes between the currently deployed dApp and the new one, requiring approval for risky modifications.
  - Warns on attribute/object/entity removal.
  - Shows new attributes and indexes.
  - Blocks deployment on dangerous changes until approved.
- **New Linter Rule**
  - `rule_outer_join_cartesian_product=true` in `.rell_lint` file.
  - Warns about outer joins without join conditions, which result in a Cartesian product.
- **ICMF Receivers**
  - Support all kinds of ICMF receivers for the in-memory ICMF mock when running `chr node start`.
- **Version Bumps**
  - Rell version 0.14.6.
  - Postchain 3.27.3.

## [0.22.4] - 2025-01-27
### Added
- **Automatic Browser Window Closing**
  - When signing transactions using `--evm-auth`, the browser window will now automatically close upon completion.
- **REPL Command Enhancements**
  - Stack traces will now be retained on errors.
  - Added an option to print the duration of executed scripts.
- **Improved Linting with Filters**
  - The `chr lint` command now supports a new multi argument of glob strings that acts as a filter for what files or dirs should be linted.

### Fixed
- **Non-Interactive Terminals**
  - Fixed an issue where users were prompted for input in non-interactive terminals.
- **REPL Command Fixes**
  - Comments in scripts are now properly handled when executing the REPL command.
  - REPL can now be executed in workspaces with a faulty Chromia configuration.
- **Help Text Improvements**
  - Updated help text for the `chr tx` command with clearer instructions on sending different transaction types.

## [0.22.2] - 2024-12-19
### Added
- **Support FT4 EVM auth through web browser**
  - Added support for FT4 EVM authentication via web browser in the `chr tx` command.

## [0.22.0] - 2024-12-11
### Added
- **FT4 Auth Descriptor Selector**
  - Introduces an interactive terminal picker for users with multiple auth descriptors, allowing direct selection of the desired descriptor.
- **Enhanced Test Result Printing**
  - Integrates Rell's test result logger to provide more detailed and clearer failure messages.
- **Experimental chr repl scripts**
  - Extends REPL functionality to accept Rell files as input and support command-line arguments for scripts.

### Fixed
- **Codegen**
  - TypeScript client stubs now fully comply with Gtv type conversions, utilizing postchain-client's RawGtv type instead of generic 'any'.
  - Aligned client stub generation with TypeScript patterns, implementing QueryObject and Operation building approach in place of the previous transaction builder pattern.

## [0.21.4] - 2024-11-18
### Added
- **New template project**
  - Adds a new template project to the `chr create-rell-dapp --template asset-management` that contains a simple frontend to connect a wallet and mint tokens on a Rell backend.
- **ICCF source update**
  - `chr tx --iccf-source` now takes a BRID instead of a chain ID, simplifying the flow and making it easier for users.
- **Help text improvement**
  - Help text for `chr deployment inspect --definitions` now shows available definition types that the user can filter on.
- **Voterset command enhancements**
  - You can now specify either the container ID or specific voterset name in the `voterset info` command.
  - Receive the voterset name from container ID using the `voterset list` command.
- **Multi-signature transaction**
  - It is now possible to rename the output file when signing a transaction file for multi-signature.

### Fixed
- Running `chr keygen` with `--dry` option now works again.
- No longer asking for input when commands are executed in a non-interactive terminal.
- Resolved issue where the parent directory couldn't be found from the working directory in the `multi-signature sign` command.
- Fixed rendering of complex types so it respects indents and newlines without escaping in the `multi-signature view` command.
- Removed creation of aliases to make auto-completion suggestions work again. Note that you would need to redo the auto-completion setup: [Auto-completion setup](https://docs.chromia.com/cli/commands/introduction#set-up-auto-completion).

## [0.21.3] - 2024-11-06
### Added
- **Improved error messages**
- **Static web building**
  - Enables building static web content without Rell sources.
- **GTV data tool**
  - New `chr tools gtv` command added for GTV data decoding and conversion.

### Fixed
- Static web hosting support in `chr node`.
- Prevents invalid XML characters in blockchain configuration.
- Requires explicit file type and name selection for key storage to avoid accidental key overwrites.
- Removes default name of generated file, requiring users to specify a file name or key-id during recovery to prevent accidental key overwriting.
- Corrected help text formatting.

## [0.21.0] - 2024-10-23
### Added
- **Multi-signature transaction support**
  - `chr multi-signature create` for creating multi-signature transactions with specified signers and signature initialization.
  - `chr multi-signature sign` enables users to add signatures with customizable keypair options.
  - `chr multi-signature send` allows submission of fully signed transactions.
  - `chr multi-signature view` to display multi-signature transaction details.
- **Static web content packaging**
  - Static web content can be packaged into blockchain configurations for direct Chromia node serving (requires Postchain 3.21+).
- **Java 21 dependency**
  - Updated package managers now include a Java 21 dependency for `chr` execution. Users can control the Java version via the `RELL_JAVA` environment variable.

### Fixed
- Exit status code is now 1 instead of 0 when the project settings file is not found.
- Prevents escaping of non-ASCII and regular Unicode characters in `query` command raw output.
- **Formatter fixes**
  - Fixed extra space after the first parenthesis in `at` expressions.
  - Doc comments are no longer lifted onto the same line as the previous definition when lacking additional newline.

## [0.20.14] - 2024-09-26
### Added
- **Client stub documentation**
  - Rell docs comments are now transformed into JS/TS/Kotlin docs comments when generating client stubs.
- **Test flag**
  - A new `--timestamp` flag for tests allows including timestamps in terminal output for execution duration tracking.
- **Deployment renaming**
  - New command `chr deployment proposal rename` creates a proposal to change the name of a deployed blockchain.

### Fixed
- Fixed an issue where whitespace was incorrectly added after the first and before the last parenthesis in a multiline `"where"` expression.
- Updated Chromia model to disallow hyphens in blockchain names, ensuring compatibility with directory chain specification.

## [0.20.13] - 2024-09-11
### Added
- **YAML anchoring**
  - Anchored values in the config file now respect the original type when included in attributes.
  - Recursively anchored values in the config file now respect the original type when included in attributes.
- **Formatter improvements**
  - Rell files created with `chr create-rell-dapp` are now formatted according to default settings.

### Fixed
- Removed unsupported SQL modules from the `chromia.yml` schema.
- **Formatter fixes**
  - Fixed an issue where the formatter added an extra newline after a Rell docs comment when followed by an annotation.
  - Resolved an issue where whitespace was incorrectly added after the first and before the last parenthesis in a multiline `"what"` expression.

## [0.20.12] - 2024-08-30
### Added
- **Java 21 upgrade**
  - Upgraded to Java 21, which is now required for `chr`.
- **Rell version update**
  - Updated to Rell 0.14.1.
- **Documentation generation**
  - Excludes all libraries from navigation pages by default when generating docs with `chr generate docs-site`. To include a library, use `--include=lib.<name of lib>`.
- **Custom GTX modules**
  - Allows custom GTX modules in blockchain config to be deployed.
- **New command: `chr deployment voterset add-dapp-provider`**
  - Enables users with the Dapp Provider role to add others to the role within the network.

### Fixed
- Resolved an issue displaying null bridges after failed deployments.
- **Plugins and package upgrades**
  - Updated several plugins and packages:
    - Dokka plugin 0.2.7
    - Rell Maven plugin 0.12.3
    - Rell Gradle plugin 0.3.3
    - Codegen 0.14.2
  - Java 21 and Rell 0.14.1 upgrades applied across plugins.

## [0.20.9] - 2024-08-20
### Added
- **Voterset and proposal functions**
  - Adds two functions (experimental) for interacting with votersets and proposals for deployed dapps:
    - `chr deployment voterset` to manage your own votersets and view others' votersets.
    - `chr deployment proposal` to manage proposals within votersets accessible to your pubkey.
- **Rell compile version**
  - Updated the default Rell compile version to 0.13.14, aligning with mainnet. Custom target versions can be specified in `chromia.yml` under `compile:rellVersion`.

## [0.20.8] - 2024-08-12
### Fixed
- Updated to the latest formatter to fix syntax errors.

## [0.20.7] - 2024-08-12
### Added
- **Code lint and format updates**
  - `chr code lint` and `chr code format` commands now ignore libraries defined in `chromia.yml` by default. To run these commands on external libraries, use the `--source-dir` option to specify the library path.

## [0.20.6] - 2024-08-01
### Added
- **Rell linter and formatter**
  - `chr code lint` analyzes Rell code for potential issues and coding style violations, configurable via `.rell_lint` file.
  - `chr code format` automatically formats Rell code, configurable via `.rell_format` file.

### Fixed
- Documentation generator now includes doc comments of child attributes on the parent page.
- Improved formatter handling of newlines after single-line comments.
- Enhanced error message when attempting to create a deployment for an already defined chain in `chromia.yml`.

## [0.20.4] - 2024-07-11
### Fixed
- Fixed an issue in docs generation where the `@see` tag in user Rell docs caused duplicate text.

## [0.20.3] - 2024-07-08
### Added
- **User-defined docs**
  - `chr generate docs-site` now includes user-defined docs comments in the generated documentation site.

### Fixed
- `chr tx` now waits for transaction confirmation by default. Use `--no-await` to skip waiting.

## [0.20.2] - 2024-06-18
### Added
- **Example usage documentation**
  - Added documentation for `query` and `tx` commands, including examples of how to use complex argument types.

### Fixed
- Fixed code generation for boolean Rell types, now correctly typed as `number` in TypeScript queries and operations (input and return).
- Code generation now exits with a non-zero exit code if unsuccessful.

## [0.20.1] - 2024-06-05
### Added
- **New config property**
  - Added `add_primary_key_to_header` to config, defaulting to `true` (impacts `brid` for chains).
- **JSON output support**
  - `deployment inspect` and `deployment info` commands now support JSON output with the new argument --output-format=(table |JSON), defaulting to table format. Automatically defaults to JSON when output is piped.

### Fixed
- Renamed `--url` flag to `--api-url` for `deployment info/inspect` commands for consistency.
- Improved `chr install` speed by up to 15%.

## [0.20.0] - 2024-05-30
### Added
- **Enhanced output formatting**
  - Added pretty print for `chr query` results.
  - New `--output-format` flag for `chr repl` and `chr query`, supporting JSON, XML, and raw output.
  - `chr repl` command now supports input piping.
- **Configuration validation**
  - `chr deployment update` now signs configuration validation requests to restrict execution to blockchain owners.
- **Documentation generation**
  - `chr generate docs-site` now includes source links in generated documentation.
- **Blockchain RID configuration**
  - Blockchain RIDs can now be configured as either strings or byte arrays in the deployment model.

### Fixed
- Fixed issue where `deployment update` with multiple chains only validated the first chain in the input.
- Clarified JSON schema descriptions for Chromia model and corrected typos.
- Updated deployment commands to use the target network to define default chains if no blockchains are specified.
- Improved TypeScript stubs in `chr generate client-stubs` to better align with `postchain-client`, reducing linter warnings.

## [0.19.1] - 2024-05-16
### Fixed
- Resolved a bug in `chr node start` where library chains caused a null pointer exception (NPE). Library chains are now filtered out from `chr node start` and `chr node update`.

## [0.19.0] - 2024-05-13
### Added
- **Library support**
  - New YAML field (`blockchains::type`) now supports configuration as either `blockchain` or `library`.
  - `chr create-rell-dapp --template plain-library` creates a library skeleton.
  - Libraries can now be compiled by configuring a blockchain as a library, e.g., `blockchains.my_lib.type: library`.
- **Library structure requirements**
  - Libraries must be located in `lib/<name>` and match the blockchain name in the YAML.
  - Root module (`lib.<name>`) must exist, and everything in `lib/<name>` is considered part of the library.
- **Configuration setting update**
  - `revolt_when_should_build_block` is set to `true` by default to align with upcoming directory chain requirements.
- **Dependency updates**
  - Updated dependencies:
    - Rell 0.13.12
    - Postchain 3.15.19
    - Postchain-Chromia 3.15.19

### Fixed
- This version impacts previously calculated blockchain RIDs due to updated configuration.

## [0.18.2] - 2024-05-02
### Added
- **Internal improvements**
  - Added exposed function to easily print configuration properties (internal use).

### Fixed
- Properly reads from the `key.id` property when loading configuration.

## [0.18.1] - 2024-05-02
### Added
- **Key management enhancements**
  - New `--key-id` option for `Keygen` command allows users to store keys in `.chromia` folder in `$SYS_HOME` as `"myKeyId"` and `"myKeyId.pubkey"` (default: `chromia_key`).
  - Config property `key.id` can now be set to easily switch between keys.
  - Renamed `--save` option to `--file` in `Keygen` command.
  - For deployment and transaction commands, `--secret` option takes precedence over `key.id` in configuration.

### Removed
- **Deprecated key recovery removal**
  - Deprecated key recovery for keys generated pre-CLI version 0.15.0 removed. Users should test recovery of their keypair with the mnemonic.
  - Users needing old key pair recovery are recommended to install an older version of Chromia CLI.

## [0.17.4] - 2024-04-16
### Added
- Updated Rell version to [0.13.11](https://gitlab.com/chromaway/rell/-/blob/dev/doc/release-notes/0.14.0.txt?ref_type=heads).

## [0.17.3] - 2024-04-12
### Added
- **Auto-configuration**
  - Automatically sets `mininterblockinterval` based on configuration rules in directory chain.

### Fixed
- Updates `brid` property under `icmf` to `bc-rid` for ICMF runtime compatibility.
- Improved Dokka generation for namespaces and mount names, where namespaces are treated as modules and mount names appear in signatures when `@mount` is used.
- Reduced Chromia CLI disk size by 50%.

## [0.17.2] - 2024-04-10
### Added
- **Experimental Windows support**
  - Added native support for Windows users via `scoop` ([scoop.sh](https://scoop.sh/)).
  - Command: `scoop bucket add chromia https://gitlab.com/chromaway/core-tools/scoop-chromia/`
  - Command: `scoop install chr`
- **Compatibility and updates**
  - `--ft-auth` updated for compatibility with FT4 version 0.4.0+.
  - Updated Rell code generation to 0.13.5, fixing NPE for partially named tuples in query return types.
  - Updated EIF to 0.4.1 to resolve EIF configuration validation issue.
  - New `strictGtvConversion` option in `chromia.yml` under `compile` property to configure strict GTV conversion.

### Fixed
- Fixed issue with `repl` on Mac.
- Fixed deployment bug where Rell versions higher than the target cluster supported were deployed.

## [0.17.1] - 2024-04-05
### Added
- Updated Rell version to [0.13.10](https://gitlab.com/chromaway/rell/-/blob/dev/doc/release-notes/0.14.0.txt?ref_type=heads).
- Added `--fail-on-error` flag for the `test` command, overriding local `test:failOnError` configuration.

### Fixed
- Updated Dokka plugin to 0.1.2 to fix broken links for anonymous functions.
- Updated code generation to 0.13.4 to remove shadowing warnings in Kotlin stubs.
- Set `compile:quiet` default to `false` for more verbose build messages in the terminal. Users can set it to `true` to suppress warnings.

## [0.17.0] - 2024-03-25
### Added
- **Split client stubs and graph generation**
  - `chr generate client-stubs` for client stubs and `chr generate graph` for mermaid graphs.
  - Deprecated `chr generate-client-stubs` in favor of separate commands.
- **Documentation generation**
  - New command: `chr generate docs-site`, enabling static API reference pages for dApps.
- **Aggregation and expression syntax**
  - Added annotations for `list`, `set`, and `map` aggregation on `at-expressions`.
  - New `at-expression` join syntax.
- **Dependency updates**
  - Updated to Postchain 3.15.5 and Rell 0.13.9.

## [0.16.3] - 2024-03-15
### Added
- Added support for big integer values in `chromia.yml` with a capital "L" suffix (e.g., `1234L`).
- Reintroduced schema validation for configuration files. See the full schema [here](https://gitlab.com/chromaway/core-tools/chromia-cli/-/blob/dev/chromia-build-tools/src/main/resources/chromia-model-schema.json?ref_type=heads).

### Fixed
- Verified that the Rell version in `chromia.yml` matches the highest supported version for the target cluster during deployment updates.
- Updated `jgit` dependency to address a security vulnerability.

## [0.16.2] - 2024-03-12
### Added
- Temporarily removed support for big integer values in `chromia.yml`.
- Rolled back the new parsing module to the previous version that works with anchors and references in YAML files.

## [0.16.1] - 2024-03-11
### Added
- Added support for big integer values in `chromia.yml` with a capital "L" suffix (e.g., `1234L`).
- Introduced economic chain support with the "Get Lease Information" command. This feature allows users to get lease information by container ID or public key (currently hidden but available under `chr deployment lease-info`).
- Updated the directory-chain version to 1.35.0.
- Updated Postchain to version 3.15.3.

### Fixed
- Standardized the message output for `chr start` when all blockchains have started, now showing "Node is initialized".
- Fixed stacktrace errors in YAML parsing and improved error messages when configuration files have issues.

## [0.16.0] - 2024-02-05
### Added
- Added `chr node start --directory-chain-mock`, which provides a directory chain mock for use in integration tests and manual testing of frontend clients.
- Enabled cross-chain transfers with FT4 and client-side usage of ICCF via node discovery features.

## [0.15.3] - 2024-01-29
### Added
- Added a prefilled `.gitignore` file to all templates created by `create-rell-dapp`.
- Introduced a directory to wrap the generated code. The directory is named after the project name, or `my-rell-dapp` by default if no project name is provided.

### Fixed
- Replaced all references to "hello" as the default blockchain name with `my_rell_dapp` if no project name is provided.

## [0.15.2] - 2024-01-25
### Fixed
- Added support for FT version 0.2.+ when using `--ft-auth` in `chr tx`.

## [0.15.1] - 2024-01-18
### Fixed
- Fixed bug where `chr test` failed for a blockchain test when ICMF was configured.

## [0.15.0] - 2024-01-17
### Added
- Added support for ICCF when running a test node with `chr node start`.
- Enabled unit tests with ICCF using `chr test`.
- Introduced support for sending ICCF proofs using `--iccf-tx` and `--iccf-source` in `chr tx`. (Note: ICCF proof operations are not verified in the test framework).
- Improved argument parsing for `chr tx/query`, now supporting nested structures (e.g., dicts are encoded as `[key: value]`).

### Fixed
- Disabled git progress monitor when running `chr install` non-interactively (e.g., in CI).
- Fixed `start` script to work for Alpine Linux and Busybox Docker images (version 0.14.3).
- Reverted to using BIP for key generation in `Keygen` (version 0.14.3).
- Fixed issue where global config overrides command-line input for `--cid` in `chr tx/query` (version 0.14.3).

## [0.14.2] - 2023-12-18
### Added
- Postchain 3.14.14, postchain-chromia 3.14.8, directory 1.30.0, postchain-client 3.12.1.
- Added Chromia.yml validation schema to the repo.

### Fixed
- Fixed issue with the `CHR_LOG_LEVEL` environment variable to properly set the log level for `chr node start`.
- Fixed concurrency issue where messages were lost due to a concurrency problem when using ICMF with a test node.

## [0.14.1] - 2023-12-06
### Fixed
- Reverted the explicit choice of test scope (`-bc` or no `-bc`), so now `chr test` runs all test modules and all blockchains by default.

## [0.14.0] - 2023-12-05
### Added
- Added ICMF support for `chr node start` (EXPERIMENTAL). (**Note**: Unprocessed messages will be lost during node restart, and the process may crash with an `OutOfMemoryException` if too many messages are sent. This is for testing, not production use.)
- Enabled compression of files for networks running the Management chain during `chr deployment create/update`.
- Added support for running tests on a selected module using `--module` for blockchain tests in `chr test`.
- Increased robustness of `chr tx` for the `--ft-auth` flag.

## [0.13.4] - 2023-11-28
### Fixed
- Fixed JavaScript typo in `generate-client-stubs`.

## [0.13.3] - 2023-11-28
### Added
- Updated `Keygen` to conform to BIP39 and BIP32 standards.

## [0.13.2] - 2023-11-15
### Added
- Added Rell 0.13.5 release notes.
- Shows unit test duration.
- `REPL` now uses the GTV output format.

### Fixed
- Exits with code 1 when a query/transaction fails in `chr repl`.
- Prints an info message when a blockchain is successfully removed from a container.

## [0.13.0 and 0.13.1] - 2023-11-08
### Added
- Reads API URL and BRID from `.chromia/config` file.
- Added the `-c` option to `chr repl` command to allow execution of a single command.
- New command: `chr deployment remove` to remove deployed blockchain from a container.
- Template flag for `chr create-rell-dapp` with options: `Minimal`, `Plain`, `Plain-Multi`.
- Codegen 0.12.0: Added option to generate mermaid entity relation diagrams.

### Fixed
- Fixed issue where `chr repl` did not print intro text when using the `-c` flag (bug from 0.13.0).
- Fixed `chr repl` to exit with status code 1 when it fails with the `-c` flag (bug from 0.13.0).
- Fixed default log level to be set as `info` instead of `debug` (macOS-specific bug).
