package com.chromia.cli.util

const val MAINNET = "mainnet"
const val TESTNET = "testnet"
const val DEVNET1 = "devnet1"
const val DEVNET2 = "devnet2"
const val CHROMIA_PREDEFINED_TESTING_NETWORK = "chromia_predefined_testing_network_chromia_cli"

val predefinedNetworks: Map<String, List<String>> = mapOf(
    MAINNET to listOf(
        "https://system.chromaway.com",
        "https://chromia.validatrium.club",
        "https://chromia-mainnet-systemnode-1.stakin-nodes.com:7740",
        "https://chroma.node.monster:7741",
        "https://chromia.mainnet-system.nodeops.ninja:7740",
        "https://chromia-mainnet-1.dappradar.com:7740",
        "https://sys-main.chromia.coinhall.org:7740",
        "https://chromia-sp.bwarelabs.com:7740",
        "https://chromia-api.hashkey.cloud:7740",
        "https://chromia-mainnet-system-node.asymm.ventures:7740",
        "https://chr.bbbnnnbbb.net:443",
        "https://chromia-system-node.moca-services.xyz:7740",
        "https://chromia-mainnet-system.dwellir.com:443",
    ),
    TESTNET to listOf(
        "https://node0.testnet.chromia.com",
        "https://node1.testnet.chromia.com",
        "https://node2.testnet.chromia.com",
    ),
    DEVNET1 to listOf(
        "https://node0.devnet1.chromia.dev",
        "https://node1.devnet1.chromia.dev",
        "https://node2.devnet1.chromia.dev",
        "https://node3.devnet1.chromia.dev",
    ),
    DEVNET2 to listOf(
        "https://node0.devnet2.chromia.dev:7740",
        "https://node1.devnet2.chromia.dev:7740",
        "https://node2.devnet2.chromia.dev:7740",
        "https://node3.devnet2.chromia.dev:7740",
    ),
    CHROMIA_PREDEFINED_TESTING_NETWORK to listOf(
            "http://localhost:7745"
    )

)
