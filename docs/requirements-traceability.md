# Requirements traceability

| Requirement | Implementation |
| --- | --- |
| FR1–FR3 registration/login/password | `auth` module, BCrypt, refresh rotation, password reset mail gateway |
| FR4–FR6 appointment lifecycle | `appointment` module and transactional conflict checks |
| FR7 notifications | persisted notifications, SSE stream, optional SMTP delivery |
| FR8–FR11 availability/resources/conflicts | doctor schedule blocks, hospital resources, overlap queries, pessimistic lock |
| FR12–FR13 urgent prioritization | transparent rule-based triage service; no diagnosis |
| FR14–FR16 medical records/data sharing | medical records, prescriptions, and clinical-document APIs with RBAC |
| FR17 role access | Spring Security route and method authorization plus service ownership checks |
| FR18 dashboard | patient, doctor, and administrator dashboard aggregation |
| FR19 encryption | BCrypt for passwords; TLS and encrypted production storage required operationally |
| FR20 audit logging | immutable audit service with action/entity/actor filtering |
| FR21 search | role-aware global search across doctors, departments, and patient directory |
| FR22–FR24 profiles/history | profile, appointment, prescription, invoice, and document history |
| FR25 reports | operational summary with revenue and domain counts |
| FR26 departments | administrator-managed department entity and shared references |
| FR27–FR30 availability/performance/errors/monitoring | health checks, Actuator, metrics, pagination, validation, global errors |
| FR31 backups | documented responsibility for MySQL and document-volume backups |
| FR32 logout | refresh-token revocation and cookie removal |
| Original billing scope | invoice aggregates, status lifecycle, PDF output, and payment record state |
| Original prescription scope | structured prescription aggregate, issue/cancel lifecycle, and PDF output |
| NFR1–NFR3 security/privacy/encryption | JWT/RBAC, ownership checks, audit trail, safe filenames, production TLS boundary |
| NFR4 response time | indexed queries, pagination, Redis cache, metrics |
| NFR5 concurrent users | stateless API, pooled database connections, virtual threads |
| NFR6 no conflicts | pessimistic lock plus appointment and availability overlap checks |
| NFR8 error handling | typed domain exceptions and global API error contract |
| NFR9–NFR10 usability/accessibility | semantic React UI, labels, focus states, responsive layout |
| NFR11–NFR12 consistency/real-time | transactions, optimistic locking, SSE notifications |
| NFR13 maintainability | modular packages, ADRs, tests, CI, Flyway |
| NFR14 dashboard performance | backend aggregation rather than browser-wide table loading |
| NFR15 activity tracking | audit entries for appointments, records, invoices, prescriptions, documents, schedules, and departments |
