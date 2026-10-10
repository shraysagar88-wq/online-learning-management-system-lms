package com.lms;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_lessons", uniqueConstraints = @UniqueConstraint(columnNames = {"courseId", "lessonOrder"}))
class Lesson {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    Long courseId;
    String title;
    @Column(length = 4000) String description;
    int lessonOrder;

    public Lesson() {}
    Lesson(Long courseId, String title, String description, int lessonOrder) { this.courseId = courseId; this.title = title; this.description = description; this.lessonOrder = lessonOrder; }
    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getLessonOrder() { return lessonOrder; }
}

@Entity
@Table(name = "lesson_completions", uniqueConstraints = @UniqueConstraint(columnNames = {"studentId", "lessonId"}))
class LessonCompletion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    Long studentId;
    Long lessonId;
    LocalDateTime completedAt = LocalDateTime.now();

    public LessonCompletion() {}
    LessonCompletion(Long studentId, Long lessonId) { this.studentId = studentId; this.lessonId = lessonId; }
}
