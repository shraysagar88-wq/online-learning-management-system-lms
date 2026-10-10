# GU_LMS Architecture and Design

## Problem and solution

GU_LMS provides one learning workspace for three roles: administrators manage the platform, instructors manage courses and grading, and students enroll, complete lessons, and submit work.

## Architecture

```mermaid
flowchart LR
    UI[Angular 18 dashboard] -->|JSON REST + bearer token or session cookie| API[Spring Boot REST API]
    API --> AUTH[Authentication and role interceptor]
    API --> SERVICE[LmsService: validation, business rules, transactions]
    SERVICE --> JPA[Spring Data JPA repositories]
    SERVICE --> JDBC[JdbcTemplate database health check]
    JPA --> DB[(PostgreSQL / H2-compatible schema)]
    JDBC --> DB
```

## Main workflows

```mermaid
sequenceDiagram
    participant S as Student
    participant A as Angular UI
    participant C as ApiController
    participant L as LmsService
    participant D as Database
    S->>A: Sign in
    A->>C: POST /api/auth/login
    C->>L: Authenticate and create session/token
    L->>D: Read user and verify password
    C-->>A: User profile + bearer token
    S->>A: Enroll or complete lesson
    A->>C: Authenticated REST request
    C->>L: Check role and ownership
    L->>D: Transactional write
    C-->>A: Updated enrollment/progress
```

## Design decisions

- JPA is used for domain CRUD and transaction boundaries; `JdbcTemplate` is also demonstrated for a direct SQL connectivity check at `/api/health/database`.
- The existing bearer-token/local-storage flow remains supported. The API additionally stores the authenticated user ID in `HttpSession`, so server-side session authentication is available without breaking the Angular client.
- Role checks occur at the API interceptor and business-layer ownership checks protect student learning data.
