"use client";

import React from "react";
import { useForm } from "react-hook-form";

import { Balance } from "@chromia/ft4";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";

import Button from "@/components/chromia-ui-kit/button";
import { Chr, LoaderCubes } from "@/components/chromia-ui-kit/icons";
import Input from "@/components/chromia-ui-kit/input";
import { useTransferTokens } from "@/hooks/token-hooks";
import { useTransactionFailedModal } from "@/modals/transaction-failed-modal";
import { useTransferedSuccessModal } from "@/modals/transfered-success-modal";

const transferTokenSchema = z.object({
  recepient: z.string().min(1, {
    message: "Recepient is required",
  }),
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

type TransferTokenValues = z.infer<typeof transferTokenSchema>;

export default function TransferTokenForm({
  onTransfered,
  balance,
}: {
  onTransfered?: () => void;
  balance: Balance;
}) {
  const {
    handleSubmit,
    register,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<TransferTokenValues>({
    resolver: zodResolver(transferTokenSchema),
    defaultValues: {
      amount: undefined,
    },
  });

  const { show: showTransferedSuccessModal } = useTransferedSuccessModal();
  const { show: showTransferedErrorModal } = useTransactionFailedModal();

  const transferTokens = useTransferTokens({
    onSuccess: async (token) => {
      showTransferedSuccessModal(token);

      onTransfered?.();
    },
    onError: showTransferedErrorModal,
  });

  return (
    <form
      onSubmit={handleSubmit((values) =>
        transferTokens(values.recepient, values.amount),
      )}
      className="space-y-6"
    >
      <Input
        label="Token"
        disabled
        value={balance.asset.symbol.toUpperCase()}
        rightElement={<Chr className="h-6 w-6" />}
      />
      <Input
        {...register("recepient")}
        label="Receiver"
        error={!!errors.recepient}
        info={errors.recepient?.message ?? ""}
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

      <div className="space-y-4">
        <div className="flex justify-between gap-2 text-sm leading-none">
          <p className="text-muted-foreground">Arrival time</p>
          <hr className="flex-1 self-end border-0 border-t border-dotted border-muted" />
          <p className="text-bold">Instantly</p>
        </div>
        <div className="flex justify-between gap-2 text-sm leading-none">
          <p className="text-muted-foreground">Network fee</p>
          <hr className="flex-1 self-end border-0 border-t border-dotted border-muted" />
          <p className="text-bold">0.00 CHR</p>
        </div>
      </div>

      <Button type="submit" className="w-full" disabled={isSubmitting}>
        {isSubmitting ? <LoaderCubes /> : "Request transfer"}
      </Button>
    </form>
  );
}
