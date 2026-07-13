#!/bin/sh
set -e

mkdir -p "${DOCUMENT_STORAGE_ROOT:-/app/data/uploads}"
chown -R synapse:synapse "${DOCUMENT_STORAGE_ROOT:-/app/data/uploads}"

exec su-exec synapse "$@"
