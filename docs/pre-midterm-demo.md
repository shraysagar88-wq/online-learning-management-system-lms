# Pre-Midterm Demonstration Guide

Use the seeded accounts in `README.md` to demonstrate the implemented backend features.

## Student

1. Sign in as `student@gu-lms.com`.
2. Browse the course catalog and enroll in a course.
3. Open **My learning**, open the enrolled course, and complete lessons.
4. Open **Assignments**, submit work, and confirm the activity entry.

## Instructor

1. Sign in as `instructor@gu-lms.com`.
2. Create a course and an assignment.
3. Select the assignment, view submissions, and save a grade and feedback.
4. Review the instructor analytics and enrollment count.

## Administrator

1. Sign in as `admin@gu-lms.com`.
2. Create, edit, and delete users and courses.
3. Review platform analytics, activity, and settings.
4. Confirm unauthorized role access returns HTTP 401/403.

## API evidence

- `POST /api/auth/login` and `POST /api/auth/logout` demonstrate authentication and server-side session lifecycle.
- `GET /api/health/database` demonstrates direct JDBC connectivity with `SELECT 1`.
- Course, enrollment, lesson, assignment, submission, analytics, settings, and activity endpoints are listed in `ApiController` and exercised by the dashboard flows above.
