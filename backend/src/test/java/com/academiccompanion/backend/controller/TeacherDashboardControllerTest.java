package com.academiccompanion.backend.controller;

import com.academiccompanion.backend.model.Assessment;
import com.academiccompanion.backend.model.Teacher;
import com.academiccompanion.backend.model.User;
import com.academiccompanion.backend.repository.AssessmentRepository;
import com.academiccompanion.backend.repository.TeacherRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TeacherDashboardControllerTest {

    private TeacherRepository teacherRepository;
    private AssessmentRepository assessmentRepository;
    private MongoTemplate mongoTemplate;
    private TeacherDashboardController controller;

    @BeforeEach
    void setUp() {
        teacherRepository = Mockito.mock(TeacherRepository.class);
        assessmentRepository = Mockito.mock(AssessmentRepository.class);
        mongoTemplate = Mockito.mock(MongoTemplate.class);

        controller = new TeacherDashboardController(
                teacherRepository,
                assessmentRepository,
                mongoTemplate
        );
    }

    private User createTeacherUser(ObjectId userId) {
        User user = new User("Dr. Teacher", "teacher@example.com", "hash", User.Role.TEACHER);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, userId.toHexString());
        } catch (Exception ignored) {
        }
        return user;
    }

    @Test
    void testGetAssignedClasses_UnauthorizedWhenNoAuth() {
        ResponseEntity<?> resp = controller.getAssignedClasses(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void testGetAssignedClasses_ForbiddenForStudent() {
        User student = new User("Student", "student@example.com", "hash", User.Role.STUDENT);
        Authentication auth = new UsernamePasswordAuthenticationToken(student, null);
        ResponseEntity<?> resp = controller.getAssignedClasses(auth);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void testGetAssignedClasses_Success() {
        ObjectId userId = new ObjectId();
        User teacherUser = createTeacherUser(userId);
        Authentication auth = new UsernamePasswordAuthenticationToken(teacherUser, null);

        Teacher teacher = new Teacher();
        teacher.setId(new ObjectId().toHexString());
        teacher.setUserId(userId);
        when(teacherRepository.findByUserId(userId)).thenReturn(Optional.of(teacher));

        ObjectId classId = new ObjectId();
        Document classDoc = new Document("_id", classId)
                .append("name", "BCSE203-A")
                .append("courseCode", "BCSE203")
                .append("courseName", "Database Systems")
                .append("section", "A");
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq("classes")))
                .thenReturn(List.of(classDoc));

        Assessment a = new Assessment();
        a.setId(new ObjectId().toHexString());
        a.setName("CAT 1");
        a.setType("CAT");
        a.setMaxMarks(20.0);
        a.setClassAverage(38.0);
        when(assessmentRepository.findByClassId(classId)).thenReturn(List.of(a));

        ResponseEntity<?> resp = controller.getAssignedClasses(auth);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody() instanceof Map<?, ?>);

        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        List<?> classes = (List<?>) body.get("classes");
        assertEquals(1, classes.size());
        Map<?, ?> c = (Map<?, ?>) classes.get(0);
        assertEquals("BCSE203-A", c.get("className"));

        List<?> assessments = (List<?>) c.get("assessments");
        assertEquals(1, assessments.size());
        Map<?, ?> aMap = (Map<?, ?>) assessments.get(0);
        assertEquals(50.0, aMap.get("rawMaxMarks"));
        assertEquals(38.0, aMap.get("classAverage"));
    }

    @Test
    void testUpdateClassAverage_Success() {
        ObjectId userId = new ObjectId();
        User teacherUser = createTeacherUser(userId);
        Authentication auth = new UsernamePasswordAuthenticationToken(teacherUser, null);

        ObjectId teacherId = new ObjectId();
        Teacher teacher = new Teacher();
        teacher.setId(teacherId.toHexString());
        teacher.setUserId(userId);
        when(teacherRepository.findByUserId(userId)).thenReturn(Optional.of(teacher));

        ObjectId classId = new ObjectId();
        Assessment a = new Assessment();
        String aId = new ObjectId().toHexString();
        a.setId(aId);
        a.setName("CAT 1");
        a.setType("CAT");
        a.setMaxMarks(20.0);
        a.setClassId(classId);
        when(assessmentRepository.findById(aId)).thenReturn(Optional.of(a));

        when(mongoTemplate.exists(any(Query.class), eq("classes"))).thenReturn(true);

        Map<String, Object> payload = Map.of("classAverage", 37.5);
        ResponseEntity<?> resp = controller.updateClassAverage(aId, payload, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(37.5, a.getClassAverage());
    }

    @Test
    void testUpdateClassAverage_RejectsExceedingMaxMarks() {
        ObjectId userId = new ObjectId();
        User teacherUser = createTeacherUser(userId);
        Authentication auth = new UsernamePasswordAuthenticationToken(teacherUser, null);

        ObjectId teacherId = new ObjectId();
        Teacher teacher = new Teacher();
        teacher.setId(teacherId.toHexString());
        teacher.setUserId(userId);
        when(teacherRepository.findByUserId(userId)).thenReturn(Optional.of(teacher));

        ObjectId classId = new ObjectId();
        Assessment a = new Assessment();
        String aId = new ObjectId().toHexString();
        a.setId(aId);
        a.setName("CAT 1");
        a.setType("CAT");
        a.setMaxMarks(20.0); // rawMaxScale is 50.0
        a.setClassId(classId);
        when(assessmentRepository.findById(aId)).thenReturn(Optional.of(a));

        when(mongoTemplate.exists(any(Query.class), eq("classes"))).thenReturn(true);

        // Attempting to set class average to 55.0 (> 50.0)
        Map<String, Object> payload = Map.of("classAverage", 55.0);
        ResponseEntity<?> resp = controller.updateClassAverage(aId, payload, auth);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void testUpdateClassAverage_ForbiddenWhenNotAssignedTeacher() {
        ObjectId userId = new ObjectId();
        User teacherUser = createTeacherUser(userId);
        Authentication auth = new UsernamePasswordAuthenticationToken(teacherUser, null);

        ObjectId teacherId = new ObjectId();
        Teacher teacher = new Teacher();
        teacher.setId(teacherId.toHexString());
        teacher.setUserId(userId);
        when(teacherRepository.findByUserId(userId)).thenReturn(Optional.of(teacher));

        ObjectId classId = new ObjectId();
        Assessment a = new Assessment();
        String aId = new ObjectId().toHexString();
        a.setId(aId);
        a.setName("CAT 1");
        a.setType("CAT");
        a.setClassId(classId);
        when(assessmentRepository.findById(aId)).thenReturn(Optional.of(a));

        // Teacher is not assigned to this class
        when(mongoTemplate.exists(any(Query.class), eq("classes"))).thenReturn(false);
        when(mongoTemplate.exists(any(Query.class), eq("teaching_assignments"))).thenReturn(false);

        Map<String, Object> payload = Map.of("classAverage", 35.0);
        ResponseEntity<?> resp = controller.updateClassAverage(aId, payload, auth);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }
}
