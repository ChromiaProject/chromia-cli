import { useMemo } from "react";

import { useAccount, useConnectors } from "wagmi";

export const useEthereumProvider = () => {
  const { connector } = useAccount();
  const allConnectors = useConnectors();

  const matchingConnector = allConnectors.find((c) => c.id === connector?.id);
  return useMemo(() => {
    return matchingConnector?.getProvider();
  }, [matchingConnector]);
};
