package com.lms;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"},
    allowedHeaders = {"Authorization", "Content-Type", "Accept"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS},
    allowCredentials = "true")
public class ApiController {
    private final LmsService lms;
    private final TokenService tokens;
    private final JdbcHealthService jdbcHealth;
    ApiController(LmsService lms, TokenService tokens, JdbcHealthService jdbcHealth) { this.lms = lms; this.tokens = tokens; this.jdbcHealth = jdbcHealth; }

    @PublicEndpoint @PostMapping("/auth/login")
    Map<String, Object> login(@RequestBody Map<String, String> body, HttpSession session) {
        String email = body.getOrDefault("email", "").trim().toLowerCase(Locale.ROOT);
        email = switch (email) { case "admin@learnspace.com" -> "admin@gu-lms.com"; case "instructor@learnspace.com" -> "instructor@gu-lms.com"; case "student@learnspace.com" -> "student@gu-lms.com"; default -> email; };
        User user = lms.authenticate(email, body.get("password"));
        session.setAttribute("authenticatedUserId", user.getId());
        return authResponse(user);
    }

    @PublicEndpoint @PostMapping("/auth/register")
    ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body, HttpSession session) {
        User user = lms.register(body.getOrDefault("name", "").trim(), body.getOrDefault("email", "").trim().toLowerCase(Locale.ROOT), body.get("password"));
        session.setAttribute("authenticatedUserId", user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse(user));
    }

    @PublicEndpoint @PostMapping("/auth/logout")
    Map<String, String> logout(HttpSession session) {
        session.invalidate();
        return Map.of("message", "Logged out successfully");
    }

    @PublicEndpoint @GetMapping("/health/database")
    Map<String, Object> databaseHealth() { return jdbcHealth.check(); }

    @RequireRole(Role.ADMIN) @GetMapping("/users") List<Map<String, Object>> allUsers() { return lms.allUsers().stream().map(this::userView).toList(); }
    @RequireRole(Role.ADMIN) @PostMapping("/users") Map<String, Object> createUser(@RequestBody Map<String, Object> body) { return userView(lms.createUser((String) body.get("name"), String.valueOf(body.getOrDefault("email", "")).toLowerCase(Locale.ROOT), String.valueOf(body.getOrDefault("password", "")), role(body.getOrDefault("role", "STUDENT")))); }
    @RequireRole(Role.ADMIN) @PutMapping("/users/{id}") Map<String, Object> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) { return userView(lms.updateUser(id, body)); }
    @RequireRole(Role.ADMIN) @DeleteMapping("/users/{id}") Map<String, String> deleteUser(@PathVariable Long id) { lms.deleteUser(id); return Map.of("message", "User deleted successfully"); }

    @GetMapping("/courses") List<Course> allCourses() { return lms.allCourses(); }
    @GetMapping("/courses/{id}") Course course(@PathVariable Long id) { return lms.course(id); }
    @RequireRole({Role.ADMIN, Role.INSTRUCTOR}) @PostMapping("/courses") Course createCourse(@RequestBody Map<String, Object> body) { return lms.createCourse(body); }
    @RequireRole({Role.ADMIN, Role.INSTRUCTOR}) @PutMapping("/courses/{id}") Course updateCourse(@PathVariable Long id, @RequestBody Map<String, Object> body) { return lms.updateCourse(id, body); }
    @RequireRole(Role.ADMIN) @DeleteMapping("/courses/{id}") Map<String, String> deleteCourse(@PathVariable Long id) { lms.deleteCourse(id); return Map.of("message", "Course deleted successfully"); }

    @RequireRole(Role.STUDENT) @PostMapping("/enrollments") Enrollment enroll(@RequestBody Map<String, Object> body) { return lms.enroll(number(body, "studentId"), number(body, "courseId")); }
    @RequireRole(Role.STUDENT) @GetMapping("/students/{studentId}/enrollments") List<Enrollment> studentEnrollments(@PathVariable Long studentId) { return lms.studentEnrollments(studentId); }
    @RequireRole(Role.STUDENT) @PatchMapping("/enrollments/{id}/progress") Enrollment progress(@PathVariable Long id, @RequestBody Map<String, Integer> body) { throw new org.springframework.web.server.ResponseStatusException(HttpStatus.GONE, "Progress is calculated from completed lessons"); }
    @RequireRole({Role.ADMIN, Role.INSTRUCTOR}) @GetMapping("/courses/{courseId}/enrollments/count") Map<String, Object> enrollmentCount(@PathVariable Long courseId) { return Map.of("courseId", courseId, "count", lms.enrollmentCount(courseId)); }
    @RequireRole(Role.STUDENT) @GetMapping("/students/{studentId}/courses/{courseId}/lessons") List<Map<String, Object>> learningLessons(@PathVariable Long studentId, @PathVariable Long courseId, @RequestAttribute("authenticatedUser") User authenticated) { requireSelf(studentId, authenticated); return lms.learningLessons(studentId, courseId); }
    @RequireRole(Role.STUDENT) @PostMapping("/lessons/{lessonId}/complete") Map<String, Object> completeLesson(@PathVariable Long lessonId, @RequestAttribute("authenticatedUser") User authenticated) { return lms.completeLesson(authenticated.getId(), lessonId); }

    @GetMapping("/courses/{courseId}/assignments") List<Assignment> courseAssignments(@PathVariable Long courseId) { return lms.courseAssignments(courseId); }
    @RequireRole(Role.INSTRUCTOR) @PostMapping("/assignments") Assignment createAssignment(@RequestBody Map<String, Object> body) { return lms.createAssignment(body); }
    @RequireRole(Role.STUDENT) @PostMapping("/submissions") Submission submit(@RequestBody Map<String, Object> body) { return lms.submit(body); }
    @RequireRole(Role.INSTRUCTOR) @GetMapping("/assignments/{assignmentId}/submissions") List<Submission> assignmentSubmissions(@PathVariable Long assignmentId) { return lms.assignmentSubmissions(assignmentId); }
    @RequireRole(Role.INSTRUCTOR) @PatchMapping("/submissions/{id}/grade") Submission grade(@PathVariable Long id, @RequestBody Map<String, Object> body) { return lms.grade(id, body); }

    @RequireRole({Role.ADMIN, Role.INSTRUCTOR}) @GetMapping("/analytics/overview") Map<String, Object> analytics(@RequestParam(required = false) Long instructorId) { return lms.analytics(instructorId).join(); }
    @RequireRole(Role.ADMIN) @GetMapping("/settings") PlatformSettings settings() { return lms.getSettings(); }
    @RequireRole(Role.ADMIN) @PutMapping("/settings") PlatformSettings updateSettings(@RequestBody Map<String, Object> values) { return lms.updateSettings(values); }
    @GetMapping("/activity") List<Map<String, Object>> activity() { return lms.activity(); }

    private Map<String, Object> authResponse(User user) { return Map.of("token", tokens.issue(user), "user", userView(user)); }
    private Map<String, Object> userView(User user) { return Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(), "role", user.getRole().name(), "active", user.isActive()); }
    private static Long number(Map<String, Object> values, String key) { try { if (values.get(key) == null) throw new IllegalArgumentException(); return Long.valueOf(values.get(key).toString()); } catch (Exception ex) { throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, key + " is required and numeric"); } }
    private static Role role(Object value) { try { return Role.valueOf(value.toString().toUpperCase(Locale.ROOT)); } catch (Exception ex) { throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role"); } }
    private static void requireSelf(Long requestedId, User authenticated) { if (!Objects.equals(requestedId, authenticated.getId())) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own learning progress"); }
}
