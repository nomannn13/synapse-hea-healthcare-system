# Synapse HEA — Healthcare Management System

A full-stack healthcare workflow project built with **Node.js, Express, MySQL, HTML, CSS, and JavaScript**.

This repository is an individual engineering revision of a University of Trento Software Engineering group project. It focuses on turning the original academic prototype into a coherent, testable portfolio project with clear authentication, role-based APIs, database consistency, appointment workflows, dashboards, Docker support, and CI.

> Original group concept: Miranda Duraku, Ritik Kumar Rai, Meliza Bodurri, and Nomaan Munshi.  
> Individual revision and maintenance: Nomaan Munshi.  
> See [`docs/PROJECT_ORIGIN.md`](docs/PROJECT_ORIGIN.md) for attribution details.

## What the system demonstrates

### Patient workflow
- Create a patient account
- Sign in with password plus demo OTP
- View appointment statistics
- Browse available doctors
- Request appointments
- View appointment history
- View recent medical records
- Read notifications
- Update profile information

### Doctor workflow
- View a role-specific dashboard
- Manage profile and weekly availability
- View appointment requests
- Confirm or reject requests
- Complete appointments
- View assigned patients
- Create and update medical records

### Administrator workflow
- View system statistics
- Inspect resources
- View audit logs
- Generate aggregate reports
- Assign rooms
- Activate, deactivate, or change user roles

## Architecture

```text
Browser UI
   ↓ HTTP/JSON
Express routes
   ↓
JWT authentication
   ↓
Role authorization
   ↓
Controllers and business rules
   ↓
MySQL connection pool
   ↓
Relational database
```

The frontend never connects directly to MySQL. Protected operations pass through JWT authentication and role checks before reaching controller logic.

## Technology stack

| Layer | Technology |
|---|---|
| Frontend | HTML5, CSS3, Vanilla JavaScript |
| Backend | Node.js 18+, Express 4 |
| Database | MySQL 8 |
| Authentication | JWT |
| Password security | bcrypt |
| Additional login step | Demo OTP with expiry and attempt limits |
| Security middleware | Helmet, CORS |
| Logging | Morgan and application audit logs |
| Testing | Node test runner and route-contract checks |
| Delivery | Docker Compose and GitHub Actions |

## Repository structure

```text
.
├── backend
│   ├── config
│   ├── controllers
│   ├── middleware
│   ├── routes
│   ├── scripts
│   ├── utils
│   ├── package.json
│   └── server.js
├── database
│   └── schema.sql
├── frontend
│   ├── css
│   ├── js
│   └── pages
├── tests
├── docs
├── Dockerfile
└── docker-compose.yml
```

## Quick start with Docker

```bash
docker compose up --build
```

In another terminal, seed the demo accounts:

```bash
docker compose exec app node scripts/seedDemo.js
```

Open:

```text
http://localhost:4000
```

Demo accounts:

| Role | Email | Password |
|---|---|---|
| Admin | `admin@hea.local` | `AdminDemo123!` |
| Doctor | `doctor@hea.local` | `DoctorDemo123!` |
| Patient | `patient@hea.local` | `PatientDemo123!` |

The demo OTP is returned by the login response only when `DEMO_OTP=true`. That mode is for local academic demonstrations and must be disabled in production.

## Native setup

### Requirements
- Node.js 18 or newer
- MySQL 8

### 1. Create the database

```bash
mysql -u root -p < database/schema.sql
```

### 2. Configure the backend

```bash
cp backend/.env.example backend/.env
```

Edit the database password and JWT secret.

### 3. Install and seed

```bash
cd backend
npm ci
npm run seed
```

### 4. Start

```bash
npm start
```

Then open `http://localhost:4000`.

## Main API groups

| Base path | Purpose |
|---|---|
| `/api/auth` | Registration, password login, OTP verification, session identity |
| `/api/patient` | Patient dashboard, profile, doctors, history, notifications |
| `/api/doctor` | Doctor dashboard, profile, schedule, appointments, patients |
| `/api/appointments` | Availability, booking, cancellation, rescheduling |
| `/api/medical` | Role-aware medical-record access and updates |
| `/api/admin` | Statistics, resources, reports, audit logs, user management |

## Verification

```bash
cd backend
npm run check
npm test
```

The current automated checks verify:

- backend JavaScript syntax;
- frontend JavaScript and inline-script syntax;
- expected route registration;
- role-middleware rejection;
- public registration being forced to the patient role.

## Engineering improvements over the original academic snapshot

- fixed a fatal controller loading error;
- aligned route names and controller exports;
- standardized `req.user.id`;
- applied JWT and role middleware;
- aligned SQL queries with the database schema;
- restricted public registration to patients;
- added a working doctor API surface;
- added role-aware medical-record access;
- changed appointment booking into a request/confirmation workflow;
- added transactions and audit logging;
- replaced placeholder reports with aggregate queries;
- fixed the broken frontend auth script;
- added Docker, CI, tests, environment documentation, and attribution.

See [`CHANGELOG.md`](CHANGELOG.md) for the detailed list.

## Honest limitations

This is an educational portfolio system, not production medical software.

Before real deployment it would still need:

- a real email or SMS OTP provider;
- Redis or another shared OTP/session store;
- secure HTTP-only cookie authentication;
- rate limiting;
- complete integration and browser tests;
- database migrations;
- healthcare-specific privacy, legal, and compliance review;
- production key management and selective field encryption;
- observability, backups, recovery, and infrastructure hardening.

## How to present this project in an interview

A truthful description is:

> “I took a university team prototype and independently refactored its backend integration, authentication, role-based APIs, appointment flow, schema consistency, dashboards, tests, Docker setup, and CI. I also documented the remaining production limitations rather than claiming the prototype was deployment-ready.”

That framing is technically strong and gives proper credit to the original group work.
