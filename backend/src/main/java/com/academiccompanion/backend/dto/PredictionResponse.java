package com.academiccompanion.backend.dto;

import java.util.List;

public class PredictionResponse {

    private String studentId;
    private String studentNumber;
    private String fullName;
    private boolean mlServiceHealthy;
    private int totalCourses;
    private int predictableCourses;
    private List<CoursePredictionDto> predictions;
    private String disclaimer;

    public PredictionResponse() {
    }

    public PredictionResponse(
            String studentId,
            String studentNumber,
            String fullName,
            boolean mlServiceHealthy,
            int totalCourses,
            int predictableCourses,
            List<CoursePredictionDto> predictions,
            String disclaimer) {
        this.studentId = studentId;
        this.studentNumber = studentNumber;
        this.fullName = fullName;
        this.mlServiceHealthy = mlServiceHealthy;
        this.totalCourses = totalCourses;
        this.predictableCourses = predictableCourses;
        this.predictions = predictions;
        this.disclaimer = disclaimer;
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

    public int getTotalCourses() {
        return totalCourses;
    }

    public void setTotalCourses(int totalCourses) {
        this.totalCourses = totalCourses;
    }

    public int getPredictableCourses() {
        return predictableCourses;
    }

    public void setPredictableCourses(int predictableCourses) {
        this.predictableCourses = predictableCourses;
    }

    public List<CoursePredictionDto> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<CoursePredictionDto> predictions) {
        this.predictions = predictions;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
