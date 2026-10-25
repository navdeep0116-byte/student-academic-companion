package com.academiccompanion.backend.dto;

import java.util.List;
import java.util.Map;

public class CourseLevelAvailabilityDto {

    private String courseId;
    private String courseCode;
    private String courseName;
    private String modelCourseCode;
    private int completedAssessmentsCount;
    private Map<String, Double> recordedMarks;
    private Map<String, LevelEligibilityDto> levels;
    private List<String> eligibleLevels;

    public CourseLevelAvailabilityDto() {
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

    public String getModelCourseCode() {
        return modelCourseCode;
    }

    public void setModelCourseCode(String modelCourseCode) {
        this.modelCourseCode = modelCourseCode;
    }

    public int getCompletedAssessmentsCount() {
        return completedAssessmentsCount;
    }

    public void setCompletedAssessmentsCount(int completedAssessmentsCount) {
        this.completedAssessmentsCount = completedAssessmentsCount;
    }

    public Map<String, Double> getRecordedMarks() {
        return recordedMarks;
    }

    public void setRecordedMarks(Map<String, Double> recordedMarks) {
        this.recordedMarks = recordedMarks;
    }

    public Map<String, LevelEligibilityDto> getLevels() {
        return levels;
    }

    public void setLevels(Map<String, LevelEligibilityDto> levels) {
        this.levels = levels;
    }

    public List<String> getEligibleLevels() {
        return eligibleLevels;
    }

    public void setEligibleLevels(List<String> eligibleLevels) {
        this.eligibleLevels = eligibleLevels;
    }
}
