package com.academiccompanion.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final StudentDashboardController studentDashboardController;

    public PredictionController(StudentDashboardController studentDashboardController) {
        this.studentDashboardController = studentDashboardController;
    }

    @GetMapping("/availability")
    public ResponseEntity<?> availability(Authentication authentication) {
        return studentDashboardController.predictionAvailability(authentication);
    }

    @GetMapping
    public ResponseEntity<?> prediction(
            Authentication authentication,
            @org.springframework.web.bind.annotation.RequestParam(value = "courseCode", required = false) String courseCode,
            @org.springframework.web.bind.annotation.RequestParam(value = "level", required = false) String level) {
        return studentDashboardController.prediction(authentication, courseCode, level);
    }
}
