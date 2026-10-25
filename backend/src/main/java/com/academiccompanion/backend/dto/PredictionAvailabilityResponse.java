package com.academiccompanion.backend.dto;

import java.util.List;

public class PredictionAvailabilityResponse {

    private String studentId;
    private String studentNumber;
    private String fullName;
    private boolean mlServiceHealthy;
    private List<String> availableModels;
    private List<CourseLevelAvailabilityDto> courses;

    public PredictionAvailabilityResponse() {
    }

    public PredictionAvailabilityResponse(
            String studentId,
            String studentNumber,
            String fullName,
            boolean mlServiceHealthy,
            List<String> availableModels,
            List<CourseLevelAvailabilityDto> courses) {
        this.studentId = studentId;
        this.studentNumber = studentNumber;
        this.fullName = fullName;
        this.mlServiceHealthy = mlServiceHealthy;
        this.availableModels = availableModels;
        this.courses = courses;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isMlServiceHealthy() {
        return mlServiceHealthy;
    }

    public void setMlServiceHealthy(boolean mlServiceHealthy) {
        this.mlServiceHealthy = mlServiceHealthy;
    }

    public List<String> getAvailableModels() {
        return availableModels;
    }

    public void setAvailableModels(List<String> availableModels) {
        this.availableModels = availableModels;
    }

    public List<CourseLevelAvailabilityDto> getCourses() {
        return courses;
    }

    public void setCourses(List<CourseLevelAvailabilityDto> courses) {
        this.courses = courses;
    }
}
