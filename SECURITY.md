# Security policy

## Supported release

Security fixes target the latest `2.x` release.

## Reporting

Do not open a public issue for a suspected vulnerability. Contact the repository owner privately with the affected endpoint, reproduction steps, impact, and suggested mitigation when available.

## Security model

- Passwords use BCrypt and are never stored in plaintext.
- Access tokens are short-lived JWTs; refresh tokens are opaque, rotated, HttpOnly, and stored only as SHA-256 hashes.
- Authorization is enforced by Spring Security and by ownership checks in domain services.
- Invoice totals are calculated on the server with `BigDecimal`.
- Clinical documents use generated storage names, restricted content types, a 10 MB limit, and audited access operations.
- Database records use optimistic locking; appointment creation also uses a pessimistic doctor lock and overlap checks.
- Local email defaults to logging. SMTP credentials must come from environment variables or a secret manager.

## Production requirements

Before any real deployment, add TLS, encrypted managed storage, malware scanning, a secrets manager, secure backups, document retention and deletion policies, consent controls, external penetration testing, dependency scanning, centralized logging, incident response, and legal/regulatory review.

Never commit `.env`, SMTP passwords, JWT secrets, patient data, exported documents, or database backups.
