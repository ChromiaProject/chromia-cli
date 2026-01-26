# Key Pair Management in Chromia CLI

This document describes how key pairs are read and managed within the Chromia CLI for various commands.

## Key Pair Reading Flow
The Chromia CLI follows a specific precedence order when determining which key pair to use for operations:

1. **Global Configuration**: 
   - The CLI first reads the keyId from the global configuration file located at `~/.chromia/config`
   - This global configuration applies to all commands unless overridden

2. **Project-specific Configuration**:
   - If a project has a local configuration file at the default location `<project-path>/.chromia/config`, the keyId specified there takes precedence over the global configuration
   - Keys are still located and read from the `~/.chromia` directory

3. **Explicit Configuration Path**:
   - If a user specifies a configuration path using the `--config` option, the keyId from that configuration takes precedence
   - Keys are still located and read from the `~/.chromia` directory

4. **Key ID Option**:
   - If the user specifies a key ID directly using the `--key-id` option for a command, this takes precedence over all configuration files
   - Keys are still located and read from the `~/.chromia` directory

5. **Secret File Option**:
   - The `--secret` option pointing to a file containing key pair information takes the highest precedence
   - This overrides all other key pair sources

## Key Storage

### Key Id
Keys are stored in the `~/.chromia` directory by default. When using the `keygen` command with the `--key-id` option, the following files are created:

- `~/.chromia/{key-id}` - Contains the private key
- `~/.chromia/{key-id}.pubkey` - Contains the public key
- `~/.chromia/{key-id}_mnemonic` - Contains the mnemonic phrase for recovery

### Secret File
When using the `keygen` command with the `--file` option the following files are created: 
- `<file_name>` - Contains the keypair
- `<file_name>_mnemonic` - Contains the mnemonic phrase for recovery
