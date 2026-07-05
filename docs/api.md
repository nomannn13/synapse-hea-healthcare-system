# API overview

All protected endpoints use `Authorization: Bearer <access-token>`. Refresh tokens are held in an HttpOnly cookie.

## Public

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/password-reset/request`
- `POST /api/v1/auth/password-reset/confirm`
- `GET /api/v1/departments`
- `GET /api/v1/doctors`
- `GET /api/v1/doctors/{id}/availability`

## Authenticated

- `/api/v1/dashboard`
- `/api/v1/profile`
- `/api/v1/appointments`
- `/api/v1/medical-records`
- `/api/v1/prescriptions`
- `/api/v1/invoices`
- `/api/v1/documents`
- `/api/v1/notifications`
- `/api/v1/search`

## Doctor

- `GET|POST /api/v1/doctors/me/availability`
- `DELETE /api/v1/doctors/me/availability/{id}`
- Prescription creation and status transitions
- Medical-record and urgent-case operations

## Administrator

- `/api/v1/admin/users`
- `/api/v1/admin/resources`
- `/api/v1/admin/audit-logs`
- `/api/v1/admin/reports/summary`
- `POST|PATCH /api/v1/departments`
- Invoice creation and status transitions

Interactive request/response schemas are available in Swagger UI at `/swagger-ui.html`.
