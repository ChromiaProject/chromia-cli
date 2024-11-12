# Asset management blockchain operations

This directory contains the Rell code for managing digital assets on the Chromia blockchain. It provides operations for
creating accounts, registering assets, minting tokens, and implementing a points-based system.

## Overview

The Rell code in this template allows you to:

- Create and manage user accounts.
- Register new assets and mint tokens.
- Add and manage points associated with user accounts.
- Query account balances and asset holdings.

## Getting started

To work with the Rell code in this template and test it locally, follow these steps:

1. **Start the local test node**:

Install library dependencies for your dapp.

   ```sh
   chr install
   ```

Ensure the local test node is running. If it's not already started, you can start it with:

   ```sh
   chr node start
   ```

#### Locally the dapp can be reached using these connection parameters:

```
url=http://localhost:7740
brid_id - will be generated after the node started
```

You can also specify additional options, such as `--wipe` to reset the database schema and start the blockchain from
scratch:

   ```sh
   chr node start --wipe
   ```

2. **Run tests**:

   Execute the predefined tests to validate the Rell code:

   ```sh
   chr test
   ```


3. **Update the local test node after changes**:

   If you modify the Rell code and need to update the running local test node with new configurations:

   ```sh
   chr node update
   ```

   Ensure you are using the same `chromia.yml` configuration and parameters as when the node was initially started.

## Customization

You can modify the provided Rell code to suit your specific requirements. This template is designed to be flexible and
extensible, allowing for easy customization to fit various use cases.

## Further information

For more detailed information on using `chr node` commands and managing the local test node, visit
the [Chromia CLI Node Documentation](https://docs.chromia.com/cli/node).

---

We hope this guide helps you effectively utilize the blockchain operations within the asset management template. Happy
coding!