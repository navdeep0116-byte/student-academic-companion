package com.academiccompanion.backend;

import com.academiccompanion.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BackendApplicationTests {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private CourseRepository courseRepository;

    @MockitoBean
    private EnrollmentRepository enrollmentRepository;

    @MockitoBean
    private AssessmentResultRepository resultRepository;

    @MockitoBean
    private AttendanceRecordRepository attendanceRepository;

    @MockitoBean
    private AssessmentRepository assessmentRepository;

    @MockitoBean
    private TeacherRepository teacherRepository;

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private org.springframework.data.mongodb.core.convert.MongoConverter mongoConverter;

    @MockitoBean
    private org.springframework.data.mongodb.gridfs.GridFsTemplate gridFsTemplate;

    @Test
    void contextLoads() {
    }

}
