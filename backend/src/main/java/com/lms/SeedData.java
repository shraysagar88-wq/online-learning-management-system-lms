package com.lms;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SeedData {
    @Bean
    CommandLineRunner seed(UserRepository users, CourseRepository courses, EnrollmentRepository enrollments,
                           AssignmentRepository assignments, SettingsRepository platformSettings, ActivityRepository activities,
                           LessonRepository lessons) {
        return args -> {
            migrateBrand(users, platformSettings);
            if (users.count() > 0) {
                seedLessons(courses, lessons);
                seedActivity(activities);
                return;
            }

            User admin = users.save(new User("Aarav Mehta", "admin@gu-lms.com", "admin123", Role.ADMIN));
            User instructor = users.save(new User("Ananya Rao", "instructor@gu-lms.com", "instructor123", Role.INSTRUCTOR));
            User student = users.save(new User("Priya Sharma", "student@gu-lms.com", "student123", Role.STUDENT));
            Course spring = courses.save(new Course("Spring Boot Fundamentals",
                    "Build production-ready REST APIs with Spring Boot, JPA, and security.",
                    "REST APIs, JPA, validation, testing", "Development", "Intermediate", instructor.getId(), instructor.getName()));
            courses.save(new Course("Angular Masterclass",
                    "Learn modern Angular with standalone components, RxJS, and routing.",
                    "Components, services, RxJS, deployment", "Development", "Beginner", instructor.getId(), instructor.getName()));
            enrollments.save(new Enrollment(student.getId(), spring.getId(), spring.getTitle()));
            assignments.save(new Assignment(spring.getId(), "REST API Design", "Design endpoints for a course enrollment flow", 100));
            seedLessons(courses, lessons);
            seedActivity(activities);
        };
    }

    private void seedLessons(CourseRepository courses, LessonRepository lessons) {
        if (lessons.count() > 0) return;
        courses.findAll().forEach(course -> {
            String[][] content = course.getTitle().contains("Spring")
                    ? new String[][]{{"REST API foundations", "HTTP methods, resources, and response design"}, {"JPA persistence", "Entities, repositories, and relational data"}, {"Validation and security", "Validate input and protect API endpoints"}}
                    : new String[][]{{"Angular components", "Build standalone components and templates"}, {"RxJS services", "Use observables and services for application data"}, {"Routing and deployment", "Connect routes and prepare the application for deployment"}};
            for (int index = 0; index < content.length; index++) lessons.save(new Lesson(course.getId(), content[index][0], content[index][1], index + 1));
        });
    }

    private void seedActivity(ActivityRepository activities) {
        if (activities.count() == 0) {
            activities.save(new Activity("ENROLLMENT", "New student enrolled in Spring Boot Fundamentals"));
            activities.save(new Activity("SUBMISSION", "Priya submitted REST API Design assignment"));
            activities.save(new Activity("COURSE", "Course 'Angular Masterclass' was published"));
        }
    }

    private void migrateBrand(UserRepository users, SettingsRepository platformSettings) {
        String[][] demoEmails = {
                {"admin@learnspace.com", "admin@gu-lms.com"},
                {"instructor@learnspace.com", "instructor@gu-lms.com"},
                {"student@learnspace.com", "student@gu-lms.com"}
        };
        for (String[] emails : demoEmails) {
            users.findByEmail(emails[0]).ifPresent(user -> {
                if (users.findByEmail(emails[1]).isEmpty()) {
                    user.setEmail(emails[1]);
                    users.save(user);
                }
            });
        }
        platformSettings.findById(1L).ifPresent(settings -> {
            if ("LearnSpace LMS".equals(settings.getPlatformName())) {
                settings.setPlatformName("GU_LMS");
                platformSettings.save(settings);
            }
        });
    }
}
