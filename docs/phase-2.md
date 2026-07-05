# Phase 2 engineering scope

Phase 2 extends the working HEA foundation with clinical and administrative workflows that share the same security, audit, notification, and persistence model.

## Billing

Invoices are aggregates containing immutable line calculations, a currency, due date, optional appointment link, and a controlled lifecycle:

`DRAFT → ISSUED → PAID`

A draft or issued invoice can be cancelled, but a paid invoice cannot. Totals are calculated on the server using `BigDecimal`; the browser never supplies a trusted final total.

## Prescriptions

A prescription belongs to one patient and one doctor and contains structured medication items. Drafts are private to the authoring doctor and administrators. Issuing a prescription timestamps it, notifies the patient, and makes the PDF available.

## Clinical documents

The database stores document metadata while bytes are stored outside MySQL. Uploaded names are never used as storage paths. The system generates a random storage name and accepts PDF, JPEG, PNG, and plain text files up to 10 MB.

## Doctor schedule

Custom availability blocks are persisted and checked for overlap. Appointment slot generation uses the configured blocks when present and retains transactional appointment conflict checks.

## Departments, search, audits, and analytics

Administrators can create and update departments. Global search returns only data appropriate to the caller’s role. Audit entries can be filtered by action, entity type, and actor. Dashboard data is aggregated by the backend rather than reconstructed from complete datasets in the browser.

## Email delivery

Notification creation writes the in-app notification first. Development uses a logging gateway. SMTP can be enabled through environment variables without changing application code.
