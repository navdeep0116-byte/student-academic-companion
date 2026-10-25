package com.academiccompanion.backend.dto;

import java.util.List;
import java.util.Map;

public class CoursePredictionDto {

    private String courseId;
    private String courseCode;
    private String courseName;
    private String predictionStatus; // AVAILABLE, INSUFFICIENT_DATA, SERVICE_UNAVAILABLE
    private String level;            // level1, level2, level3, or null
    private String stageDescription;
    private Double predictedOverallScore;
    private String likelyGrade;
    private String riskLevel;
    private List<Double> illustrativeRange;
    private int completedAssessmentsCount;
    private Map<String, Double> normalizedInputMarks;
    private String statusMessage;
    private String warning;

    public CoursePredictionDto() {
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getPredictionStatus() {
        return predictionStatus;
    }

    public void setPredictionStatus(String predictionStatus) {
        this.predictionStatus = predictionStatus;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getStageDescription() {
        return stageDescription;
    }

    public void setStageDescription(String stageDescription) {
        this.stageDescription = stageDescription;
    }

    public Double getPredictedOverallScore() {
        return predictedOverallScore;
    }

    public void setPredictedOverallScore(Double predictedOverallScore) {
        this.predictedOverallScore = predictedOverallScore;
    }

    public String getLikelyGrade() {
        return likelyGrade;
    }

    public void setLikelyGrade(String likelyGrade) {
        this.likelyGrade = likelyGrade;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<Double> getIllustrativeRange() {
        return illustrativeRange;
    }

    public void setIllustrativeRange(List<Double> illustrativeRange) {
        this.illustrativeRange = illustrativeRange;
    }

    public int getCompletedAssessmentsCount() {
        return completedAssessmentsCount;
    }

    public void setCompletedAssessmentsCount(int completedAssessmentsCount) {
        this.completedAssessmentsCount = completedAssessmentsCount;
    }

    public Map<String, Double> getNormalizedInputMarks() {
        return normalizedInputMarks;
    }

    public void setNormalizedInputMarks(Map<String, Double> normalizedInputMarks) {
        this.normalizedInputMarks = normalizedInputMarks;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
