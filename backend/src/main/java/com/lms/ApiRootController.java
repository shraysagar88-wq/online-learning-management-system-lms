package com.lms;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiRootController {
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "application", "GU_LMS API",
                "status", "UP",
                "api", "/api",
                "frontend", "http://localhost:4200");
    }
}