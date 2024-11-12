"use client";

import React, { useCallback } from "react";

import Link from "next/link";
import { useRouter } from "next/navigation";

import { useFtAccounts, useGetBalances } from "@chromia/react";
import { ArrowLeft } from "lucide-react";

import LinkIconButton from "@/components/chromia-ui-kit/link-icon-button";
import { CardLoading } from "@/components/layout/card-loading";
import { Card, CardTitle } from "@/components/ui/card";
import TransferTokenForm from "@/forms/transfer";
import { publicClientConfig as clientConfig } from "@/utils/generate-client-config";

export default function BurnToken() {
  const { data: ftAccounts } = useFtAccounts({ clientConfig });

  const {
    flatData: balances,
    isLoading,
    mutate,
  } = useGetBalances(
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

  const router = useRouter();

  const handleSuccess = useCallback(async () => {
    await mutate();
    router.push("/token");
  }, [mutate, router]);

  if (isLoading || !balances?.length) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <CardLoading />
      </div>
    );
  }

  return (
    <div className="flex flex-1 items-center justify-center">
      <Card className="w-full max-w-lg p-6">
        <div className="mb-8 flex items-center">
          <Link href="/token" passHref legacyBehavior>
            <LinkIconButton Icon={<ArrowLeft />} variant="ghost" />
          </Link>
          <CardTitle className="mx-auto text-center text-xl">
            Transfer
          </CardTitle>
        </div>
        <TransferTokenForm balance={balances[0]} onTransfered={handleSuccess} />
      </Card>
    </div>
  );
}
