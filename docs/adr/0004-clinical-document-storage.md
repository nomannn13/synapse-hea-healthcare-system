# ADR 0004: store clinical file bytes outside the relational database

## Status

Accepted.

## Decision

Store clinical-document metadata in MySQL and file bytes in a mounted document volume. Generate random server-side storage names and preserve the original filename only as metadata.

## Rationale

This keeps relational queries small, avoids trusting user filenames as paths, and provides a direct migration path to S3-compatible object storage. The abstraction is intentionally isolated in `FileStorageService`.

## Consequences

The database and document volume must be backed up together. A production deployment also needs encryption at rest, malware scanning, retention controls, and object-level access logging.
