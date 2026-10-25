package com.academiccompanion.backend.repository;

import com.academiccompanion.backend.model.Enrollment;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {

    List<Enrollment> findByStudentId(ObjectId studentId);

    List<Enrollment> findByUserId(ObjectId userId);
}
