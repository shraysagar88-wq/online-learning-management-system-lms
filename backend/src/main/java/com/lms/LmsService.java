package com.lms;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import jakarta.annotation.PreDestroy;

@Service
class LmsService {
    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final SettingsRepository settings;
    private final ActivityRepository activities;
    private final LessonRepository lessons;
    private final LessonCompletionRepository completions;
    private final PasswordEncoder passwordEncoder;
    private final ExecutorService analyticsExecutor = Executors.newFixedThreadPool(2);

    LmsService(UserRepository users, CourseRepository courses, EnrollmentRepository enrollments,
               AssignmentRepository assignments, SubmissionRepository submissions, SettingsRepository settings,
               ActivityRepository activities, LessonRepository lessons, LessonCompletionRepository completions, PasswordEncoder passwordEncoder) {
        this.users = users; this.courses = courses; this.enrollments = enrollments; this.assignments = assignments;
        this.submissions = submissions; this.settings = settings; this.activities = activities; this.lessons = lessons; this.completions = completions;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    User authenticate(String email, String rawPassword) {
        User user = users.findByEmail(email).orElse(null);
        if (user == null || !user.isActive() || rawPassword == null || !matchesPassword(user, rawPassword))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        return user;
    }

    @Transactional
    User register(String name, String email, String password) {
        requireText(name, "name", 100); requireEmail(email); requirePassword(password);
        if (users.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        return users.save(new User(name, email, passwordEncoder.encode(password), Role.STUDENT));
    }

    @Transactional(readOnly = true) List<User> allUsers() { return users.findAll(); }

    @Transactional
    User createUser(String name, String email, String password, Role role) {
        requireText(name, "name", 100); requireEmail(email); requirePassword(password);
        if (users.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        return users.save(new User(name, email, passwordEncoder.encode(password), role));
    }

    @Transactional
    User updateUser(Long id, Map<String, Object> values) {
        User user = users.findById(id).orElseThrow(() -> notFound("User not found"));
        if (values.containsKey("name")) { requireText((String) values.get("name"), "name", 100); user.setName((String) values.get("name")); }
        if (values.containsKey("email")) { requireEmail((String) values.get("email")); user.setEmail(((String) values.get("email")).toLowerCase(Locale.ROOT)); }
        if (values.containsKey("role")) user.setRole(parseRole(values.get("role")));
        if (values.containsKey("active")) user.setActive(Boolean.TRUE.equals(values.get("active")));
        return users.save(user);
    }

    @Transactional
    void deleteUser(Long id) { if (!users.existsById(id)) throw notFound("User not found"); users.deleteById(id); }

    @Transactional(readOnly = true) List<Course> allCourses() { return courses.findAll(); }
    @Transactional(readOnly = true) Course course(Long id) { return courses.findById(id).orElseThrow(() -> notFound("Course not found")); }

    @Transactional
    Course createCourse(Map<String, Object> values) {
        String title = text(values, "title", 150); String description = optionalText(values, "description", 4000);
        return courses.save(new Course(title, description, optionalText(values, "syllabus", 4000), optionalText(values, "category", 80), optionalText(values, "level", 40), number(values, "instructorId"), optionalText(values, "instructorName", 100)));
    }

    @Transactional
    Course updateCourse(Long id, Map<String, Object> values) {
        Course course = course(id);
        if (values.containsKey("title")) course.setTitle(text(values, "title", 150));
        if (values.containsKey("description")) course.setDescription(optionalText(values, "description", 4000));
        if (values.containsKey("syllabus")) course.setSyllabus(optionalText(values, "syllabus", 4000));
        if (values.containsKey("category")) course.setCategory(optionalText(values, "category", 80));
        if (values.containsKey("level")) course.setLevel(optionalText(values, "level", 40));
        if (values.containsKey("published")) course.setPublished(Boolean.TRUE.equals(values.get("published")));
        return courses.save(course);
    }

    @Transactional
    void deleteCourse(Long id) { if (!courses.existsById(id)) throw notFound("Course not found"); courses.deleteById(id); }

    @Transactional
    Enrollment enroll(Long studentId, Long courseId) {
        if (studentId == null || courseId == null) throw bad("studentId and courseId are required");
        if (enrollments.findByStudentIdAndCourseId(studentId, courseId).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled");
        Course course = course(courseId); Enrollment result = enrollments.save(new Enrollment(studentId, courseId, course.getTitle()));
        record("ENROLLMENT", "A student enrolled in " + course.getTitle()); return result;
    }

    @Transactional
    Enrollment updateProgress(Long id, int progress) { Enrollment enrollment = enrollments.findById(id).orElseThrow(() -> notFound("Enrollment not found")); enrollment.setProgress(Math.max(0, Math.min(100, progress))); return enrollments.save(enrollment); }
    @Transactional(readOnly = true) List<Enrollment> studentEnrollments(Long id) { return enrollments.findByStudentId(id); }
    @Transactional(readOnly = true) long enrollmentCount(Long id) { return enrollments.countByCourseId(id); }

    @Transactional(readOnly = true)
    List<Map<String, Object>> learningLessons(Long studentId, Long courseId) {
        verifyEnrollment(studentId, courseId);
        return lessons.findByCourseIdOrderByLessonOrder(courseId).stream().map(lesson -> lessonView(studentId, lesson)).toList();
    }

    @Transactional
    Map<String, Object> completeLesson(Long studentId, Long lessonId) {
        Lesson lesson = lessons.findById(lessonId).orElseThrow(() -> notFound("Lesson not found"));
        verifyEnrollment(studentId, lesson.getCourseId());
        completions.findByStudentIdAndLessonId(studentId, lessonId).orElseGet(() -> completions.save(new LessonCompletion(studentId, lessonId)));
        Enrollment enrollment = enrollments.findByStudentIdAndCourseId(studentId, lesson.getCourseId()).orElseThrow(() -> notFound("Enrollment not found"));
        List<Lesson> courseLessons = lessons.findByCourseIdOrderByLessonOrder(lesson.getCourseId());
        List<Long> lessonIds = courseLessons.stream().map(Lesson::getId).toList();
        long completed = lessonIds.isEmpty() ? 0 : completions.countByStudentIdAndLessonIdIn(studentId, lessonIds);
        enrollment.setProgress(courseLessons.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / courseLessons.size()));
        enrollments.save(enrollment);
        record("LESSON", "A student completed " + lesson.getTitle());
        return Map.of("lessonId", lessonId, "completed", true, "progress", enrollment.getProgress(), "status", enrollment.getStatus());
    }

    private void verifyEnrollment(Long studentId, Long courseId) { if (enrollments.findByStudentIdAndCourseId(studentId, courseId).isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this course"); }
    private Map<String, Object> lessonView(Long studentId, Lesson lesson) { return Map.of("id", lesson.getId(), "courseId", lesson.getCourseId(), "title", lesson.getTitle(), "description", lesson.getDescription(), "lessonOrder", lesson.getLessonOrder(), "completed", completions.findByStudentIdAndLessonId(studentId, lesson.getId()).isPresent()); }

    @Transactional(readOnly = true) List<Assignment> courseAssignments(Long id) { return assignments.findByCourseId(id); }
    @Transactional
    Assignment createAssignment(Map<String, Object> values) {
        Long courseId = number(values, "courseId"); course(courseId); String title = text(values, "title", 150);
        int points = integer(values, "points", 1, 1000); Assignment assignment = new Assignment(courseId, title, optionalText(values, "description", 4000), points);
        if (values.get("dueDate") != null) assignment.setDueDate(java.time.LocalDateTime.parse(values.get("dueDate").toString()));
        return assignments.save(assignment);
    }
    @Transactional
    Submission submit(Map<String, Object> values) { Long assignmentId = number(values, "assignmentId"); if (!assignments.existsById(assignmentId)) throw notFound("Assignment not found"); Submission result = submissions.save(new Submission(assignmentId, number(values, "studentId"), text(values, "studentName", 100), text(values, "content", 10000))); record("SUBMISSION", text(values, "studentName", 100) + " submitted an assignment"); return result; }
    @Transactional(readOnly = true) List<Submission> assignmentSubmissions(Long id) { return submissions.findByAssignmentId(id); }
    @Transactional
    Submission grade(Long id, Map<String, Object> values) { Submission submission = submissions.findById(id).orElseThrow(() -> notFound("Submission not found")); int grade = integer(values, "grade", 0, 1000); submission.grade(grade, optionalText(values, "feedback", 4000)); record("GRADE", "An assignment submission was graded"); return submissions.save(submission); }

    CompletableFuture<Map<String, Object>> analytics(Long instructorId) {
        return CompletableFuture.supplyAsync(() -> calculateAnalytics(instructorId), analyticsExecutor);
    }

    @Transactional(readOnly = true)
    Map<String, Object> calculateAnalytics(Long instructorId) {
        List<Enrollment> all = enrollments.findAll(); long total = all.size(); long completed = all.stream().filter(e -> e.getProgress() >= 100).count();
        TreeMap<YearMonth, Long> monthly = new TreeMap<>(); YearMonth current = YearMonth.now();
        for (int offset = 5; offset >= 0; offset--) monthly.put(current.minusMonths(offset), 0L);
        all.forEach(e -> { if (e.getEnrolledAt() != null) { YearMonth month = YearMonth.from(e.getEnrolledAt()); if (monthly.containsKey(month)) monthly.compute(month, (k, v) -> v + 1); } });
        List<Map<String, Object>> monthlyView = monthly.entrySet().stream().map(e -> Map.<String, Object>of("month", e.getKey().format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)), "count", e.getValue())).toList();
        long students = instructorId == null ? users.findByRole(Role.STUDENT).size() : all.stream().filter(e -> courses.findByInstructorId(instructorId).stream().anyMatch(c -> Objects.equals(c.getId(), e.getCourseId()))).map(Enrollment::getStudentId).distinct().count();
        return Map.of("users", users.count(), "students", students, "courses", courses.count(), "enrollments", total, "submissions", submissions.count(), "completionRate", total == 0 ? 0 : Math.round(completed * 100.0 / total), "monthlyEnrollments", monthlyView);
    }

    @Transactional(readOnly = true) PlatformSettings getSettings() { return settings.findById(1L).orElseGet(() -> settings.save(new PlatformSettings())); }
    @Transactional PlatformSettings updateSettings(Map<String, Object> values) { PlatformSettings value = getSettings(); if (values.containsKey("platformName")) value.setPlatformName(text(values, "platformName", 100)); if (values.containsKey("allowSelfEnrollment")) value.setAllowSelfEnrollment(Boolean.TRUE.equals(values.get("allowSelfEnrollment"))); if (values.containsKey("maintenanceMode")) value.setMaintenanceMode(Boolean.TRUE.equals(values.get("maintenanceMode"))); if (values.containsKey("timezone")) value.setTimezone(text(values, "timezone", 80)); return settings.save(value); }

    @Transactional(readOnly = true) List<Map<String, Object>> activity() { return activities.findTop20ByOrderByCreatedAtDesc().stream().map(a -> Map.<String, Object>of("type", a.getType(), "message", a.getMessage(), "time", a.getCreatedAt().toString())).toList(); }
    @Transactional void record(String type, String message) { activities.save(new Activity(type, message)); }
    @PreDestroy void shutdown() { analyticsExecutor.shutdown(); }

    private boolean matchesPassword(User user, String raw) { String stored = user.getPassword(); if (stored.startsWith("$2")) return passwordEncoder.matches(raw, stored); if (!Objects.equals(stored, raw)) return false; user.setPassword(passwordEncoder.encode(raw)); users.save(user); return true; }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private static void requireText(String value, String name, int max) { if (value == null || value.isBlank() || value.length() > max) throw bad(name + " is required and must be at most " + max + " characters"); }
    private static void requireEmail(String value) { if (value == null || !value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw bad("A valid email is required"); }
    private static void requirePassword(String value) { if (value == null || value.length() < 8) throw bad("Password must contain at least 8 characters"); }
    private static String text(Map<String, Object> values, String key, int max) { String value = values.get(key) == null ? null : values.get(key).toString().trim(); requireText(value, key, max); return value; }
    private static String optionalText(Map<String, Object> values, String key, int max) { if (values.get(key) == null) return ""; String value = values.get(key).toString().trim(); if (value.length() > max) throw bad(key + " is too long"); return value; }
    private static Long number(Map<String, Object> values, String key) { try { if (values.get(key) == null) throw bad(key + " is required"); return Long.valueOf(values.get(key).toString()); } catch (NumberFormatException ex) { throw bad(key + " must be numeric"); } }
    private static int integer(Map<String, Object> values, String key, int min, int max) { try { int value = Integer.parseInt(values.getOrDefault(key, 100).toString()); if (value < min || value > max) throw bad(key + " must be between " + min + " and " + max); return value; } catch (NumberFormatException ex) { throw bad(key + " must be numeric"); } }
    private static Role parseRole(Object value) { try { return Role.valueOf(value.toString().toUpperCase(Locale.ROOT)); } catch (Exception ex) { throw bad("Invalid role"); } }
}
