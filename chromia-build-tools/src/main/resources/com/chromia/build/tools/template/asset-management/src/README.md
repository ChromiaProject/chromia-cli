# Asset management dApp with Next.js and Chromia

## Project Description

This is a sample application demonstrating how to build a frontend for a blockchain using **Chromia** technology.
The app allows users to create their own cryptocurrency and manage it, including transferring tokens between wallets and
burning tokens.
The project is based on Next.js and integrates with Metamask to allow users to log in and manage their tokens.

The main goal is to showcase how to build frontend functionalities that interact with a backend written in **Rell**.

## Features

1. Create your own tokens/cryptocurrency – Users can create their own tokens after logging in via Metamask.
2. Transfer tokens – The app allows users to transfer created tokens between Metamask wallets.
3. Burn tokens – Users can burn a specified amount of tokens.

## Requirements

- Node.js: version 23 or higher
- pnpm: version 9 or higher
- Metamask wallet

## Dependencies

The application uses the following technologies and libraries:

- **Next.js** – React framework for building web applications
- **Tailwind CSS** – Utility-first CSS framework for styling
- **@tanstack/react-query** – Data fetching and state management library
- **shadcn and radix** – Set of UI components
- **react-hook-form and zod** – Form handling and validation

## Installation

To install the project locally, follow these steps:

1. Install dependencies:
   Use pnpm to install all required packages:

```bash
pnpm install
```

2. Configure the environment:
   Create an .env file based on the .env.example file and fill it with the appropriate values:

```bash
NEXT_PUBLIC_NODE_URL=http://localhost:7740
NEXT_PUBLIC_BRID=<Your_BRID_for_the_Dapp>
```

- NEXT_PUBLIC_NODE_URL – The URL of the Chromia blockchain node.
- NEXT_PUBLIC_BRID – The BRID of your locally or remotely running Dapp.

## Running the Application

Once the dependencies are installed and the .env file is configured, you can run the application locally:

```bash
pnpm dev
```

The app will be available at http://localhost:3000.

## Project Structure

- **app** – Contains the main application logic and routing. This directory follows the new Next.js 14 app directory
  structure.
- **components** – Contains all React visual components used in the application.
- **forms** – Contains form components used in the application.
- **hooks** – Custom hooks used in the application.
    - **chromia-hooks** – Hook for interacting with the Chromia blockchain.
    - **token-hooks** – Hook for managing token-related functionalities.
- **modals** – Contains modal components used in the application.
- **providers** – Contains context providers used in the application.

## No Backend

This application does not include its own backend. All backend functionalities are based on the Chromia blockchain and a
backend built using Rell.

## Hosting the webpage on the blockchain

If you want to host the website on your dapp as a static page, this is done by the following steps.

1. Run `chr node start --wipe` to get the blockchainRid

2. replacing the empty Nextjs config with the following. And set the .env variable NEXT_PUBLIC_BRID to the new brid.

``` 
const nextConfig = {
    output: 'export',
    images: {
        unoptimized: true,
    },
    basePath: `/web_query/${process.env.NEXT_PUBLIC_BRID}/web_static`,
  };
```

3. Build the frontend and package it using

```bash
pnpm build
```

4. Add webStatic to your `rell/chromia.yml`

```yaml
blockchains:
  asset_management:
    webStatic: out #this attribute
```

5. Update the blockchain `chr node update` to get the new queries enabled
6. Navigate to `http://localhost:7740/web_query/<blockchainRid>/web_static` to interact with you webpage