# Chromia CLI Library Commands Documentation

This documentation describes the `library` commands in the Chromia CLI, which provide a wrapper around the library-chain blockchain functionality.
These commands allow developers to manage organizations, libraries, developers, and collaborate in the Chromia library ecosystem.

## Overview

The library commands are organized into several categories:
- **Developer Management**: Create, update, and query developer accounts
- **Organization Management**: Create and manage organizations that own libraries
- **Library Management**: Create, deploy, list, and manage libraries and their versions
- **Collaboration**: Invite users, manage permissions, and handle invitations
- **Installation**: Install libraries from the registry to local projects

## Common Options

All library commands inherit common options for connecting to the library-chain:

### Authentication Options
- `--secret <path>`: Path to the secret key file for signing transactions
- `--url <url>`: URL to the target node where library-chain is deployed (e.g., `https://custom-network`)
- `--brid <hex>`: Optional - Blockchain RID of the library-chain, not required if `--url` in predefined networks : [`testnet`, `mainnet`, `devnet`, `local`]

### Examples of Common Usage Patterns
```bash
# Using with testnet
library <command> --url testnet

# Using with local development
library <command> --url localhost 
```

## Developer Commands

### `library developer create`
Create a new library developer account.

**Usage:**
```bash
library developer create --name <name> [options]
```

**Options:**
- `--name <name>` (required): Name of the library developer
- `--strategy <strategy>`: Account creation strategy (default: `transfer_open`)
  - `transfer_open`: Transfer with open strategy
  - `open`: Open strategy

**Example:**
```bash
library developer create --name "Misha" --secret keys/dev1 --url mainnet
```

### `library developer get`
Retrieve information about a developer by account ID.

**Usage:**
```bash
library developer get <account_id> [options]
```

**Arguments:**
- `account_id`: Developer account ID (hex string)

**Example:**
```bash
library developer get 10d69812799efac34e327de10ff1d35d0e0515cae9b320d9865076d9d7e3843a --url testnet
```

### `library developer update`
Update a developer's name.

**Usage:**
```bash
library developer update <new_name> [options]
```

**Arguments:**
- `new_name`: New name for the developer

**Options:**
- `--secret` : Path to a secret key

**Example:**
```bash
library developer update "Tim" --secret keys/dev1
```

## Organization Commands

### `library organization create`
Create a new organization that can own libraries.

**Usage:**
```bash
library organization create --org-id <id> --name <name> --description <desc> [options]
```

**Options:**
- `--org-id <id>` (required): Unique identifier for the organization (e.g., "net.my_org")
- `--name <name>`, `-n <name>` (required): Name of the organization
- `--description <desc>`, `-d <desc>` (required): Description of the organization
- `--is-official`: Mark as official Chromia library (flag, requires admin privileges)

**Example:**
```bash
library organization create --org-id "org.example" --name "test organization" --description "Test organization" --secret keys/dev1 --url devnet
```

### `library organization invite-user`
Invite a user to join an organization with organization-wide access.

**Usage:**
```bash
library organization invite-user <org_id> <developer_id> [options]
```

**Arguments:**
- `org_id`: Organization ID to invite user to
- `developer_id`: Account ID of the developer to invite (hex string)

**Options:**
- `--access-level <level>`, `-a <level>`: Access level (default: `publisher`)
  - `admin`: Full administrative access
  - `publisher`: Can create versions and manage metadata
  - `reviewer`: Read-only access
- `--expiry-ms <milliseconds>`, `-e <milliseconds>`: Invitation expiry time in milliseconds (default: 10 minutes)

## Library Management Commands

### `library create`
Create a new library in an organization.

**Usage:**
```bash
library create --organization <org_id> --name <display_name> --library <lib_name> --description <desc>  [options]
```

**Options:**
- `--library <lib_name>` (required): Library name to include from chromia.yml configuration
- `--name <display_name>`, `-n <display_name>` (required): Display name for the library
- `--description <desc>`, `-d <desc>` (required): Description of the library
- `--organization <org_id>`, `-o <org_id>` (required): Organization ID that will own this library
- `--version <version>`, `-v <version>`: Initial version (default: "0.0.1")
> library id will be <org_id> + '.' + <name>
>   e.g: <org_id> = 'com.chromia' and <display_name> = 'ft4', the id will be 'com.chromia.ft4'

**Example:**
- The --library flag references a library anchor defined in your `chromia.yml`, which specifies the exact module directory to upload.
- `chr` will upload only the files from the specified module directory that will define the library

```yaml
blockchains:
  my_lib:     # <------ the library to include
    module: lib.my_rell_dapp
    type: library # <----- the type must be library
    test:
      modules:
        - tests
```

```bash
library create --id "my_lib_1" --library "my_lib" --name "My library" --description "My test custom library" --version "0.0.1" --organization "com.example" --secret keys/dev2 --url testnet
```

### `library deploy`
Deploy a new version of an existing library.

**Usage:**
```bash
library deploy --id <lib_id> --library <lib_name> --version <version> --description <desc> [options]
```

**Options:**
- `--id <lib_id>` (required): ID of the library to deploy a new version for
- `--library <lib_name>` (required): Library name from chromia.yml configuration
- `--version <version>`, `-v <version>` (required): Version number for this deployment
- `--description <desc>`, `-d <desc>` (required): Description for this version

**Example:**
```bash
library deploy --id "my_lib" --version "0.0.4" --description "my custom library" --library "my_lib" --secret keys/dev2 --url testnet
```

### `library list`
List all available libraries in the registry.

**Usage:**
```bash
library list [options]
```

**Options:**
- `--limit <number>`, `-l <number>`: Maximum number of libraries to display (default: 10)
- `--offset <number>`, `-o <number>`: Number of libraries to skip (default: 0)
- `--sort-by <order>`: Sort order (`asc` or `desc`, default: `desc`)

**Example:**
```bash
library list --url testnet
```

### `library view`
View detailed information about a specific library.

**Usage:**
```bash
library view <library_id> [options]
```

**Arguments:**
- `library_id`: ID of the library to view

### `library versions`
List all versions of a specific library.

**Usage:**
```bash
library versions <library_id> [options]
```

**Arguments:**
- `library_id`: ID of the library to list versions for

**Options:**
- `--limit <number>`, `-l <number>`: Maximum number of versions to display (default: 10)
- `--offset <number>`, `-o <number>`: Number of versions to skip (default: 0)

### `library install`
Install libraries from the registry to your local project.

The `library install` command reads your `chromia.yml` file and installs all libraries defined in the `libs` section. It supports both library-chain libraries (published to Chromia's library registry) and external Git libraries.

**Usage:**
```bash
library install [<library_id>] [options]
```

**Arguments:**
- `library_id` (optional): Specific library ID to install. If not provided, all configured libraries will be installed.

**Options:**
- `--force`, `-f`: Force installation even if RID verification fails (use with caution)

**Configuration:**

Given that `some_lib` library exists in the library-chain on `testnet`, you can configure it in your `chromia.yml` file using the `libs` section:

```yaml
libs:
  ft4:                    # <--- External Git library
    registry: https://bitbucket.org/chromawallet/ft3-lib
    path: rell/src/lib/ft4
    tagOrBranch: v1.0.0r
    rid: x"FA487D75E63B6B58381F8D71E0700E69BEDEAD3A57D1E6C1A9ABB149FAC9E65F"
    insecure: false
  some_lib:               # <--- Library-chain library using predefined network
    version: 0.0.1
    registry: testnet     # Uses predefined Chromia network
  another_lib:            # <--- Library-chain library on custom network
    version: 1.0.0
    registry: https://custom-network.com:7740
    brid: x"6933A4AB594C85FCAF8D3B7EA14F11CA4B06826EE1A3A823055D1CC923E71FF9"
```

**Library Types:**

1. **Library-chain libraries**: Published to Chromia's library registry
   - Specify `version` and `registry`
   - `registry` can be a predefined network name (`testnet`, `devnet1`, `localhost`) or custom URL
   - For custom URLs, include the `brid` of the library-chain

2. **External Git libraries**: Hosted in Git repositories
   - Specify `registry` (Git URL), `path`, `tagOrBranch`, `rid`, and `insecure` flag

**Examples:**

Install all configured libraries:
```bash
library install
```

Install a specific library using its configured registry:
```bash
library install my_lib
```

Install a specific version library using its configured registry:
```bash
library install my_lib@1.0.0
```

Install a specific library from a custom registry:
```bash
library install my_lib --url https://custom-registry.com:7740
```

Install with force flag to bypass RID verification:
```bash
library install my_lib --force
```

## Collaboration Commands

### `library invite-user`
Invite a user to collaborate on a specific library.

**Usage:**
```bash
library invite-user --library-id <lib_id> --developer-id <dev_id> [options]
```

**Options:**
- `--library-id <lib_id>` (required): ID of the library to invite user to
- `--developer-id <dev_id>` (required): Account ID of the developer to invite (hex string)
- `--access-level <level>`, `-a <level>`: Access level (default: `publisher`)
  - `admin`: Full administrative access to the library
  - `publisher`: Can create versions and manage metadata
  - `reviewer`: Read-only access
- `--expiry-ms <milliseconds>`, `-e <milliseconds>`: Invitation expiry time in milliseconds (default: 10 minutes)

**Example:**
```bash
library invite-user --library-id "lib1.test" --developer-id "10d69812799efac34e327de10ff1d35d0e0515cae9b320d9865076d9d7e3843a" --secret keys/dev1 --url testnet
```

### `library accept-invitation`
Accept a library or organization invitation.

**Usage:**
```bash
library accept-invitation <invitation_code> [options]
```

**Arguments:**
- `invitation_code`: The invitation code to accept

**Example:**
```bash
library accept-invitation "zABCDEFGHIJK" --secret keys/dev2 --url testnet
```

### `library list-invitations`
List pending invitations for the current developer.

**Usage:**
```bash
library list-invitations [options]
```

**Example:**
```bash
library list-invitations --secret keys/dev2 --url testnet
```

### `library delete-dev`
Remove a developer's access from a library.

**Usage:**
```bash
library delete-dev --library-id <lib_id> --dev-id <dev_id> [options]
```

**Options:**
- `--library-id <lib_id>` (required): ID of the library to remove user from
- `--dev-id <dev_id>` (required): Account ID of the developer to remove

**Example:**
```bash
library delete-dev --library-id "lib1.test" --dev-id "10d69812799efac34e327de10ff1d35d0e0515cae9b320d9865076d9d7e3843a" --secret keys/dev1 --url testnet
```

### `library update-user-permission`
Update a user's permission level for a library.

**Usage:**
```bash
library update-user-permission --library-id <lib_id> --dev-id <dev_id> --access-level <level> [options]
```

**Options:**
- `--library-id <lib_id>` (required): ID of the library
- `--dev-id <dev_id>` (required): Account ID of the developer
- `--access-level <level>` (required): New access level (`admin`, `publisher`, or `reviewer`)

## Access Levels

The library system supports three access levels:

### Admin
- Full administrative access
- Can invite/remove users
- Can update user permissions
- Can create new library versions
- Can manage library metadata
- Can delete invitations

### Publisher
- Can create new library versions
- Can manage library metadata
- Cannot manage user access

### Reviewer
- Read-only access
- Can view library details and versions
- Cannot modify anything

## Organization vs Library Access

The system supports hierarchical access control:

1. **Organization-level access**: Grants permissions to all libraries within an organization
2. **Library-level access**: Grants permissions to a specific library only

When a developer has both types of access, the higher permission level applies. Organization administrators can manage all libraries within their organization.

## Common Workflows

### Setting up a new organization and library
```bash
# 1. Create developer account
library developer create --name "John Doe" --secret keys/dev1 --url <url> --brid <brid>

# 2. Create organization
library organization create --org-id "com.mycompany" --name "My Company" --description "Company libraries" --secret keys/dev1 --url <url> 

# 3. Create library
library create --id "my-utils" --library "utils" --name "Utility Library" --description "Common utilities" --organization "com.mycompany" --secret keys/dev1 --url testnet
```

### Collaborating on a library
```bash
# 1. Invite a developer to a library
library invite-user --library-id "my-utils" --developer-id "<dev_account_id>" --access-level "publisher" --secret keys/admin --url <url>

# 2. Developer accepts invitation
library accept-invitation "<invitation_code>" --secret keys/dev2 --url <url> --brid <brid>

# 3. Deploy new version
library deploy --id "my-utils" --version "1.1.0" --description "Bug fixes" --rid "<version_rid>" --library "utils" --secret keys/dev2 --url <url> --brid <brid>
```

## Notes
- Private keys (`--secret`) are required for all operations that modify blockchain state
- Invitation codes are single-use and expire after the specified duration
  - Library files are automatically collected from the configured source directory (only `.rell` files are included). Before uploading, `chr` validates the code for compilation errors to ensure library quality
  