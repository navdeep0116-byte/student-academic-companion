
package com.academiccompanion.backend.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "students")
public class Student {

    @Id
    private String id;

    private ObjectId userId;

    private String studentNumber;

    private String fullName;

    private Instant createdAt;

    private String program;

    private String department;

    private Integer yearOfStudy;

    private String section;

    private Double previousCgpa;

    private Instant previousCgpaUpdatedAt;

    private String previousCgpaUpdatedBy;
    private String demoSeed;

    public Student() {
    }

    public Student(ObjectId userId, String studentNumber, String fullName) {
        this.userId = userId;
        this.studentNumber = studentNumber;
        this.fullName = fullName;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ObjectId getUserId() {
        return userId;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(Integer yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Double getPreviousCgpa() {
        return previousCgpa;
    }

    public void setPreviousCgpa(Double previousCgpa) {
        this.previousCgpa = previousCgpa;
    }

    public Instant getPreviousCgpaUpdatedAt() {
        return previousCgpaUpdatedAt;
    }

    public void setPreviousCgpaUpdatedAt(Instant previousCgpaUpdatedAt) {
        this.previousCgpaUpdatedAt = previousCgpaUpdatedAt;
    }

    public String getPreviousCgpaUpdatedBy() {
        return previousCgpaUpdatedBy;
    }

    public void setPreviousCgpaUpdatedBy(String previousCgpaUpdatedBy) {
        this.previousCgpaUpdatedBy = previousCgpaUpdatedBy;
    }

    public String getDemoSeed() {
        return demoSeed;
    }

    public void setDemoSeed(String demoSeed) {
        this.demoSeed = demoSeed;
    }
}

