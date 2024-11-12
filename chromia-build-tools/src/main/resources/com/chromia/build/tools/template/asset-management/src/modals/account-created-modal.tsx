import { create, useModal } from "@ebay/nice-modal-react";

import Button from "@/components/chromia-ui-kit/button";
import { DrawerDialog } from "@/components/ui/drawer-dialog";
import TadaBg from "@/components/ui/tada-bg";

const AccountCreatedModal = create(() => {
  const modal = useModal();

  return (
    <DrawerDialog
      DialogContentProps={{ "aria-describedby": undefined }}
      footer={
        <Button variant="secondary" onClick={modal.hide}>
          Great 🎉
        </Button>
      }
      open={modal.visible}
      title={
        <span className="grid place-items-center py-20">
          <TadaBg className="absolute" />
          Thanks for creation an account!
        </span>
      }
      onOpenChange={modal.hide}
    ></DrawerDialog>
  );
});

export const useAccountCreatedModal = () => {
  const modal = useModal(AccountCreatedModal);

  return modal;
};
