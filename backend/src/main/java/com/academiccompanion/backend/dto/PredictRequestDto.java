package com.academiccompanion.backend.dto;

import java.util.Map;

public class PredictRequestDto {

    private String courseCode;
    private String level;
    private Map<String, Double> whatIfMarks;

    public PredictRequestDto() {
    }

    public PredictRequestDto(String courseCode, String level, Map<String, Double> whatIfMarks) {
        this.courseCode = courseCode;
        this.level = level;
        this.whatIfMarks = whatIfMarks;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Map<String, Double> getWhatIfMarks() {
        return whatIfMarks;
    }

    public void setWhatIfMarks(Map<String, Double> whatIfMarks) {
        this.whatIfMarks = whatIfMarks;
    }
}
