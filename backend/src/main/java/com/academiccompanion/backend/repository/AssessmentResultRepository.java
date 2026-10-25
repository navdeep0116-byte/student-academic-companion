package com.academiccompanion.backend.repository;

import com.academiccompanion.backend.model.AssessmentResult;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssessmentResultRepository extends MongoRepository<AssessmentResult, String> {

    List<AssessmentResult> findByStudentId(ObjectId studentId);

    List<AssessmentResult> findByUserId(ObjectId userId);

    List<AssessmentResult> findByEnrollmentId(ObjectId enrollmentId);

    List<AssessmentResult> findByStudentIdAndCourseId(ObjectId studentId, ObjectId courseId);
}
