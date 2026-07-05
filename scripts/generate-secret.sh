#!/usr/bin/env sh
set -eu
openssl rand -base64 48 | tr -d '\n'
printf '\n'
