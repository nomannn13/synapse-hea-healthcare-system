# Synapse HEA — Healthcare Management System

A full-stack healthcare workflow application built with **Node.js, Express, MySQL, HTML, CSS, and JavaScript**.

Synapse HEA began as a Software Engineering group project at the University of Trento. This repository contains my independently maintained and extended portfolio revision, focused on improving backend integration, authentication, role-based authorization, database consistency, appointment workflows, dashboards, automated testing, Docker support, and continuous integration.

> This is an educational portfolio project. It is not affiliated with, endorsed by, or intended for deployment by any healthcare organization.

## Project origin and attribution

The original academic concept and initial prototype were developed collaboratively as part of university coursework.

This repository represents an independently revised and maintained version prepared by **Nomaan Munshi** for learning and portfolio purposes.

Detailed project history and attribution are available in:

[`docs/PROJECT_ORIGIN.md`](docs/PROJECT_ORIGIN.md)

---

## Main features

### Patient workflow

Patients can:

* create a patient account;
* sign in using a password and demo OTP verification;
* view appointment statistics;
* browse available doctors;
* check doctor availability;
* request appointments;
* cancel or reschedule eligible appointments;
* view appointment history;
* view recent medical records;
* read notifications;
* update personal profile information.

### Doctor workflow

Doctors can:

* access a role-specific dashboard;
* view and update their profile;
* manage weekly availability;
* review appointment requests;
* confirm or reject appointment requests;
* mark appointments as completed;
* view assigned patients;
* create medical records;
* update authorized medical records.

### Administrator workflow

Administrators can:

* view system-wide statistics;
* inspect users, patients, doctors, and appointments;
* review resources;
* view audit logs;
* generate aggregate reports;
* assign rooms;
* activate or deactivate accounts;
* update user roles.

---

## Architecture

```text
Browser interface
        ↓
HTTP and JSON requests
        ↓
Express routes
        ↓
JWT authentication
        ↓
Role-based authorization
        ↓
Controllers and business rules
        ↓
MySQL connection pool
        ↓
Relational database
```

The frontend does not connect directly to MySQL.

Protected operations are intended to pass through:

1. JWT verification;
2. authenticated-user identification;
3. role authorization;
4. controller-level business rules;
5. parameterized database queries;
6. structured JSON responses.

This separation improves maintainability, security, testing, and future extensibility.

---

## Technology stack

| Layer                         | Technology                                    |
| ----------------------------- | --------------------------------------------- |
| Frontend                      | HTML5, CSS3, Vanilla JavaScript               |
| Backend                       | Node.js 18+, Express 4                        |
| Database                      | MySQL 8                                       |
| Authentication                | JSON Web Tokens                               |
| Password security             | bcrypt                                        |
| Additional login verification | Demo OTP with expiry and attempt limits       |
| Security middleware           | Helmet and CORS                               |
| HTTP logging                  | Morgan                                        |
| Application logging           | Database-backed audit logs                    |
| Testing                       | Node.js test runner and route-contract checks |
| Containerization              | Docker and Docker Compose                     |
| Continuous integration        | GitHub Actions                                |

---

## Repository structure

```text
.
├── .github
│   └── workflows
├── backend
│   ├── config
│   ├── controllers
│   ├── middleware
│   ├── routes
│   ├── scripts
│   ├── utils
│   ├── .env.example
│   ├── package.json
│   └── server.js
├── database
│   └── schema.sql
├── docs
│   ├── PROJECT_ORIGIN.md
│   └── PORTFOLIO_AUDIT.md
├── frontend
│   ├── css
│   ├── js
│   └── pages
├── tests
├── .gitignore
├── CHANGELOG.md
├── Dockerfile
├── docker-compose.yml
└── README.md
```

---

## Authentication and authorization

The application separates authentication from authorization.

### Authentication

Authentication answers:

> Who is the user?

The login flow:

1. validates the submitted email and password;
2. looks up the user in MySQL;
3. compares the submitted password with the stored bcrypt hash;
4. creates a temporary OTP challenge;
5. verifies the OTP;
6. issues a signed JWT;
7. returns the authenticated user’s safe profile information.

### Authorization

Authorization answers:

> What is the authenticated user allowed to do?

Protected routes use role checks for:

* patients;
* doctors;
* administrators;
* shared doctor-or-admin operations;
* role-aware medical-record access.

Frontend redirects improve usability, but backend middleware is responsible for enforcing security.

---

## Appointment workflow

The appointment lifecycle follows this general flow:

```text
Patient selects doctor and date
        ↓
System checks doctor availability
        ↓
System checks scheduling conflicts
        ↓
Patient submits appointment request
        ↓
Appointment is stored as pending
        ↓
Doctor confirms or rejects request
        ↓
Patient dashboard and notifications update
```

Database transactions are used for operations where multiple related changes must succeed or fail together.

Conflict checks are performed before appointment creation or rescheduling.

---

## Medical-record access

Medical-record access is role-aware.

* Patients can view their own records.
* Doctors can create or update records within authorized workflows.
* Administrators may access administrative-level functionality where permitted.
* Unauthorized users should not be able to access records belonging to another patient.

All healthcare information in this repository is demonstration data only.

Do not use real patient or confidential medical data with this project.

---

## Quick start with Docker

### Requirements

Install:

* Docker Desktop;
* Docker Compose.

Start the application:

```bash
docker compose up --build
```

In another terminal, seed the local demonstration accounts:

```bash
docker compose exec app node scripts/seedDemo.js
```

Open the application:

```text
http://localhost:4000
```

---

## Local demonstration accounts

| Role          | Email               | Password          |
| ------------- | ------------------- | ----------------- |
| Administrator | `admin@hea.local`   | `AdminDemo123!`   |
| Doctor        | `doctor@hea.local`  | `DoctorDemo123!`  |
| Patient       | `patient@hea.local` | `PatientDemo123!` |

These credentials are exclusively for local development and demonstration.

They must not be reused in production systems.

When:

```text
DEMO_OTP=true
```

the demonstration OTP may be returned by the login response.

This behaviour is intended only for local academic demonstrations and must be disabled in any deployed environment.

---

## Native installation

### Requirements

Install:

* Node.js 18 or newer;
* npm;
* MySQL 8.

### 1. Clone the repository

```bash
git clone https://github.com/nomannn13/synapse-hea-healthcare-system.git
cd synapse-hea-healthcare-system
```

### 2. Create the database

From the project root:

```bash
mysql -u root -p < database/schema.sql
```

### 3. Configure environment variables

Copy the example environment file:

```bash
cp backend/.env.example backend/.env
```

Update the values in:

```text
backend/.env
```

Example configuration:

```env
PORT=4000

DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_local_database_password
DB_NAME=hea_database

JWT_SECRET=replace_with_a_long_random_secret
JWT_EXPIRES_IN=24h

DEMO_OTP=true
NODE_ENV=development
```

Never commit the real `.env` file.

The repository should only contain:

```text
.env.example
```

### 4. Install backend dependencies

```bash
cd backend
npm ci
```

### 5. Seed local demonstration data

```bash
npm run seed
```

### 6. Start the application

```bash
npm start
```

Open:

```text
http://localhost:4000
```

---

## Main API groups

| Base path           | Purpose                                                                          |
| ------------------- | -------------------------------------------------------------------------------- |
| `/api/auth`         | Registration, password login, OTP verification, identity and password operations |
| `/api/patient`      | Patient dashboard, profile, doctors, history and notifications                   |
| `/api/doctor`       | Doctor dashboard, profile, availability, appointments and assigned patients      |
| `/api/appointments` | Availability, booking, cancellation and rescheduling                             |
| `/api/medical`      | Role-aware medical-record access, creation and updates                           |
| `/api/admin`        | Statistics, resources, reports, audit logs and user management                   |

---

## Example request flow

A patient booking request follows a flow similar to:

```text
Frontend form submission
        ↓
POST /api/appointments
        ↓
JWT authentication middleware
        ↓
Patient-role authorization
        ↓
Request validation
        ↓
Doctor availability check
        ↓
Appointment-conflict check
        ↓
Database transaction
        ↓
Appointment creation
        ↓
Audit-log entry
        ↓
JSON response
```

---

## HTTP response conventions

The API uses standard HTTP status codes.

| Status                      | Meaning                                   |
| --------------------------- | ----------------------------------------- |
| `200 OK`                    | Request completed successfully            |
| `201 Created`               | A resource was created                    |
| `400 Bad Request`           | Input was invalid or incomplete           |
| `401 Unauthorized`          | Authentication was missing or invalid     |
| `403 Forbidden`             | The user lacked permission                |
| `404 Not Found`             | The requested resource was not found      |
| `409 Conflict`              | The request conflicted with existing data |
| `500 Internal Server Error` | An unexpected server error occurred       |

---

## Database design

The relational database contains entities such as:

* users;
* patients;
* doctors;
* appointments;
* doctor schedules;
* medical records;
* notifications;
* resources;
* audit logs.

MySQL was selected because healthcare workflows contain strongly related entities that benefit from:

* foreign keys;
* joins;
* structured constraints;
* transactions;
* consistent relational queries.

Parameterized queries are used instead of directly concatenating user input into SQL statements.

---

## Verification and testing

Install dependencies first:

```bash
cd backend
npm ci
```

Run syntax and consistency checks:

```bash
npm run check
```

Run automated tests:

```bash
npm test
```

The current automated checks cover areas such as:

* backend JavaScript syntax;
* frontend JavaScript syntax;
* inline frontend scripts;
* expected Express route registration;
* authorization-middleware rejection;
* public registration being restricted to patient accounts;
* basic server-startup behaviour.

---

## Continuous integration

The repository includes a GitHub Actions workflow.

When code is pushed or a pull request is opened, the workflow can automatically:

1. install Node.js;
2. install project dependencies;
3. run syntax checks;
4. run automated tests;
5. report whether the build passes.

This helps detect integration errors before changes are merged.

---

## Engineering improvements in this portfolio revision

This portfolio revision focuses on turning an academic prototype into a more coherent and testable software project.

Improvements include:

* resolved backend startup and controller-loading issues;
* aligned Express route names with controller exports;
* standardized authenticated-user information through `req.user.id`;
* applied JWT authentication and role-based authorization;
* aligned SQL queries with the database schema;
* restricted public registration to patient accounts;
* completed the doctor-facing API workflow;
* implemented role-aware medical-record access;
* improved appointment request, confirmation, cancellation and rescheduling flows;
* introduced database transactions for related operations;
* improved audit logging;
* implemented aggregate administrative reports;
* improved frontend authentication and dashboard integration;
* added Docker and Docker Compose support;
* added automated syntax and route checks;
* added GitHub Actions continuous integration;
* added environment documentation and project attribution.

See:

[`CHANGELOG.md`](CHANGELOG.md)

for the detailed revision history.

---

## Project scope and limitations

This is an educational portfolio system, not production medical software.

A real healthcare deployment would additionally require:

* a production email or SMS verification provider;
* Redis or another shared OTP and session store;
* secure HTTP-only cookie authentication;
* refresh-token rotation and token revocation;
* rate limiting and stronger abuse prevention;
* complete database integration tests;
* browser-based end-to-end testing;
* database migration management;
* healthcare-specific privacy and regulatory review;
* production key management;
* selective sensitive-field encryption;
* secure HTTPS deployment;
* monitoring and observability;
* automated backups;
* disaster recovery;
* infrastructure hardening;
* penetration testing;
* formal accessibility and usability validation.

The project must not be treated as certified healthcare software.

---

## Security notes

* Passwords are hashed using bcrypt.
* JWTs are signed, not encrypted.
* Demo OTP mode must be disabled outside local development.
* Real secrets must be stored outside the repository.
* `.env` files must never be committed.
* No real medical or patient information should be stored in the demonstration database.
* Backend authorization is required even when frontend role checks exist.
* HTTPS is required for secure transport in a deployed environment.

---

## Future improvements

Potential future work includes:

* production email-based OTP verification;
* secure HTTP-only cookie sessions;
* refresh-token rotation;
* Redis-backed session and OTP storage;
* API rate limiting;
* OpenAPI or Swagger documentation;
* database migrations;
* improved notification delivery;
* advanced appointment filtering;
* automated browser testing;
* accessibility improvements;
* deployment to a cloud environment;
* monitoring, logs and alerting;
* responsive dashboard improvements.

---

## Educational objectives

This project demonstrates practical experience with:

* full-stack web development;
* REST API design;
* relational database modelling;
* JWT authentication;
* role-based access control;
* password hashing;
* database transactions;
* scheduling and conflict detection;
* backend middleware;
* frontend-to-backend integration;
* automated testing;
* Docker;
* GitHub Actions;
* software-maintenance and refactoring;
* requirements-to-implementation traceability.

---

## Attribution

The original concept began as a collaborative University of Trento Software Engineering project.

This repository contains an independently maintained and extended portfolio revision by:

**Nomaan Munshi**

Detailed attribution and project history are documented in:

[`docs/PROJECT_ORIGIN.md`](docs/PROJECT_ORIGIN.md)

---

## Disclaimer

This repository is provided for educational and portfolio purposes.

It must not be used to diagnose, treat, manage, or store real healthcare information without substantial additional engineering, security, legal, privacy, and regulatory work.
