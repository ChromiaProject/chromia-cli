import { useCallback } from "react";

import { createAmount, op } from "@chromia/ft4";
import {
  useFtAccounts,
  useFtSession,
  useGetAllAssets,
  useGetBalances,
  usePostchainClient,
} from "@chromia/react";
import { useAccount } from "wagmi";

import { ensureBuffer } from "@/utils/ensure-buffer";
import { publicClientConfig as clientConfig } from "@/utils/generate-client-config";

interface MintTokenParams {
  ticker: string;
  name: string;
  amount: number;
}

export function useMintToken({
  onSuccess,
  onError,
}: {
  onSuccess?: (token: MintTokenParams) => void;
  onError?: (token: MintTokenParams) => void;
} = {}) {
  const { address: ethAddress } = useAccount();
  const { data: client } = usePostchainClient({ config: clientConfig });
  const { mutate: mutateAllAssets } = useGetAllAssets({
    clientConfig,
    params: [],
  });

  const { data: ftAccounts } = useFtAccounts({ clientConfig });

  const { data: session } = useFtSession(
    ftAccounts?.length ? { clientConfig, account: ftAccounts[0] } : null,
  );

  const registerAsset = useCallback(
    async (token: MintTokenParams) => {
      if (!client || !ethAddress || !session) return;

      try {
        await session
          .transactionBuilder()
          .add(
            op(
              "register_and_mint_asset",
              token.name,
              token.ticker,
              8,
              BigInt(token.amount),
              "https://cdn-icons-png.flaticon.com/512/4863/4863873.png",
            ),
          )
          .buildAndSend();

        await mutateAllAssets();

        onSuccess?.(token);
      } catch (error) {
        console.error(error);

        onError?.(token);
      }
    },
    [client, ethAddress, mutateAllAssets, onError, onSuccess, session],
  );

  return registerAsset;
}

export function useTransferTokens({
  onSuccess,
  onError,
}: {
  onSuccess?: (token: MintTokenParams) => void;
  onError?: (token: MintTokenParams) => void;
}) {
  const { data: ftAccounts } = useFtAccounts({ clientConfig });

  const { data: session } = useFtSession(
    ftAccounts?.length ? { clientConfig, account: ftAccounts[0] } : null,
  );

  const { flatData: balances } = useGetBalances(
    ftAccounts?.length
      ? {
          clientConfig,
          account: ftAccounts[0],
          params: [10],
          swrInfiniteConfiguration: {
            refreshInterval: 20_000,
          },
        }
      : null,
  );

  const transferTokens = useCallback(
    async (recipient: string, amount: number) => {
      if (!balances?.length) {
        return;
      }
      const asset = balances[0].asset;

      try {
        await session?.account.transfer(
          ensureBuffer(recipient),
          ensureBuffer(asset.id),
          createAmount(amount),
        );

        onSuccess?.({ ticker: asset.symbol, name: asset.name, amount });
      } catch (error) {
        console.error(error);

        onError?.({ ticker: asset.symbol, name: asset.name, amount });
      }
    },
    [balances, onError, onSuccess, session?.account],
  );

  return transferTokens;
}

export function useBurnTokens({
  onSuccess,
  onError,
}: {
  onSuccess?: (token: MintTokenParams) => void;
  onError?: (token: MintTokenParams) => void;
}) {
  const { data: ftAccounts } = useFtAccounts({ clientConfig });

  const { data: session } = useFtSession(
    ftAccounts?.length ? { clientConfig, account: ftAccounts[0] } : null,
  );

  const { flatData: balances } = useGetBalances(
    ftAccounts?.length
      ? {
          clientConfig,
          account: ftAccounts[0],
          params: [10],
          swrInfiniteConfiguration: {
            refreshInterval: 20_000,
          },
        }
      : null,
  );

  const burnTokens = useCallback(
    async (amount: number) => {
      if (!balances?.length) {
        return;
      }
      const asset = balances[0].asset;

      try {
        await session?.account.burn(asset.id, createAmount(amount));

        onSuccess?.({ ticker: asset.symbol, name: asset.name, amount });
      } catch (error) {
        console.error(error);

        onError?.({ ticker: asset.symbol, name: asset.name, amount });
      }
    },
    [balances, onError, onSuccess, session?.account],
  );

  return burnTokens;
}
