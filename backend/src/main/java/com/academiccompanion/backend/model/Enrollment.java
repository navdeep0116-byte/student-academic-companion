package com.academiccompanion.backend.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "enrollments")
public class Enrollment {

    @Id
    private String id;

    private ObjectId studentId;

    private ObjectId userId;

    private ObjectId courseId;

    private String courseCode;

    private ObjectId classId;

    private ObjectId semesterId;

    private String status;

    private Instant enrolledAt;

    public Enrollment() {
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

    public ObjectId getClassId() {
        return classId;
    }

    public void setClassId(ObjectId classId) {
        this.classId = classId;
    }

    public ObjectId getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(ObjectId semesterId) {
        this.semesterId = semesterId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(Instant enrolledAt) {
        this.enrolledAt = enrolledAt;
    }
}
