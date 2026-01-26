# Introduction & Project Context

## Project Overview

**Project Name:** Chromia CLI (chromia-cli)  
**Repository:** https://gitlab.com/chromaway/core-tools/chromia-cli

Chromia CLI is a command-line tool designed to simplify and streamline the complete development lifecycle of Rell decentralized applications (dapps) on the Chromia blockchain. The CLI provides a unified interface that consolidates all essential functionality needed for building, testing, deploying, and managing Rell dapps into a single, easy-to-use tool.

## Project Purpose

Chromia CLI exists to eliminate the complexity and fragmentation that developers face when working with blockchain development. Instead of juggling multiple tools, scripts, and manual processes, developers can use a single CLI (`chr`) to handle everything from project scaffolding to production deployment.

The tool addresses the entire development workflow:
- **Project Creation**: Generate template projects with different structures (minimal, plain, multi-module, library-focused, or asset management templates)
- **Development**: Build, test, and interact with Rell code locally
- **Library Management**: Install and manage reusable Rell libraries from the Chromia library registry
- **Deployment**: Deploy dapps to various Chromia networks (mainnet, testnet, devnet, or custom networks)
- **Interaction**: Query blockchain state, submit transactions, and interact with deployed dapps
- **Testing**: Run tests, generate test data, and validate code quality

## Value Created

Chromia CLI creates value by:

1. **Reducing Development Friction**: Developers can start building immediately without configuring multiple tools or understanding complex deployment processes
2. **Standardizing Workflows**: Provides consistent, repeatable processes for common development tasks across all Chromia dapp projects
3. **Enabling Library Ecosystem**: Facilitates discovery, installation, and management of reusable Rell libraries, promoting code reuse and collaboration
4. **Simplifying Deployment**: Abstracts away the complexity of blockchain deployment, allowing developers to focus on application logic
5. **Improving Developer Experience**: Offers features like auto-completion, REPL for interactive development, and comprehensive error messages

## Users

Chromia CLI serves two primary user groups:

1. **External Developers**: Developers building dapps on the Chromia blockchain who need a streamlined development experience
2. **Internal Chromia Developers**: Chromia team members developing platform features, libraries, or example applications

Both groups benefit from the unified interface and standardized workflows that the CLI provides.

## Ecosystem Context

### Upstream Projects

Chromia CLI integrates with and depends on several upstream projects in the Chromia ecosystem:

- **[Postchain](https://gitlab.com/chromaway/core/postchain)**: The underlying blockchain infrastructure that powers Chromia networks. The CLI uses Postchain client libraries to interact with blockchain nodes.
- **[Postchain Client](https://gitlab.com/chromaway/core/postchain-client)**: Client SDK that provide communication with Chromia networks.
- **[Rell](https://gitlab.com/chromaway/rell)**: The programming language used to write Chromia dapps. The CLI includes the Rell compiler and runtime, enabling local development and testing.
- **[Directory Chain](https://gitlab.com/chromaway/core/directory-chain)**: Chromia's directory service that maintains metadata about deployed blockchains and containers. The CLI queries Directory Chain to discover and interact with deployed dapps.
- **[Library Chain](https://bitbucket.org/chromawallet/library-chain/)**: A specialized blockchain that serves as a registry for reusable Rell libraries. The CLI interacts with Library Chain to discover, publish, and install libraries.
- **[Chromia CLI Tools](https://gitlab.com/chromaway/core-tools/chromia-cli-tools)**: Shared build and configuration utilities used across Chromia tooling.
- **[Rell Toolbox](https://gitlab.com/chromaway/core-tools/rell-toolbox)**: Rell language tooling project, used in chromia-cli for code formatter and linter.

### Data and Control Flow

The CLI acts as a bridge between developers and the Chromia blockchain ecosystem:

1. **Development Flow**: Developers use CLI commands to create projects, test Rell code, and build blockchain configurations locally. The CLI compiles Rell code and generates deployment artifacts.

2. **Library Flow**: When installing libraries, the CLI queries Library Chain to discover available libraries, or clones library repositories from Git, and integrates them into the local project structure.

3. **Deployment Flow**: During deployment, the CLI:
   - Reads project configuration from `chromia.yml`
   - Compiles Rell code and generates blockchain configurations
   - Submits deployment proposals to Directory Chain
   - Manages blockchain lifecycle (create, update, pause, resume, remove)

4. **Interaction Flow**: When querying or submitting transactions, the CLI:
   - Connects to Chromia network nodes (mainnet, testnet, or custom)
   - Uses Postchain client libraries to communicate with blockchain nodes
   - Handles authentication (FT4, blockchain auth, or EVM-compatible)
   - Formats and displays results to developers

5. **Network Discovery**: The CLI queries Directory Chain to discover available blockchains, containers, and network endpoints, enabling developers to interact with the broader Chromia ecosystem.

## External References

- **Official Documentation**: https://docs.chromia.com/build/cli/introduction - Comprehensive documentation covering Chromia platform, Rell language, and CLI usage
- **CLIKT Documentation**: https://ajalt.github.io/clikt/ - Library for creating Command Line Interfaces
