package com.lms;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
class Activity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    String type; String message; LocalDateTime createdAt = LocalDateTime.now();
    public Activity() {}
    Activity(String type, String message) { this.type = type; this.message = message; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
