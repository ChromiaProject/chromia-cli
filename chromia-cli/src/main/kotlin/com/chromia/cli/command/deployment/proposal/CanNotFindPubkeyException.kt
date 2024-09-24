package com.chromia.cli.command.deployment.proposal

import com.github.ajalt.clikt.core.PrintMessage

class CanNotFindPubkeyException : PrintMessage("Could not find a pubkey to act on proposals with, please specify config file to be used", 1)