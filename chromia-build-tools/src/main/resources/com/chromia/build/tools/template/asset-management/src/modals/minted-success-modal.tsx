import { create, useModal } from "@ebay/nice-modal-react";

import Button from "@/components/chromia-ui-kit/button";
import SuccessIcon from "@/components/icons/success";
import { DrawerDialog } from "@/components/ui/drawer-dialog";
import TadaBg from "@/components/ui/tada-bg";

const MintedSuccessModal = create<{ amount: number; ticker: string }>(
  ({ amount, ticker }) => {
    const modal = useModal();

    return (
      <DrawerDialog
        visuallyHiddenTitle
        DialogContentProps={{ "aria-describedby": undefined }}
        footer={
          <Button variant="secondary" onClick={modal.hide}>
            Great 🎉
          </Button>
        }
        open={modal.visible}
        title="You have successfully minted"
        onOpenChange={modal.hide}
      >
        <div className="space-y-6">
          <div className="grid place-items-center">
            <TadaBg className="max-w-full" />
            <SuccessIcon className="absolute scale-y-75 text-[6rem] opacity-60 blur-2xl" />
            <SuccessIcon className="absolute text-[6rem]" />
          </div>
          <div>
            <p className="text-center text-xs text-muted-foreground">
              You have successfully minted
            </p>
            <p className="text-center font-serif text-3xl font-bold text-accent">
              {Intl.NumberFormat().format(amount)} {ticker.toUpperCase()}
            </p>
          </div>
        </div>
      </DrawerDialog>
    );
  },
);

export const useMintedSuccessModal = () => {
  const modal = useModal(MintedSuccessModal);

  return modal;
};
