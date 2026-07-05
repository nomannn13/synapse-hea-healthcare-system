# ADR 0002: Short-lived JWT plus rotating opaque refresh token

**Status:** Accepted

Access tokens are short-lived JWTs sent in the Authorization header. Refresh tokens are high-entropy opaque values stored only in an HttpOnly cookie; only a SHA-256 hash is stored in MySQL. Every refresh revokes the old token and creates a replacement.

This limits database checks on normal API requests while preserving revocation and replay detection for long-lived sessions.
