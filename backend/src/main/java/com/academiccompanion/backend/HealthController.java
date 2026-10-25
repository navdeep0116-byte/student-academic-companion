
package com.academiccompanion.backend;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final MongoTemplate mongoTemplate;

    public HealthController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "application", "Student Academic Companion Backend"
        );
    }

    @GetMapping("/api/health/database")
    public Map<String, String> databaseHealth() {
        Map<String, String> response = new LinkedHashMap<>();

        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));
            response.put("status", "CONNECTED");
            response.put("database", "student_academic_companion");
        } catch (Exception e) {
            response.put("status", "DISCONNECTED");
            response.put("error", e.getClass().getSimpleName());
        }

        return response;
    }
}
