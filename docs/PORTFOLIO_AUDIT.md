# Portfolio audit: submitted snapshot vs personal revision vs portfolio revision

## Verdict

The personal version supplied by Nomaan was **materially better than the group-submitted snapshot**, but it was not yet suitable for a job application without further repair.

The portfolio revision in this repository keeps the strongest ideas from the personal version and fixes the remaining integration, security, documentation, and packaging problems.

## What Nomaan's personal version had already improved

Compared with the submitted group snapshot, the personal version already fixed several important defects:

1. Removed the fatal top-level `await` and misplaced appointment SQL from `patientController.js`.
2. Standardized most appointment code from `req.user.user_id` to `req.user.id`.
3. Aligned cancellation and rescheduling parameter names.
4. Added an alias so the appointment list route could register.
5. Applied JWT protection to patient, administrator, and appointment routes.
6. Corrected patient and doctor role-record inserts to better match the schema.
7. Corrected registration and login audit-log inserts.
8. Added an OTP login step.
9. Added a doctor dashboard page.
10. Rebuilt the frontend with a much stronger responsive visual design.

Those are meaningful engineering improvements. They show that the personal version was not merely a visual copy: it repaired backend integration and authentication issues that existed in the submitted snapshot.

## Why the personal version was still not portfolio-ready

The audit found these remaining issues:

- `frontend/js/auth.js` had an `Unexpected end of input` syntax error.
- Doctor routes registered zero endpoints because route names did not match controller exports.
- Medical-record routes registered zero endpoints for the same reason.
- Patient doctor search queried `doctors.department_id`, but the schema stored a text `department` field.
- Public registration still allowed a caller to request the `admin` role.
- Patient routes had authentication but not patient-only authorization.
- Appointment routes were authenticated but not restricted by operation and role.
- The OTP was returned in the API response without a production/demo switch.
- OTP attempts were unlimited and identifiers used `Math.random`.
- Login was recorded before OTP verification.
- The doctor dashboard was visually present but static.
- Reports still returned an empty array.
- Absolute `http://localhost:4000/api` URLs reduced deployment portability.
- Dynamic dashboard data was inserted with `innerHTML` without consistent escaping.
- The patient page had duplicate HTML IDs.
- The repository placed the actual source inside a nested ZIP and had an empty root page.
- The README linked to another member's repository and overclaimed incomplete features.
- There were no automated tests, CI workflow, Docker setup, accurate attribution notes, or environment template.

## What the portfolio revision fixes

### Integration
- Explicit, non-conditional route registration.
- Matching route and controller contracts for every API group.
- Standard `req.user.id` interface.
- Functional patient, doctor, administrator, appointment, and medical-record route maps.
- Startup-safe server structure that exports the Express app for testing.

### Security
- Public registration is forced to the patient role.
- JWT middleware is attached to protected routes.
- Patient, doctor, and administrator role authorization is enforced.
- OTP uses cryptographic random generation, expiry, and attempt limits.
- Demo OTP exposure is controlled by an environment flag.
- Password hashing uses bcrypt with 12 rounds in the revised flows.
- CORS is environment-configurable instead of permanently allowing every origin.
- Helmet is enabled.
- Server-side errors no longer expose raw database details.
- Audit logging uses one schema-aligned utility.

### Business logic
- Registration uses a database transaction.
- Booking checks doctor status, working schedule, future date, and conflicts.
- Appointment booking creates a pending request instead of pretending immediate doctor confirmation.
- Doctor confirmation and rejection are implemented.
- Patient and doctor notifications are created.
- Cancellation and rescheduling verify ownership or administrator access.
- Doctor dashboard, profile, schedule, appointment, completion, and patient APIs are implemented.
- Medical-record reads are role-aware and individual-record access is checked.
- Placeholder administrator reports were replaced with aggregate queries.
- Room assignment checks resource availability in a transaction.

### Database
- Controllers and schema now use the same department relationship.
- Role tables have unique user relationships.
- Appointment, medical-record, resource, shift, billing, and audit foreign keys are aligned.
- Useful indexes were added.
- The missing rejected appointment state was added.
- Demo data is created through a seed script instead of a hard-coded weak administrator row.

### Frontend
- Fixed the broken auth script.
- Replaced absolute API URLs with same-origin `/api`.
- Added role guards to dashboards.
- Connected the doctor dashboard to live API data.
- Restricted registration UI to patient accounts.
- Escaped dynamic dashboard values.
- Corrected duplicate patient-dashboard IDs.
- Updated demo credentials and project claims.

### Engineering quality
- Added syntax checks for backend JavaScript, frontend JavaScript, and inline page scripts.
- Added route-contract, authorization, registration-role, and server-smoke tests.
- Added GitHub Actions CI.
- Added Docker Compose.
- Added `.env.example` and `.gitignore`.
- Added accurate project-origin attribution.
- Replaced the misleading README with an honest portfolio README.

## Why this revision is stronger than the group submission

It is stronger because it does not merely add more features. It improves **coherence**:

- requirements map to actual routes;
- routes map to real controller exports;
- controllers use fields that exist in the schema;
- protected operations have backend authorization;
- frontend pages call endpoints that exist;
- security claims are separated from demo-only behavior;
- automated checks catch the exact class of mistakes that damaged the submitted snapshot.

That is the strongest Software Engineering lesson from the project: integration quality and traceability matter as much as writing isolated code.

## Why this is still not production medical software

The revision is a credible educational portfolio project, but it still needs:

- a real email/SMS OTP provider;
- Redis or another shared OTP store;
- rate limiting;
- HTTP-only cookie authentication;
- CSRF controls if cookies are used;
- real MySQL integration tests;
- browser end-to-end tests;
- database migrations;
- production encryption and key management;
- legal/privacy/compliance review;
- deployment monitoring, backups, and recovery.

## Safe job-application description

Use this:

> Individually refactored and extended a university team healthcare-management prototype. Fixed backend route/controller and schema integration, implemented role-based JWT APIs and demo OTP authentication, completed appointment and dashboard workflows, added audit logging, tests, Docker, and CI, and documented remaining production-security limitations.

Do not claim:

- that the original project was entirely solo;
- that the system is production-ready;
- that it is legally compliant healthcare software;
- that every feature from D1 and D2 is fully implemented;
- that demo OTP is real multi-factor authentication in production.
