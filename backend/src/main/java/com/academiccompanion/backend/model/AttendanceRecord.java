package com.academiccompanion.backend.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "attendance")
public class AttendanceRecord {

    @Id
    private String id;

    private ObjectId studentId;

    private ObjectId userId;

    private ObjectId enrollmentId;

    private ObjectId courseId;

    private String courseCode;

    private Integer classesConducted;

    private Integer classesAttended;

    private Integer classesMissed;

    private Double attendancePercentage;

    public AttendanceRecord() {
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

    public Integer getClassesConducted() {
        return classesConducted;
    }

    public void setClassesConducted(Integer classesConducted) {
        this.classesConducted = classesConducted;
    }

    public Integer getClassesAttended() {
        return classesAttended;
    }

    public void setClassesAttended(Integer classesAttended) {
        this.classesAttended = classesAttended;
    }

    public Integer getClassesMissed() {
        return classesMissed;
    }

    public void setClassesMissed(Integer classesMissed) {
        this.classesMissed = classesMissed;
    }

    public Double getAttendancePercentage() {
        if (attendancePercentage != null) {
            return attendancePercentage;
        }
        if (classesConducted != null && classesConducted > 0 && classesAttended != null) {
            return Math.round((classesAttended.doubleValue() / classesConducted.doubleValue()) * 1000.0) / 10.0;
        }
        return null;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }
}
