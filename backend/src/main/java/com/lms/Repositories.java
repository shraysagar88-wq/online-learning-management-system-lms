package com.lms;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface UserRepository extends JpaRepository<User,Long>{ Optional<User> findByEmail(String email); List<User> findByRole(Role role); }
interface CourseRepository extends JpaRepository<Course,Long>{ List<Course> findByInstructorId(Long instructorId); }
interface EnrollmentRepository extends JpaRepository<Enrollment,Long>{ List<Enrollment> findByStudentId(Long studentId); long countByCourseId(Long courseId); Optional<Enrollment> findByStudentIdAndCourseId(Long s,Long c); }
interface AssignmentRepository extends JpaRepository<Assignment,Long>{ List<Assignment> findByCourseId(Long courseId); }
interface SubmissionRepository extends JpaRepository<Submission,Long>{ List<Submission> findByAssignmentId(Long assignmentId); }
interface SettingsRepository extends JpaRepository<PlatformSettings,Long>{}
interface ActivityRepository extends JpaRepository<Activity,Long>{ List<Activity> findTop20ByOrderByCreatedAtDesc(); }
interface LessonRepository extends JpaRepository<Lesson,Long>{ List<Lesson> findByCourseIdOrderByLessonOrder(Long courseId); }
interface LessonCompletionRepository extends JpaRepository<LessonCompletion,Long>{ Optional<LessonCompletion> findByStudentIdAndLessonId(Long studentId, Long lessonId); long countByStudentIdAndLessonIdIn(Long studentId, Collection<Long> lessonIds); }
