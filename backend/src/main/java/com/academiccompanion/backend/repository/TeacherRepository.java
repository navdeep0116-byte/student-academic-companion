package com.academiccompanion.backend.repository;

import com.academiccompanion.backend.model.Teacher;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface TeacherRepository extends MongoRepository<Teacher, String> {

    Optional<Teacher> findByUserId(ObjectId userId);

    Optional<Teacher> findByEmployeeNumber(String employeeNumber);
}
