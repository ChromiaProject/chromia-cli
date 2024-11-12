"use client";

import type React from "react";

import { ConnectKitButton } from "connectkit";
import { useAccount } from "wagmi";

import Button from "@/components/chromia-ui-kit/button";
import { CardLoading } from "@/components/layout/card-loading";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { useChromiaAccount } from "@/hooks/chromia-hooks";
import { useAccountCreatedModal } from "@/modals/account-created-modal";

const Layout = ({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) => {
  const { isConnected } = useAccount();
  const { show } = useAccountCreatedModal();
  const { hasAccount, createAccount, isLoading, tried } = useChromiaAccount({
    onAccountCreated: show,
  });

  if (isConnected) {
    if (hasAccount) {
      return children;
    }

    return (
      <div className="flex flex-1 items-center justify-center">
        {isLoading ? (
          <CardLoading />
        ) : (
          <Card className="min-w-52 p-6 text-center">
            <CardTitle>Account not found</CardTitle>
            <CardDescription className="mt-2">
              You need to create an chromia account first to mint own token.
            </CardDescription>
            <Button onClick={createAccount} className="mx-auto mt-4 min-w-44">
              {tried ? "Retry creating account" : "Create account"}
            </Button>
          </Card>
        )}
      </div>
    );
  }

  return (
    <>
      <div className="flex flex-1 items-center justify-center">
        <ConnectKitButton.Custom>
          {({ show, isConnecting }) => {
            if (isConnecting) {
              return <CardLoading />;
            }

            return (
              <Card className="min-w-52 p-6 text-center">
                <CardTitle>Connect to your wallet</CardTitle>
                <CardDescription className="mt-2">
                  You need to connect your wallet to mint own token.
                </CardDescription>
                <Button onClick={show} className="mx-auto mt-4 w-44">
                  Connect wallet
                </Button>
              </Card>
            );
          }}
        </ConnectKitButton.Custom>
      </div>
    </>
  );
};

export default Layout;
