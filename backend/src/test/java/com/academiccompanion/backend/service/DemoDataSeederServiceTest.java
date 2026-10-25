package com.academiccompanion.backend.service;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DemoDataSeederServiceTest {

    private MongoTemplate mongoTemplate;
    private PasswordEncoder passwordEncoder;
    private DemoDataSeederService seederService;

    @BeforeEach
    void setUp() {
        mongoTemplate = Mockito.mock(MongoTemplate.class);
        passwordEncoder = new BCryptPasswordEncoder();
        seederService = new DemoDataSeederService(mongoTemplate, passwordEncoder);
        seederService.setDemoPassword("DemoStudent@2026!");

        com.mongodb.client.result.DeleteResult deleteResult = Mockito.mock(com.mongodb.client.result.DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(0L);
        when(mongoTemplate.remove(any(Query.class), anyString())).thenReturn(deleteResult);
    }

    @Test
    void testPreservationOfNonDemoRecords_OnlyDeletesDemoTaggedRecords() {
        seederService.executeSeed();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<String> collCaptor = ArgumentCaptor.forClass(String.class);

        verify(mongoTemplate, atLeastOnce()).remove(queryCaptor.capture(), collCaptor.capture());

        List<Query> queries = queryCaptor.getAllValues();
        assertFalse(queries.isEmpty());

        for (Query q : queries) {
            Document queryObj = q.getQueryObject();
            assertTrue(queryObj.containsKey("demoSeed"), "Deletions must strictly target demoSeed property");
            assertEquals("sac-demo-v1", queryObj.get("demoSeed"), "Deletions must strictly match seed tag sac-demo-v1");
        }

        verify(mongoTemplate, never()).dropCollection(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUserPopulation_ThirtyNineUsersTotalWithAdminAndTeachers() {
        seederService.executeSeed();

        Map<String, List<Document>> byCollection = captureInsertedDocuments();

        List<Document> users = byCollection.get("users");
        assertNotNull(users);
        assertEquals(39, users.size(), "Total users must be exactly 39 (30 students, 8 teachers, 1 admin)");

        // 1. Admin
        List<Document> admins = users.stream().filter(u -> "ADMIN".equals(u.getString("role"))).toList();
        assertEquals(1, admins.size(), "Must have exactly 1 admin user");
        Document admin = admins.get(0);
        assertEquals("admin.demo@example.com", admin.getString("email"));
        assertEquals("ACTIVE", admin.getString("status"));
        assertTrue(passwordEncoder.matches("DemoStudent@2026!", admin.getString("passwordHash")));

        // 2. Teachers
        List<Document> teachers = users.stream().filter(u -> "TEACHER".equals(u.getString("role"))).toList();
        assertEquals(8, teachers.size(), "Must have exactly 8 teacher users");

        // 3. Students
        List<Document> studentUsers = users.stream().filter(u -> "STUDENT".equals(u.getString("role"))).toList();
        assertEquals(30, studentUsers.size(), "Must have exactly 30 student users");

        long cseCount = studentUsers.stream().filter(u -> u.getString("email").endsWith(".cse@example.com")).count();
        long itCount = studentUsers.stream().filter(u -> u.getString("email").endsWith(".it@example.com")).count();
        assertEquals(15, cseCount, "Must have exactly 15 CSE student accounts");
        assertEquals(15, itCount, "Must have exactly 15 IT student accounts");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testStudentAndCourseEnrollmentIsolation() {
        seederService.executeSeed();

        Map<String, List<Document>> byCollection = captureInsertedDocuments();

        // 1. Students profiles
        List<Document> students = byCollection.get("students");
        assertNotNull(students);
        assertEquals(30, students.size(), "Must have exactly 30 student profiles");

        List<Document> cseStudents = students.stream().filter(s -> "Computer Science and Engineering".equals(s.getString("department"))).toList();
        List<Document> itStudents = students.stream().filter(s -> "Information Technology".equals(s.getString("department"))).toList();
        assertEquals(15, cseStudents.size(), "Must have 15 CSE student profiles");
        assertEquals(15, itStudents.size(), "Must have 15 IT student profiles");

        // 2. 8 unique subjects
        List<Document> courses = byCollection.get("courses");
        assertNotNull(courses);
        assertEquals(8, courses.size(), "Must have exactly 8 unique subjects");

        Set<String> cseCourseCodes = Set.of("BCSE203", "BCSE204", "BCSE207", "BCSE205");
        Set<String> itCourseCodes = Set.of("BITC201", "BITC202", "BITC203", "BITC204");

        Set<String> allCodes = courses.stream().map(c -> c.getString("code")).collect(Collectors.toSet());
        assertEquals(8, allCodes.size(), "All 8 course codes must be unique");
        assertTrue(allCodes.containsAll(cseCourseCodes));
        assertTrue(allCodes.containsAll(itCourseCodes));

        // 3. Enrollments: exactly 120 (30 students * 4 each)
        List<Document> enrollments = byCollection.get("enrollments");
        assertNotNull(enrollments);
        assertEquals(120, enrollments.size(), "Total enrollments must be exactly 120");

        // Verify each CSE student is enrolled in exactly 4 CSE courses (and 0 IT courses)
        for (Document cseStudent : cseStudents) {
            ObjectId sId = cseStudent.getObjectId("_id");
            List<Document> studentEnrs = enrollments.stream().filter(e -> sId.equals(e.getObjectId("studentId"))).toList();
            assertEquals(4, studentEnrs.size(), "Every CSE student must be enrolled in exactly 4 subjects");
            Set<String> enrCodes = studentEnrs.stream().map(e -> e.getString("courseCode")).collect(Collectors.toSet());
            assertEquals(cseCourseCodes, enrCodes, "CSE student must be enrolled strictly in CSE subjects");
        }

        // Verify each IT student is enrolled in exactly 4 IT courses (and 0 CSE courses)
        for (Document itStudent : itStudents) {
            ObjectId sId = itStudent.getObjectId("_id");
            List<Document> studentEnrs = enrollments.stream().filter(e -> sId.equals(e.getObjectId("studentId"))).toList();
            assertEquals(4, studentEnrs.size(), "Every IT student must be enrolled in exactly 4 subjects");
            Set<String> enrCodes = studentEnrs.stream().map(e -> e.getString("courseCode")).collect(Collectors.toSet());
            assertEquals(itCourseCodes, enrCodes, "IT student must be enrolled strictly in IT subjects");
        }

        // 4. Teaching assignments: exactly 8 (one teacher per unique subject)
        List<Document> teachingAssignments = byCollection.get("teaching_assignments");
        assertNotNull(teachingAssignments);
        assertEquals(8, teachingAssignments.size(), "Must have exactly 8 teaching assignments");
        Set<ObjectId> assignedTeachers = teachingAssignments.stream().map(t -> t.getObjectId("teacherId")).collect(Collectors.toSet());
        assertEquals(8, assignedTeachers.size(), "All 8 teachers must be assigned to subjects");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSchemaCompatibilityAndPendingAssessmentRules() {
        seederService.executeSeed();

        Map<String, List<Document>> byCollection = captureInsertedDocuments();

        // 1. Assessment Types in assessments collection
        Set<String> allowedTypes = Set.of("QUIZ", "ASSIGNMENT", "CAT", "FAT", "PROJECT", "OTHER");
        List<Document> assessments = byCollection.get("assessments");
        assertNotNull(assessments);
        assertEquals(56, assessments.size(), "8 classes * 7 assessments = 56 assessments");

        for (Document a : assessments) {
            String type = a.getString("type");
            assertTrue(allowedTypes.contains(type), "Assessment type '" + type + "' must be supported by schema.js enum");
            if (a.getString("name").contains("Lab")) {
                assertEquals("OTHER", type, "Lab assessment must use schema-compatible type 'OTHER'");
            }
        }

        // 2. Results rules: no null marks, FAT never in results, all numeric
        List<Document> results = byCollection.get("results");
        assertNotNull(results);
        assertFalse(results.isEmpty());

        for (Document r : results) {
            assertNotNull(r.get("marksObtained"), "Results must never contain null marksObtained");
            assertTrue(r.get("marksObtained") instanceof Number, "marksObtained must be numeric");
            double marks = ((Number) r.get("marksObtained")).doubleValue();
            assertTrue(marks >= 0.0, "marksObtained must be non-negative");
            assertEquals("COMPLETED", r.getString("status"), "Only completed assessments may have result documents");
            assertNotEquals("FAT", r.getString("assessmentType"), "FAT must NEVER be completed or inserted into results");
        }

        // 3. Attendance: 120 records (1 per enrollment)
        List<Document> attendance = byCollection.get("attendance");
        assertNotNull(attendance);
        assertEquals(120, attendance.size(), "Must have exactly 120 attendance records (1 per enrollment)");
        for (Document att : attendance) {
            assertNotNull(att.getObjectId("enrollmentId"));
            assertNotNull(att.getDate("date"));
            assertEquals("PRESENT", att.getString("status"));
            assertNotNull(att.getDouble("attendancePercentage"));
        }
    }

    @Test
    void testIdempotencyAndRerunSafety() {
        DemoDataSeederService.SeedSummary first = seederService.executeSeed();
        DemoDataSeederService.SeedSummary second = seederService.executeSeed();

        assertTrue(first.isExecuted());
        assertTrue(second.isExecuted());
        assertEquals(first.getInsertedCounts(), second.getInsertedCounts(), "Subsequent seed runs must produce identical counts");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, atLeast(32)).remove(queryCaptor.capture(), anyString());

        for (Query q : queryCaptor.getAllValues()) {
            assertEquals("sac-demo-v1", q.getQueryObject().get("demoSeed"),
                    "All deletion queries must strictly filter on demoSeed: sac-demo-v1");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<Document>> captureInsertedDocuments() {
        ArgumentCaptor<Collection<Document>> docsCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<String> collCaptor = ArgumentCaptor.forClass(String.class);

        verify(mongoTemplate, atLeastOnce()).insert(docsCaptor.capture(), collCaptor.capture());

        List<Collection<Document>> allBatches = docsCaptor.getAllValues();
        List<String> allCollections = collCaptor.getAllValues();

        Map<String, List<Document>> byCollection = new HashMap<>();
        for (int i = 0; i < allCollections.size(); i++) {
            byCollection.put(allCollections.get(i), (List<Document>) allBatches.get(i));
        }
        return byCollection;
    }
}
