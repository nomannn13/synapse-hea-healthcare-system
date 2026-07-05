# GitHub publishing checklist

- Replace the repository URL placeholders and confirm your preferred author name.
- Generate a new `JWT_SECRET`; never publish `.env`.
- Run backend tests and the frontend check locally.
- Start Docker Compose and run `scripts/smoke-test.sh`.
- Capture your own screenshots of patient, doctor, and admin flows.
- Use a clear repository description and topics: `java`, `spring-boot`, `react`, `typescript`, `mysql`, `redis`, `hospital-management`.
- Publish only code you have read and can explain.
- Use several truthful commits rather than forging dates or pretending every line was handwritten without assistance.
- Keep demo data fictional.
- Turn on GitHub secret scanning and Dependabot after creating the repository.
