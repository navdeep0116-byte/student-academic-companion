package com.academiccompanion.backend.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "assessments")
public class Assessment {

    @Id
    private String id;

    private ObjectId courseId;

    private String courseCode;

    private ObjectId classId;

    private ObjectId semesterId;

    private String title;

    private String name;

    private String type;

    private Double maxMarks;

    private Double weightage;

    /**
     * Teacher-entered class average for this specific assessment and class offering.
     * Null when the teacher has not yet submitted a class average.
     */
    private Double classAverage;

    private String status;

    private Date date;

    private Date dueDate;

    private Date createdAt;

    private String demoSeed;

    public Assessment() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getTitle() {
        return title != null ? title : name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name != null ? name : title;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(Double maxMarks) {
        this.maxMarks = maxMarks;
    }

    public Double getWeightage() {
        return weightage;
    }

    public void setWeightage(Double weightage) {
        this.weightage = weightage;
    }

    public Double getClassAverage() {
        return classAverage;
    }

    public void setClassAverage(Double classAverage) {
        this.classAverage = classAverage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getDemoSeed() {
        return demoSeed;
    }

    public void setDemoSeed(String demoSeed) {
        this.demoSeed = demoSeed;
    }
}
