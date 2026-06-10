# SyllabusSync

A full-stack web application that helps students organize their academic workload by parsing syllabus PDFs and matching student availability with instructor office hours.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Tech Stack](#tech-stack)
- [Repository Structure](#repository-structure)
- [External Accounts Required](#external-accounts-required)
- [Environment Variables](#environment-variables)
- [Local Development Setup](#local-development-setup)
- [Database Setup](#database-setup)
- [Running the Application](#running-the-application)
- [Building for Production](#building-for-production)
- [Deployment (Azure)](#deployment-azure)
- [CI/CD Pipeline](#cicd-pipeline)
- [Architecture Overview](#architecture-overview)
- [Key Features](#key-features)
- [User Roles](#user-roles)
- [API Overview](#api-overview)
- [Known Limitations / Tech Debt](#known-limitations--tech-debt)
- [Original Team](#original-team)

---

## Project Overview

SyllabusSync solves three problems students face:

- Syllabi are long and hard to reference quickly
- Students forget deadlines and office hours
- Scheduling meetings with instructors requires back-and-forth emails

**How it works:**
1. A professor creates a course and receives a unique join code
2. Students join with a `.edu` email and that join code
3. Students (or the instructor) upload the syllabus PDF — Claude AI extracts due dates, grade breakdown, office hours, policies, etc.
4. Students enter their personal availability
5. The app shows office-hour compatibility between student and instructor schedules
6. Either party can propose a meeting; notifications and emails are sent on status changes

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Frontend | Vue.js | 3.5.25 |
| Frontend build | Vite | 7.3.1 |
| UI components | Vuetify | 4.0.7 |
| Client routing | Vue Router | 5.0.3 |
| PDF preview | PDF.js | 5.6.205 |
| Backend | Spring Boot | 3.5.10 |
| Language | Java | 17 |
| Build tool | Maven (wrapper included) | — |
| Database | MySQL | 8.x recommended |
| ORM | JPA / Hibernate | — |
| Security | Spring Security | — |
| AI parsing | Anthropic Claude API | claude-sonnet-4-20250514 |
| Email | Gmail SMTP via Spring Mail | — |
| Calendar | Canvas iCal integration | iCal4j 4.0.8 |
| Deployment | Azure App Service | — |
| CI/CD | GitHub Actions | — |

---

## Repository Structure

```
CSCD488-SeniorProject/
├── .github/workflows/
│   └── main_syllabussync.yml     # CI/CD: builds and deploys to Azure on push to main
├── backend-springboot/
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd           # Maven wrapper — no local Maven install required
│   └── src/main/
│       ├── java/com/cscd488seniorproject/syllabussyncproject/
│       │   ├── config/           # Spring Security configuration
│       │   ├── controllers/      # REST endpoints
│       │   ├── service/          # Business logic (including ClaudeService for AI parsing)
│       │   ├── entity/           # JPA entity classes (map to DB tables)
│       │   ├── repository/       # Spring Data JPA repositories
│       │   ├── dto/              # Data transfer objects
│       │   ├── meeting/          # Meeting scheduling feature (self-contained package)
│       │   ├── notification/     # In-app notification system
│       │   ├── emailMeetingNotifications/  # Email notification service
│       │   └── scheduler/        # Scheduled tasks (calendar sync)
│       └── resources/
│           └── application.yaml  # App configuration (reads from env vars)
└── frontend-vue/
    ├── package.json
    ├── vite.config.js            # Dev proxy to localhost:8080
    └── src/
        ├── views/                # Page-level components (Login, Dashboard, Calendar, etc.)
        ├── components/           # Reusable UI components
        ├── composables/
        │   └── useMe.js          # Current authenticated user context (used across all views)
        ├── router/index.js       # Client-side route definitions
        └── assets/shared.css     # Global styles shared across all views
```

---

## External Accounts Required

A new team needs to provision the following accounts and credentials. **Do not commit any credentials to the repository.**

### 1. Anthropic (Claude API)

- **Purpose:** Parses uploaded syllabus PDFs and returns structured JSON (due dates, grade breakdown, office hours, policies)
- **Model used:** `claude-sonnet-4-20250514`
- **What to do:** Create an account at [https://console.anthropic.com](https://console.anthropic.com), generate an API key, and set it as `ANTHROPIC_API_KEY`
- **Cost:** Pay-per-token; syllabus parsing uses up to 1,500 output tokens per upload. Monitor usage in the Anthropic console.
- **Note:** The feature flag `SYLLABUS_IMPORT_ENABLED` (set to `"false"` in the CI workflow) can be used to disable AI parsing without code changes if costs are a concern.

### 2. Gmail Account (SMTP)

- **Purpose:** Sends email notifications for meeting proposals, acceptances, and rejections
- **What to do:** Create or designate a Gmail account for the app. Enable 2-Step Verification, then generate an **App Password** (Google Account → Security → App Passwords). Use the app password, not the account password.
- **Set as:** `MAIL_USERNAME` (the Gmail address) and `MAIL_PASSWORD` (the app password)

### 3. MySQL Database

- **Purpose:** Primary data store for all application data
- **What to do:** Provision a MySQL 8.x database (local, cloud-hosted, or Azure Database for MySQL). Create the schema manually — see [Database Setup](#database-setup).
- **Set as:** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

### 4. Azure App Service (for deployment)

- **Purpose:** Hosts the production application as a single JAR
- **What to do:** Create an Azure Web App (Java 17, Linux) named to match the workflow, or update the `app-name` field in `.github/workflows/main_syllabussync.yml`. Configure OIDC federated credentials between the Azure app registration and GitHub (no long-lived secrets stored in the repo — see [CI/CD Pipeline](#cicd-pipeline)).
- **Required GitHub secrets (set in repo Settings → Secrets):**
  - `AZUREAPPSERVICE_CLIENTID_*`
  - `AZUREAPPSERVICE_TENANTID_*`
  - `AZUREAPPSERVICE_SUBSCRIPTIONID_*`

  The current secrets reference specific IDs tied to the original team's Azure subscription; a new team must replace them with their own.

---

## Environment Variables

All sensitive configuration is read from environment variables. **Never hard-code these values.**

| Variable | Where used | Description |
|---|---|---|
| `ANTHROPIC_API_KEY` | `application.yaml` | Anthropic Claude API key for PDF parsing |
| `DB_URL` | `application.yaml` | JDBC URL, e.g. `jdbc:mysql://localhost:3306/syllabussync` |
| `DB_USERNAME` | `application.yaml` | MySQL username |
| `DB_PASSWORD` | `application.yaml` | MySQL password |
| `MAIL_USERNAME` | `application.yaml` | Gmail address used for sending notifications |
| `MAIL_PASSWORD` | `application.yaml` | Gmail App Password (not the account password) |
| `SYLLABUS_IMPORT_ENABLED` | Frontend build / Azure | Feature flag — `"true"` enables AI syllabus parsing; `"false"` disables it |

For local development, set these as shell environment variables before starting the backend, or configure them in your IDE's run configuration. Do not create a `.env` file inside the repo without adding it to `.gitignore` first.

---

## Local Development Setup

### Prerequisites

- Java 17 (JDK)
- Node.js 20+
- MySQL 8.x running locally
- Git

### 1. Clone the repository

```bash
git clone <repo-url>
cd CSCD488-SeniorProject
```

### 2. Set up the database

See [Database Setup](#database-setup) below.

### 3. Configure environment variables

```bash
export DB_URL=jdbc:mysql://localhost:3306/syllabussync
export DB_USERNAME=your_mysql_user
export DB_PASSWORD=your_mysql_password
export ANTHROPIC_API_KEY=sk-ant-...
export MAIL_USERNAME=your-app-email@gmail.com
export MAIL_PASSWORD=your-gmail-app-password
```

### 4. Start the backend

```bash
cd backend-springboot
./mvnw spring-boot:run
```

The backend starts on **http://localhost:8080**.

### 5. Start the frontend

```bash
cd frontend-vue
npm install
npm run dev
```

The frontend dev server starts on **http://localhost:5173** and proxies all `/api` requests to the backend automatically (configured in `vite.config.js`).

---

## Database Setup

Hibernate is configured with `ddl-auto: validate` — it will **not** auto-create or migrate the schema. You must create the database and tables manually before starting the backend.

### Option A: Let Hibernate generate the schema (dev only)

Temporarily change `ddl-auto` in `application.yaml` from `validate` to `create` (or `update`), start the app once to generate tables, then change it back.

> **Do not use `create` in production** — it drops and recreates all tables on every startup.

### Option B: Export DDL from Hibernate (recommended)

Add the following to `application.yaml` temporarily and start the app with an empty database:

```yaml
spring:
  jpa:
    properties:
      javax.persistence.schema-generation.scripts.action: create
      javax.persistence.schema-generation.scripts.create-target: schema.sql
```

This writes the DDL to `schema.sql`, which you can then use to initialize any database.

### Core tables (derived from JPA entities)

The schema includes these key tables:

- `useraccount` — users with roles: `STUDENT`, `TA`, `PROFESSOR`
- `course` — courses identified by `(ClassCode, Quarter, Year)` composite key with a unique `JoinCode`
- `syllabuses` — AI-extracted syllabus data stored as JSON columns (due dates, grade breakdown, etc.)
- `class_enrollment` — student enrollment records
- `teaches_relation` / `assists_relation` / `ta_relation` — role-to-course relationships
- `office_hours_schedule` / `office_hours_exception` — instructor availability
- `external_event` — imported calendar events
- `calendar_subscription` — Canvas iCal feed subscriptions
- `meeting` — meeting requests and their status
- `notification` — in-app notifications
- `email_notification` — email send log

**Naming strategy:** `PhysicalNamingStrategyStandardImpl` — table and column names match entity field names exactly (no snake_case conversion).

---

## Running the Application

### Development (recommended)

Run backend and frontend separately as described in [Local Development Setup](#local-development-setup). The Vite dev server provides hot reload and proxies API calls.

### Production-like (single JAR)

```bash
# Build the Vue frontend
cd frontend-vue
npm ci
npm run build

# Copy dist into Spring Boot static resources
cp -r dist/* ../backend-springboot/src/main/resources/static/

# Build and run the JAR
cd ../backend-springboot
./mvnw clean package -DskipTests
java -jar target/syllabussyncproject-*.jar
```

The app is then available at **http://localhost:8080**.

---

## Building for Production

The GitHub Actions pipeline does this automatically on push to `main`. To do it manually:

```bash
# 1. Build frontend (disable AI parsing for production)
cd frontend-vue
VITE_SYLLABUS_IMPORT_ENABLED=false npm run build

# 2. Copy into backend static folder
rm -rf ../backend-springboot/src/main/resources/static/*
cp -r dist/* ../backend-springboot/src/main/resources/static/

# 3. Build backend JAR (skip tests for speed)
cd ../backend-springboot
./mvnw -B clean package -DskipTests
# Output: target/syllabussyncproject-0.0.1-SNAPSHOT.jar
```

---

## Deployment (Azure)

The app is deployed as a single executable JAR to **Azure App Service**.

### Azure App Service settings

The following **Application Settings** must be configured in the Azure portal (or via Azure CLI) for the Web App:

| Setting | Value |
|---|---|
| `DB_URL` | JDBC URL for your Azure MySQL or remote DB |
| `DB_USERNAME` | DB username |
| `DB_PASSWORD` | DB password |
| `ANTHROPIC_API_KEY` | Your Anthropic API key |
| `MAIL_USERNAME` | Gmail address |
| `MAIL_PASSWORD` | Gmail App Password |
| `SYLLABUS_IMPORT_ENABLED` | `false` (set by CI) |
| `JAVA_OPTS` | `-Dserver.port=80` (Azure routes port 80 to 8080 by default; adjust as needed) |

### Connecting to Azure Database for MySQL

If using Azure Database for MySQL, append `?useSSL=true&requireSSL=false` or configure SSL appropriately in the JDBC URL.

---

## CI/CD Pipeline

**File:** `.github/workflows/main_syllabussync.yml`

Triggers on push to `main` or manual dispatch (`workflow_dispatch`).

**Steps:**
1. Build the Vue frontend with `VITE_SYLLABUS_IMPORT_ENABLED=false`
2. Copy the Vue `dist/` output into `backend-springboot/src/main/resources/static/`
3. Build the Spring Boot JAR (`mvn clean package -DskipTests`)
4. Authenticate to Azure using OIDC federated credentials (no long-lived secrets)
5. Set `SYLLABUS_IMPORT_ENABLED=false` as an Azure App Setting
6. Deploy the JAR to the Azure Web App named `syllabussync`

### Updating for a new Azure subscription

The workflow references three GitHub secrets tied to the original team's Azure app registration:

- `AZUREAPPSERVICE_CLIENTID_EF2E571B217646A98C92CCD138B91672`
- `AZUREAPPSERVICE_TENANTID_446BC23E295B41A08802B992CCEBD962`
- `AZUREAPPSERVICE_SUBSCRIPTIONID_0BA6463238AF42BFAF8DA82F5559C523`

To use a new Azure subscription:
1. Create an Azure App Registration with federated credentials for GitHub Actions
2. Add your new Client ID, Tenant ID, and Subscription ID as GitHub repository secrets
3. Update the secret names in the workflow file to match
4. Update `app-name: 'syllabussync'` in the workflow to your new Azure Web App name

---

## Architecture Overview

```
Browser
  │
  ├─ Dev:  http://localhost:5173  (Vite dev server, proxies /api → :8080)
  └─ Prod: https://<azure-app>.azurewebsites.net
                │
                ▼
        Spring Boot JAR (:8080)
          ├── Spring Security (session-based auth, BCrypt passwords)
          ├── REST Controllers (/api/*)
          │     └── SpaController (serves Vue index.html for all non-API routes)
          ├── Service layer
          │     └── ClaudeService → Anthropic API (PDF parsing)
          │     └── EmailService → Gmail SMTP
          │     └── CanvasCalendarService → Canvas iCal feeds
          └── JPA Repositories → MySQL
```

**Authentication:** Session cookie (`JSESSIONID`). Spring Security form login at `/api/login`. All routes except `/`, `/login`, `/signup`, `/api/health` require authentication.

**Frontend routing:** Vue Router handles client-side navigation. The `SpaController` in Spring Boot forwards all non-API, non-static requests to `index.html` so that page refreshes work correctly.

---

## Key Features

### Syllabus Parsing (AI)

Uploading a PDF sends the base64-encoded file to the Anthropic Claude API (`claude-sonnet-4-20250514`). Claude returns structured JSON with confidence levels (`high` / `medium` / `low`) for each extracted field:

- Class meeting times and location
- Office hours
- Grade scale and grade breakdown
- Pass conditions
- Attendance policy
- Due dates
- Late work policy
- AI policy

The response is stored in the `syllabuses` table as JSON columns. Students can review and manually correct low-confidence fields.

### Office Hours Compatibility

Students enter their weekly availability. The app compares this against the professor's and TAs' `office_hours_schedule` records and highlights overlapping windows.

### Meeting Scheduling

Any party can propose a meeting by selecting a time block. The other party receives an in-app notification and an email. Meetings can be accepted, declined, or cancelled; each state change triggers notifications.

### Calendar View

Aggregates extracted due dates from all enrolled courses into a single calendar. Also supports subscribing to Canvas iCal feeds via `CanvasCalendarService`.

### Workload Projections

`WorkloadProjections.vue` and `workload.js` estimate study hours per week based on the grade weight of upcoming assignments.

---

## User Roles

| Role | Capabilities |
|---|---|
| `STUDENT` | Join courses, upload syllabi, view calendar, enter availability, schedule meetings |
| `PROFESSOR` | Create courses, manage office hours, upload syllabi, propose meetings |
| `TA` | Assist courses (linked via `assists_relation`), manage availability, participate in meetings |

Role assignment is stored in the `useraccount` table. Role-based access is enforced via Spring Security's method security (`MethodSecurityConfig`).

---

## API Overview

All endpoints are under `/api`. Key groups:

| Path prefix | Controller | Description |
|---|---|---|
| `/api/login`, `/api/signup` | `AuthController` | Authentication |
| `/api/classes` | `ClassesController` | Course CRUD and enrollment |
| `/api/syllabus` | `SyllabusController` | Syllabus upload, parse, save, retrieve |
| `/api/officehours` | `OfficeHoursController` | Office hours schedule management |
| `/api/availability` | `AvailabilityController` | Student/TA availability |
| `/api/meetings` | `MeetingController` | Meeting proposals and status updates |
| `/api/notifications` | `NotificationController` | In-app notifications |
| `/api/calendar` | `CanvasCalendarController` | Canvas calendar subscriptions |
| `/api/profile-photo` | `ProfilePhotoController` | Profile photo upload/retrieve |
| `/api/health` | `HealthController` | Health check (no auth required) |

---

## Known Limitations / Tech Debt

- **No database migration tool.** There is no Flyway or Liquibase setup. Schema changes require manual SQL scripts or a temporary switch to `ddl-auto: update`. A new team should add a migration tool early.
- **CSRF disabled.** Spring Security's CSRF protection is turned off (`csrf.disable()`). This is acceptable for a same-origin SPA with session cookies but should be reviewed before deploying to a public-facing production environment.
- **`SYLLABUS_IMPORT_ENABLED` is hardcoded to `false` in CI.** Enabling AI parsing in production requires changing the workflow and verifying Anthropic billing is set up.
- **Profile photos stored as BLOBs.** Large photos will increase database size significantly. Migrating to object storage (e.g., Azure Blob Storage) is recommended for scale.
- **No unit or integration test suite.** `SyllabusSyncApplicationTests.java` is a placeholder. Tests should be written before extending core features.
- **`.edu` email enforcement.** The signup flow expects `.edu` emails, but the enforcement is client-side only. A new team should add server-side validation.

---

## Original Team

- Raul Reano
- Alejandro Ramirez
- Benjamin Burns

Course: CSCD 488 Senior Project, Eastern Washington University
