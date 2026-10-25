package com.academiccompanion.backend.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "results")
public class AssessmentResult {

    @Id
    private String id;

    private ObjectId studentId;

    private ObjectId userId;

    private ObjectId enrollmentId;

    private ObjectId courseId;

    private String courseCode;

    private ObjectId assessmentId;

    private String assessmentName;

    private String assessmentType;

    private Double obtainedMarks;

    private Double marksObtained;

    private Double maxMarks;

    private String status;

    private String grade;

    private ObjectId semesterId;

    public AssessmentResult() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ObjectId getStudentId() {
        return studentId;
    }

    public void setStudentId(ObjectId studentId) {
        this.studentId = studentId;
    }

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public ObjectId getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(ObjectId enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public ObjectId getCourseId() {
        return courseId;
    }

    public void setCourseId(ObjectId courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public ObjectId getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(ObjectId assessmentId) {
        this.assessmentId = assessmentId;
    }

    public String getAssessmentName() {
        return assessmentName;
    }

    public void setAssessmentName(String assessmentName) {
        this.assessmentName = assessmentName;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(String assessmentType) {
        this.assessmentType = assessmentType;
    }

    public Double getObtainedMarks() {
        return obtainedMarks != null ? obtainedMarks : marksObtained;
    }

    public void setObtainedMarks(Double obtainedMarks) {
        this.obtainedMarks = obtainedMarks;
    }

    public Double getMarksObtained() {
        return marksObtained != null ? marksObtained : obtainedMarks;
    }

    public void setMarksObtained(Double marksObtained) {
        this.marksObtained = marksObtained;
    }

    public Double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(Double maxMarks) {
        this.maxMarks = maxMarks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public ObjectId getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(ObjectId semesterId) {
        this.semesterId = semesterId;
    }
}
