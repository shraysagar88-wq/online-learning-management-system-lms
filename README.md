# GU LMS

GU LMS is a full-stack online learning management system for managing courses, users, enrollments, learning materials, assignments, grades, and progress. It includes a responsive Angular frontend, a Spring Boot REST API, and PostgreSQL persistence.

## Features

- Role-based dashboards for administrators, instructors, and students
- User and course management
- Course catalog, enrollment, and learning-material access
- Assignment creation, submission, grading, and feedback
- Student progress and enrollment metrics
- Analytics overview and recent activity feed
- PostgreSQL-backed persistence with development seed data
- Bearer-token authentication with role-aware API access
- Responsive interface with search, charts, course cards, and data tables

## Technology stack

- **Frontend:** Angular 18, TypeScript, RxJS
- **Backend:** Java 25, Spring Boot 3.5, Spring Web, Spring Data JPA, Bean Validation
- **Database:** PostgreSQL 16
- **Build tools:** npm and Maven
- **Optional tooling:** Docker Compose

## Project structure

```text
backend/       Spring Boot API and domain logic
frontend/      Angular application
docs/          Architecture and project documentation
outputs/       Generated project outputs
run-local.ps1  Windows helper script for starting the application
```

## Getting started

### Prerequisites

- JDK 25
- Maven
- Node.js and npm
- PostgreSQL 16, or Docker Desktop with Docker Compose

### 1. Start PostgreSQL

From the project root, start the database with Docker:

```bash
docker compose up -d postgres
```

The included configuration creates a database named `gu_lms` with these development credentials:

```text
Host:     localhost
Port:     5432
Database: gu_lms
Username: postgres
Password: postgres
```

To use an existing PostgreSQL installation, create the database first:

```sql
CREATE DATABASE gu_lms;
```

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

The API is available at `http://localhost:8080/api`.

### 3. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm start
```

Open `http://localhost:4200` in a browser.

On Windows, `run-local.ps1` can start the database checks, backend, and frontend together when PostgreSQL, Java, Maven, and npm are installed.

## Demo accounts

| Role | Email | Password |
| --- | --- | --- |
| Administrator | `admin@gu-lms.com` | `admin123` |
| Instructor | `instructor@gu-lms.com` | `instructor123` |
| Student | `student@gu-lms.com` | `student123` |

These credentials are intended for local development only. Change them and configure a secure authentication secret before deploying the application.

## Configuration

The backend uses PostgreSQL by default. Override the connection settings with environment variables when needed:

| Variable | Default |
| --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/gu_lms` |
| `DATABASE_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` | `postgres` |
| `LMS_AUTH_SECRET` | `change-this-development-secret` |

Hibernate updates the database schema automatically during local development. Use a controlled migration process for production deployments.

## API areas

The backend exposes endpoints for authentication, users, courses, lessons, enrollments, progress, assignments, submissions, analytics, activity, settings, and database health checks.

Key resources include `/api/auth/login`, `/api/users`, `/api/courses`, `/api/enrollments`, `/api/assignments`, `/api/submissions`, `/api/analytics/overview`, `/api/activity`, and `/api/health/database`.

## Production considerations

- Replace the development authentication secret and demo credentials.
- Use HTTPS and a production-grade authentication strategy such as Spring Security with JWT or an external identity provider.
- Move the frontend API URL into environment-specific Angular configuration.
- Store uploaded course materials in durable object storage.
- Use database migrations and separate production database credentials.

## Documentation

Additional system design information is available in [`docs/architecture.md`](docs/architecture.md).
