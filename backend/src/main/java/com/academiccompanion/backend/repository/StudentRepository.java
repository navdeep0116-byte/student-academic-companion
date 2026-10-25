
package com.academiccompanion.backend.repository;

import com.academiccompanion.backend.model.Student;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StudentRepository extends MongoRepository<Student, String> {

    Optional<Student> findByUserId(ObjectId userId);

    boolean existsByStudentNumber(String studentNumber);
}
