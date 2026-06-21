# Changelog

## Portfolio revision 2.0.0

### Fixed
- Removed the fatal top-level `await` and misplaced SQL statements from the patient controller.
- Standardized authenticated user identity as `req.user.id`.
- Aligned appointment route parameters with controller parameters.
- Connected JWT and role middleware to protected APIs.
- Replaced silent conditional route registration with explicit route contracts.
- Aligned doctor, medical-record, patient, appointment, admin, and authentication handlers.
- Corrected audit-log inserts to match the SQL schema.
- Corrected patient and doctor profile creation to match schema requirements.
- Replaced the placeholder reports endpoint with real aggregate reports.
- Restricted public registration to patient accounts.
- Fixed the broken frontend authentication script.
- Removed hard-coded absolute API URLs so the frontend works when deployed on the same origin.
- Added role guards to all dashboards.
- Added HTML escaping for dynamic dashboard values.
- Corrected duplicate HTML IDs in the patient dashboard.
- Replaced the static doctor page with a live dashboard API integration.

### Added
- Demo OTP login with expiry and attempt limits.
- Doctor dashboard, schedule, appointment response, completion, and patient endpoints.
- Role-aware medical-record read/write APIs.
- Patient notification read endpoint.
- Appointment request/confirmation workflow and doctor notifications.
- Database transactions around registration, booking, room assignment, and state changes.
- Parameterized SQL throughout the new flows.
- `.env.example`, `.gitignore`, Docker Compose, and GitHub Actions CI.
- Syntax checks and route-contract tests.
- Clear project-origin and attribution documentation.

### Remaining production work
- Replace demo OTP delivery with an email/SMS provider and store hashed OTPs in Redis.
- Move browser tokens from `localStorage` to secure HTTP-only cookies.
- Add API rate limiting and CSRF protection if cookie authentication is used.
- Add full MySQL integration tests and browser end-to-end tests.
- Add migrations instead of relying only on a bootstrap schema.
- Encrypt selected medical fields with proper key management if required by the deployment.
- Add deployment, observability, backup, and disaster-recovery configuration.
