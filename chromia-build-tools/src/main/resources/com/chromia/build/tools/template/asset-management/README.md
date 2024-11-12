# Asset management template

This template provides a foundation for building dApps focused on asset management on the Chromia blockchain. It
includes components for blockchain operations and a frontend for user interaction.

## Overview

The asset management template is designed to help developers quickly implement and deploy common blockchain
functionalities, such as creating accounts, registering assets, minting tokens, and querying balances.

### Features

- **Account management**: Create and authenticate user accounts on the Chromia blockchain.
- **Asset management**: Register new assets, mint tokens, and manage asset balances.
- **Points system**: Add and manage a points-based system tied to user accounts.
- **Frontend integration**: Provides a web interface to interact with the blockchain backend.

## Directory structure

- **`rell/`**: Contains the code for blockchain operations. Refer to `rell/README.md` for detailed setup and deployment
  instructions.
- **`src/`**: Contains the frontend code for interacting with the blockchain. Refer to `src/README.md` for
  setup and usage instructions.
- Config files for the frontend and the rell backend exist in the root directory

## Getting started

To start using the asset management template:

1. **Follow the instructions for each component**:

    - **Blockchain setup**: Navigate to the `rell` directory and follow the instructions in `rell/README.md` to set up,
      compile, and deploy the blockchain code.

   ```sh
   cd rell
   ```

    - **Frontend setup**: see `src/README.md` for instructions on
      setting up the frontend application. All config files exist in the root of the project.

## Customization

You can modify the provided code to suit your specific requirements. The template is designed to be flexible and
extendable, allowing for easy customization to fit various use cases.

## Further information

For more information on developing with Chromia, see the main project [README.md](../README.md) or visit
the [Chromia Developer Documentation](https://docs.chromia.com/).

---

We hope this template provides a solid foundation for your asset management dApp on Chromia. Happy coding!