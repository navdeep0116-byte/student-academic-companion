package com.academiccompanion.backend.repository;

import com.academiccompanion.backend.model.AttendanceRecord;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends MongoRepository<AttendanceRecord, String> {

    List<AttendanceRecord> findByStudentId(ObjectId studentId);

    List<AttendanceRecord> findByUserId(ObjectId userId);

    List<AttendanceRecord> findByEnrollmentId(ObjectId enrollmentId);

    Optional<AttendanceRecord> findByStudentIdAndCourseId(ObjectId studentId, ObjectId courseId);
}
