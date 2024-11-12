"use client";

import React from "react";
import { useForm } from "react-hook-form";

import { Balance } from "@chromia/ft4";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";

import Button from "@/components/chromia-ui-kit/button";
import { Chr, LoaderCubes } from "@/components/chromia-ui-kit/icons";
import Input from "@/components/chromia-ui-kit/input";
import { useBurnTokens } from "@/hooks/token-hooks";
import { useBurnedSuccessModal } from "@/modals/burned-success-modal";
import { useTransactionFailedModal } from "@/modals/transaction-failed-modal";

const burnTokenSchema = z.object({
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

type BurnTokenValues = z.infer<typeof burnTokenSchema>;

export default function BurnTokenForm({
  onBurned,
  balance,
}: {
  onBurned?: () => void;
  balance: Balance;
}) {
  const {
    handleSubmit,
    register,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<BurnTokenValues>({
    resolver: zodResolver(burnTokenSchema),
    defaultValues: {
      amount: undefined,
    },
  });

  const { show: showBurnedSuccessModal } = useBurnedSuccessModal();
  const { show: showBurnedErrorModal } = useTransactionFailedModal();

  const burnToken = useBurnTokens({
    onSuccess: async (token) => {
      showBurnedSuccessModal(token);

      onBurned?.();
    },
    onError: showBurnedErrorModal,
  });

  return (
    <form
      onSubmit={handleSubmit((values) => burnToken(values.amount))}
      className="space-y-6"
    >
      <Input
        disabled
        label="Token"
        value={balance.asset.symbol.toUpperCase()}
        rightElement={<Chr className="h-6 w-6" />}
      />
      <Input
        {...register("amount", { valueAsNumber: true })}
        label="Amount"
        error={!!errors.amount}
        rightElement={
          <Button
            variant="secondary"
            size="s"
            onClick={() => setValue("amount", Number(balance.amount.value))}
          >
            Max
          </Button>
        }
        info={
          errors.amount?.message ??
          `Balance: ${Intl.NumberFormat().format(balance.amount.value)} ${balance.asset.symbol.toUpperCase()}`
        }
      />

      <Button type="submit" className="w-full" disabled={isSubmitting}>
        {isSubmitting ? <LoaderCubes /> : "Burn"}
      </Button>
    </form>
  );
}
