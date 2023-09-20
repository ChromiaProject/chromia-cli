#!/bin/sh
ssh-agent -a /tmp/chr-test-ssh-socket
SSH_AUTH_SOCK="/tmp/chr-test-ssh-socket" ssh-add $1