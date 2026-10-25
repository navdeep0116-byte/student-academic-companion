package com.academiccompanion.backend.repository;

import com.academiccompanion.backend.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CourseRepository extends MongoRepository<Course, String> {

    Optional<Course> findByCode(String code);
}
