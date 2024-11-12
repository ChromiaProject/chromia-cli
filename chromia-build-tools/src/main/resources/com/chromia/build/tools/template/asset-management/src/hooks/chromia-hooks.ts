import { useCallback, useState } from "react";

import {
  AuthFlag,
  createSingleSigAuthDescriptorRegistration,
  registerAccount,
  registrationStrategy,
} from "@chromia/ft4";
import {
  createChromiaHooks,
  useEvmKeyStore,
  useFtAccounts,
  usePostchainClient,
} from "@chromia/react";
import { IClient } from "postchain-client";
import { useAccount } from "wagmi";

import { publicClientConfig as clientConfig } from "@/utils/generate-client-config";

const { useChromiaQuery, useChromiaImmutableQuery } = createChromiaHooks({
  defaultClientConfig: {
    blockchainRid: process.env.NEXT_PUBLIC_BRID,
    directoryNodeUrlPool: process.env.NEXT_PUBLIC_NODE_URL,
  },
});

export { useChromiaImmutableQuery, useChromiaQuery };

export const useChromiaAccount = ({
  onAccountCreated,
}: {
  onAccountCreated?: () => void;
} = {}) => {
  const [isLoading, setIsLoading] = useState(false);
  const [tried, setTried] = useState(false);
  const { address: ethAddress } = useAccount();
  const { data: client } = usePostchainClient({ config: clientConfig });
  const { data: keyStore } = useEvmKeyStore();
  const { mutate, data: ftAccounts } = useFtAccounts({ clientConfig });

  const createAccount = useCallback(async () => {
    try {
      setIsLoading(true);

      if (!ethAddress || !keyStore || !client) return;

      const ad = createSingleSigAuthDescriptorRegistration(
        [AuthFlag.Account, AuthFlag.Transfer],
        keyStore.id,
      );

      await registerAccount(
        client as IClient,
        keyStore,
        registrationStrategy.open(ad),
      );

      await mutate();

      onAccountCreated?.();
    } catch (e) {
      console.error(e);
    } finally {
      setIsLoading(false);
      setTried(true);
    }
  }, [client, ethAddress, keyStore, mutate, onAccountCreated]);

  return {
    createAccount,
    isLoading,
    tried,
    account: ftAccounts?.[0],
    hasAccount: !!ftAccounts?.length,
  };
};
