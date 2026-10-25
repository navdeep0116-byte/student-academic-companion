package com.academiccompanion.backend.repository;

import com.academiccompanion.backend.model.Assessment;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface AssessmentRepository extends MongoRepository<Assessment, String> {

    List<Assessment> findByClassId(ObjectId classId);

    List<Assessment> findByCourseId(ObjectId courseId);

    List<Assessment> findByCourseCode(String courseCode);
}
