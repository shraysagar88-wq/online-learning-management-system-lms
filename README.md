# GU_LMS

An online learning management system built with an Angular standalone frontend and a Spring Boot REST API.

## Included functionality

- Role-aware dashboards for Admin, Instructor, and Student users.
- Admin: user CRUD, course management, system analytics, activity feed, and settings API.
- Instructor: course creation/management, assignment workflow APIs, grading, student progress and enrollment metrics.
- Student: course catalog, enrollment, material/course access, assignment submission, progress tracking, grades and feedback API.
- PostgreSQL database with seed data for immediate local development.
- Responsive dashboard UI with search, charts, course cards, activity timeline, and user-management table.

## Demo accounts

| Role | Email | Password |
|---|---|---|
| Admin | admin@gu-lms.com | admin123 |
| Instructor | instructor@gu-lms.com | instructor123 |
| Student | student@gu-lms.com | student123 |

Legacy demo emails ending in `@learnspace.com` remain accepted at login and are migrated to `@gu-lms.com` on startup.

## Run locally

### Backend

```bash
cd backend
mvn spring-boot:run
```

The REST API runs at `http://localhost:8080/api`.

The backend now uses PostgreSQL by default. Set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` when your PostgreSQL instance does not use the defaults. The default connection is `jdbc:postgresql://localhost:5432/gu_lms`, with username `postgres` and password `postgres`.

Create the database before starting the backend:

```sql
CREATE DATABASE gu_lms;
```

Or start PostgreSQL with Docker from the project root:

```bash
docker compose up -d postgres
```

The application creates/updates its tables with Hibernate and inserts the demo accounts when the PostgreSQL database is empty. Existing H2 data is not automatically migrated; export/import it separately if it must be preserved.

### Frontend

```bash
cd frontend
npm install
npm start
```

Open `http://localhost:4200`.

## API overview

`/api/auth/login`, `/api/users`, `/api/courses`, `/api/enrollments`, `/api/assignments`, `/api/submissions`, `/api/analytics/overview`, `/api/settings`, and `/api/activity` are implemented in `ApiController`.

For production, rotate `LMS_AUTH_SECRET` and consider Spring Security/JWT, move the Angular API base URL to environment configuration, and store uploaded course materials in object storage.

## Review 1 implementation notes

- API access is authenticated with a validated bearer token and role-based endpoint protection for Admin, Instructor, and Student operations.
- Business operations are implemented in `LmsService` with transaction boundaries and centralized input validation.
- Analytics uses a bounded executor and `CompletableFuture` so the application demonstrates explicit concurrent processing without creating unbounded threads.
- Activity events are persisted in PostgreSQL. Course publication and assignment due dates are stored through the API.

## Pre-midterm implementation enhancements

- Added architecture and workflow diagrams in [`docs/architecture.md`](docs/architecture.md).
- Preserved JPA for domain CRUD and added a direct JDBC connectivity check at `GET /api/health/database`.
- Preserved the existing bearer-token/local-storage flow and added server-side `HttpSession` support with `POST /api/auth/logout`.
- Added [`docs/pre-midterm-demo.md`](docs/pre-midterm-demo.md) with concrete UI and API demonstration steps for the backend features.
