package com.academiccompanion.backend.controller;

import com.academiccompanion.backend.dto.CoursePredictionDto;
import com.academiccompanion.backend.dto.PredictionResponse;
import com.academiccompanion.backend.model.*;
import com.academiccompanion.backend.repository.*;
import com.academiccompanion.backend.service.MlPredictionService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StudentDashboardControllerTest {

    private StudentRepository studentRepository;
    private EnrollmentRepository enrollmentRepository;
    private CourseRepository courseRepository;
    private AssessmentResultRepository resultRepository;
    private AttendanceRecordRepository attendanceRepository;
    private AssessmentRepository assessmentRepository;
    private MlPredictionService mlPredictionService;
    private StudentDashboardController controller;

    @BeforeEach
    void setUp() {
        studentRepository = Mockito.mock(StudentRepository.class);
        enrollmentRepository = Mockito.mock(EnrollmentRepository.class);
        courseRepository = Mockito.mock(CourseRepository.class);
        resultRepository = Mockito.mock(AssessmentResultRepository.class);
        attendanceRepository = Mockito.mock(AttendanceRecordRepository.class);
        assessmentRepository = Mockito.mock(AssessmentRepository.class);
        mlPredictionService = Mockito.mock(MlPredictionService.class);

        controller = new StudentDashboardController(
                studentRepository,
                enrollmentRepository,
                courseRepository,
                resultRepository,
                attendanceRepository,
                assessmentRepository,
                mlPredictionService
        );
    }

    @Test
    void testPrediction_UnauthorizedWhenNoAuth() {
        ResponseEntity<?> resp = controller.prediction(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void testPrediction_UnauthorizedWhenWrongRole() {
        User teacher = new User("Dr. Teacher", "teacher@example.com", "hash", User.Role.TEACHER);
        Authentication auth = new UsernamePasswordAuthenticationToken(teacher, null);
        ResponseEntity<?> resp = controller.prediction(auth);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void testPrediction_SuccessWithAuthenticatedStudent() {
        ObjectId userId = new ObjectId();
        User studentUser = new User("Demo Student", "student@example.com", "hash", User.Role.STUDENT);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(studentUser, userId.toHexString());
        } catch (Exception ignored) {
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(studentUser, null);

        Student student = new Student(userId, "SAC12345", "Demo Student");
        try {
            var field = Student.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(student, new ObjectId().toHexString());
        } catch (Exception ignored) {
        }

        when(studentRepository.findByUserId(userId)).thenReturn(Optional.of(student));

        Enrollment enrollment = new Enrollment();
        ObjectId courseId = new ObjectId();
        enrollment.setCourseId(courseId);
        enrollment.setCourseCode("BCSE203");
        when(enrollmentRepository.findByStudentId(any())).thenReturn(List.of(enrollment));

        Course course = new Course();
        course.setCode("BCSE203");
        course.setName("Database Systems");
        when(courseRepository.findById(courseId.toHexString())).thenReturn(Optional.of(course));

        CoursePredictionDto predictionDto = new CoursePredictionDto();
        predictionDto.setCourseCode("BCSE203");
        predictionDto.setPredictedOverallScore(82.5);
        predictionDto.setPredictionStatus("AVAILABLE");
        predictionDto.setLevel("level1");

        when(mlPredictionService.isHealthy()).thenReturn(true);
        when(mlPredictionService.predictForCourse(any(), any(), any(), any())).thenReturn(predictionDto);

        ResponseEntity<?> response = controller.prediction(auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof PredictionResponse);

        PredictionResponse body = (PredictionResponse) response.getBody();
        assertEquals(1, body.getTotalCourses());
        assertEquals(1, body.getPredictableCourses());
        assertTrue(body.isMlServiceHealthy());
        assertEquals("SAC12345", body.getStudentNumber());
    }

    @Test
    void testMarks_UnauthorizedWhenNoAuth() {
        ResponseEntity<?> resp = controller.marks(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void testMarks_SuccessWithAuthenticatedStudent() {
        ObjectId userId = new ObjectId();
        User studentUser = new User("Demo Student", "student@example.com", "hash", User.Role.STUDENT);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(studentUser, userId.toHexString());
        } catch (Exception ignored) {
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(studentUser, null);
        Student student = new Student(userId, "SAC12345", "Demo Student");
        student.setId(new ObjectId().toHexString());
        when(studentRepository.findByUserId(userId)).thenReturn(Optional.of(student));

        Enrollment enrollment = new Enrollment();
        ObjectId courseId = new ObjectId();
        enrollment.setCourseId(courseId);
        enrollment.setCourseCode("BCSE203");
        when(enrollmentRepository.findByStudentId(any())).thenReturn(List.of(enrollment));

        Course course = new Course();
        course.setCode("BCSE203");
        course.setName("Database Systems");
        course.setCredits(4.0);
        course.setEvaluationType("THEORY");
        when(courseRepository.findById(courseId.toHexString())).thenReturn(Optional.of(course));

        ObjectId aId = new ObjectId();
        Assessment assess = new Assessment();
        assess.setId(aId.toHexString());
        assess.setClassAverage(36.5);
        when(assessmentRepository.findAllById(any())).thenReturn(List.of(assess));

        AssessmentResult result = new AssessmentResult();
        result.setCourseCode("BCSE203");
        result.setAssessmentId(aId);
        result.setAssessmentName("CAT 1");
        result.setAssessmentType("CAT");
        result.setObtainedMarks(18.0);
        result.setMaxMarks(20.0);
        result.setStatus("COMPLETED");
        when(resultRepository.findByStudentId(any())).thenReturn(List.of(result));

        ResponseEntity<?> response = controller.marks(auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map<?, ?>);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.containsKey("courses"));
        List<?> courses = (List<?>) body.get("courses");
        assertEquals(1, courses.size());
        Map<?, ?> firstCourse = (Map<?, ?>) courses.get(0);
        assertEquals("BCSE203", firstCourse.get("courseCode"));
        assertEquals(4.0, firstCourse.get("credits"));

        List<?> assessments = (List<?>) firstCourse.get("assessments");
        assertEquals(1, assessments.size());
        Map<?, ?> firstAssessment = (Map<?, ?>) assessments.get(0);
        assertEquals(45.0, firstAssessment.get("rawMarks"));
        assertEquals(50.0, firstAssessment.get("rawMaxMarks"));
        assertEquals(15.0, firstAssessment.get("weightage"));
        assertEquals(36.5, firstAssessment.get("classAverage"));
    }

    @Test
    void testAttendance_SuccessWithAuthenticatedStudent() {
        ObjectId userId = new ObjectId();
        User studentUser = new User("Demo Student", "student@example.com", "hash", User.Role.STUDENT);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(studentUser, userId.toHexString());
        } catch (Exception ignored) {
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(studentUser, null);
        Student student = new Student(userId, "SAC12345", "Demo Student");
        student.setId(new ObjectId().toHexString());
        when(studentRepository.findByUserId(userId)).thenReturn(Optional.of(student));

        Enrollment enrollment = new Enrollment();
        ObjectId courseId = new ObjectId();
        enrollment.setCourseId(courseId);
        enrollment.setCourseCode("BCSE203");
        when(enrollmentRepository.findByStudentId(any())).thenReturn(List.of(enrollment));

        Course course = new Course();
        course.setCode("BCSE203");
        course.setName("Database Systems");
        course.setCredits(4.0);
        when(courseRepository.findById(courseId.toHexString())).thenReturn(Optional.of(course));

        AttendanceRecord attendance = new AttendanceRecord();
        attendance.setCourseCode("BCSE203");
        attendance.setClassesConducted(40);
        attendance.setClassesAttended(36);
        attendance.setClassesMissed(4);
        attendance.setAttendancePercentage(90.0);
        when(attendanceRepository.findByStudentId(any())).thenReturn(List.of(attendance));

        ResponseEntity<?> response = controller.attendance(auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map<?, ?>);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.containsKey("courses"));
        List<?> courses = (List<?>) body.get("courses");
        assertEquals(1, courses.size());
        Map<?, ?> firstCourse = (Map<?, ?>) courses.get(0);
        assertEquals("BCSE203", firstCourse.get("courseCode"));
        assertEquals(90.0, firstCourse.get("attendancePercentage"));
    }
}
