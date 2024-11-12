"use client";

import React from "react";
import { useForm } from "react-hook-form";

import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";

import Button from "@/components/chromia-ui-kit/button";
import { Chr, LoaderCubes, Plus } from "@/components/chromia-ui-kit/icons";
import Input from "@/components/chromia-ui-kit/input";
import { useMintToken } from "@/hooks/token-hooks";
import { useMintedSuccessModal } from "@/modals/minted-success-modal";
import { useTransactionFailedModal } from "@/modals/transaction-failed-modal";

const mintTokenSchema = z.object({
  ticker: z.string().min(1, { message: "Ticker is required" }),
  name: z.string().min(1, { message: "Name is required" }),
  amount: z
    .number({
      message: "Amount is required",
    })
    .min(1, {
      message: "Amount should be greater than 0",
    })
    .max(100_000, {
      message: "Max possible to mint 100 000 tokens",
    }),
});

type MintTokenForm = z.infer<typeof mintTokenSchema>;

export default function MintToken({ onMinted }: { onMinted: () => void }) {
  const {
    handleSubmit,
    register,
    formState: { errors, isSubmitting },
  } = useForm<MintTokenForm>({
    resolver: zodResolver(mintTokenSchema),
    defaultValues: {
      ticker: "",
      name: "",
      amount: undefined,
    },
  });

  const { show: showMintedSuccessModal } = useMintedSuccessModal();
  const { show: showMintedErrorModal } = useTransactionFailedModal();

  const registerAsset = useMintToken({
    onSuccess: (token) => {
      showMintedSuccessModal(token);
      onMinted();
    },
    onError: showMintedErrorModal,
  });

  return (
    <form
      onSubmit={handleSubmit((values) => registerAsset(values))}
      className="space-y-6"
    >
      <Input
        {...register("ticker")}
        label="Enter token tiker"
        error={!!errors.ticker}
        info={errors.ticker?.message ?? ""}
        rightElement={<Chr className="h-6 w-6" />}
      />
      <Input
        {...register("name")}
        label="Enter token full name"
        error={!!errors.name}
        info={errors.name?.message ?? ""}
      />
      <Input
        {...register("amount", { valueAsNumber: true })}
        label="Enter amount"
        error={!!errors.amount}
        info={errors.amount?.message ?? "Max possible to mint 100 000 tokens"}
      />

      <Button type="submit" className="w-full" disabled={isSubmitting}>
        {isSubmitting ? (
          <LoaderCubes />
        ) : (
          <>
            Mint
            <Plus className="h-5 w-5" />
          </>
        )}
      </Button>
    </form>
  );
}
