import { getDefaultConfig } from "connectkit";
import { createConfig,http } from "wagmi";
import { mainnet, sepolia } from "wagmi/chains";

export const config = createConfig(
  getDefaultConfig({
    chains: [mainnet, sepolia],
    transports: {
      [mainnet.id]: http(),
      [sepolia.id]: http(),
    },
    // Required API Keys
    walletConnectProjectId: process.env.NEXT_PUBLIC_WALLETCONNECT_PROJECT_ID!,
    appName: "easy-start",
  })
);
