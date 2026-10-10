package com.lms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Small, read-only JDBC example kept separate from the JPA business repositories. */
@Service
class JdbcHealthService {
    private final JdbcTemplate jdbc;

    JdbcHealthService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    Map<String, Object> check() {
        Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("database", "UP", "jdbcCheck", Integer.valueOf(1).equals(result));
    }
}
