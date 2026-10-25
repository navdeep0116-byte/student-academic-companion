package com.academiccompanion.backend.service;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DemoDataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeederService.class);
    public static final String SEED_TAG = "sac-demo-v1";

    public static final List<String> SEEDED_COLLECTIONS = List.of(
            "enrollments", "courses", "semesters", "assessments", "assignments", "attendance",
            "results", "predictions", "recommendations", "study_sessions", "submissions",
            "teaching_assignments", "classes", "teachers", "students", "users"
    );

    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.demo-password:DemoStudent@2026!}")
    private String demoPassword;

    public DemoDataSeederService(MongoTemplate mongoTemplate, PasswordEncoder passwordEncoder) {
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public void setDemoPassword(String demoPassword) {
        this.demoPassword = demoPassword;
    }

    public static class SeedSummary {
        private final Map<String, Integer> insertedCounts = new LinkedHashMap<>();
        private final Map<String, Long> deletedCounts = new LinkedHashMap<>();
        private boolean executed = false;

        public void recordInsert(String collection, int count) {
            insertedCounts.put(collection, count);
        }

        public void recordDelete(String collection, long count) {
            deletedCounts.put(collection, count);
        }

        public Map<String, Integer> getInsertedCounts() {
            return insertedCounts;
        }

        public Map<String, Long> getDeletedCounts() {
            return deletedCounts;
        }

        public boolean isExecuted() {
            return executed;
        }

        public void setExecuted(boolean executed) {
            this.executed = executed;
        }
    }

    public SeedSummary executeSeed() {
        log.info("Starting safe synthetic demo data seeding with tag: {}", SEED_TAG);
        SeedSummary summary = new SeedSummary();

        // 1. Idempotency: Remove only prior records carrying demoSeed: "sac-demo-v1"
        for (String coll : SEEDED_COLLECTIONS) {
            Query query = new Query(Criteria.where("demoSeed").is(SEED_TAG));
            long deleted = mongoTemplate.remove(query, coll).getDeletedCount();
            summary.recordDelete(coll, deleted);
            log.info("Cleaned prior demo records from collection '{}': {} deleted", coll, deleted);
        }

        Date now = new Date();
        String passwordToHash = (demoPassword != null && !demoPassword.isBlank()) ? demoPassword : "DemoStudent@2026!";
        String demoPasswordHash = (passwordEncoder != null)
                ? passwordEncoder.encode(passwordToHash)
                : "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

        List<Document> userDocs = new ArrayList<>();

        // 2. Admin Account (1 User with role ADMIN)
        ObjectId adminUserId = new ObjectId();
        Document adminUser = new Document("_id", adminUserId)
                .append("fullName", "System Administrator")
                .append("email", "admin.demo@example.com")
                .append("passwordHash", demoPasswordHash)
                .append("role", "ADMIN")
                .append("status", "ACTIVE")
                .append("createdAt", now)
                .append("demoSeed", SEED_TAG);
        userDocs.add(adminUser);

        // 3. Teachers (8 Faculty members: 4 CSE, 4 IT)
        List<String[]> teachersInfo = List.of(
                // CSE Faculty
                new String[]{"Dr. Priya Sharma", "priya.sharma.demo@example.com", "Computer Science and Engineering", "Associate Professor"},
                new String[]{"Prof. Amit Verma", "amit.verma.demo@example.com", "Computer Science and Engineering", "Assistant Professor"},
                new String[]{"Dr. Suresh Reddy", "suresh.reddy.demo@example.com", "Computer Science and Engineering", "Associate Professor"},
                new String[]{"Prof. Neha Iyer", "neha.iyer.demo@example.com", "Computer Science and Engineering", "Assistant Professor"},
                // IT Faculty
                new String[]{"Prof. Karan Mehta", "karan.mehta.demo@example.com", "Information Technology", "Associate Professor"},
                new String[]{"Dr. Ananya Sen", "ananya.sen.demo@example.com", "Information Technology", "Associate Professor"},
                new String[]{"Prof. Rajesh Nair", "rajesh.nair.demo@example.com", "Information Technology", "Assistant Professor"},
                new String[]{"Dr. Sara Khan", "sara.khan.demo@example.com", "Information Technology", "Associate Professor"}
        );

        List<Document> teacherDocs = new ArrayList<>();
        List<ObjectId> teacherIds = new ArrayList<>();

        for (int i = 0; i < teachersInfo.size(); i++) {
            String[] t = teachersInfo.get(i);
            ObjectId userId = new ObjectId();
            ObjectId teacherId = new ObjectId();
            teacherIds.add(teacherId);

            Document user = new Document("_id", userId)
                    .append("fullName", t[0])
                    .append("email", t[1])
                    .append("passwordHash", demoPasswordHash)
                    .append("role", "TEACHER")
                    .append("status", "ACTIVE")
                    .append("createdAt", now)
                    .append("demoSeed", SEED_TAG);
            userDocs.add(user);

            Document teacher = new Document("_id", teacherId)
                    .append("userId", userId)
                    .append("employeeNumber", String.format("TCH%03d", i + 1))
                    .append("fullName", t[0])
                    .append("department", t[2])
                    .append("designation", t[3])
                    .append("createdAt", now)
                    .append("demoSeed", SEED_TAG);
            teacherDocs.add(teacher);
        }

        // 4. Exactly 30 Students: 15 CSE + 15 IT
        List<String> cseFirstNames = List.of(
                "Aarav", "Aditya", "Arjun", "Vihaan", "Kabir",
                "Dev", "Rohan", "Nikhil", "Manav", "Yash",
                "Dhruv", "Kunal", "Reyansh", "Ishaan", "Krishna"
        );
        List<String> cseLastNames = List.of(
                "Sharma", "Verma", "Gupta", "Singh", "Patel",
                "Reddy", "Iyer", "Mehta", "Rao", "Nair",
                "Kapoor", "Bhatia", "Joshi", "Agarwal", "Malhotra"
        );

        List<String> itFirstNames = List.of(
                "Diya", "Ananya", "Isha", "Meera", "Saanvi",
                "Aditi", "Sara", "Avni", "Kiara", "Tara",
                "Riya", "Anika", "Pihu", "Naina", "Myra"
        );
        List<String> itLastNames = List.of(
                "Patel", "Sharma", "Verma", "Gupta", "Reddy",
                "Singh", "Iyer", "Mehta", "Rao", "Nair",
                "Kapoor", "Bhatia", "Joshi", "Agarwal", "Malhotra"
        );

        List<Document> studentDocs = new ArrayList<>();
        List<ObjectId> cseStudentIds = new ArrayList<>();
        List<ObjectId> cseStudentUserIds = new ArrayList<>();
        List<ObjectId> itStudentIds = new ArrayList<>();
        List<ObjectId> itStudentUserIds = new ArrayList<>();

        // Generate 15 CSE Students
        for (int i = 0; i < 15; i++) {
            ObjectId userId = new ObjectId();
            ObjectId studentId = new ObjectId();
            cseStudentUserIds.add(userId);
            cseStudentIds.add(studentId);

            String fullName = cseFirstNames.get(i) + " " + cseLastNames.get(i);
            String email = String.format("student%02d.cse@example.com", i + 1);
            String studentNumber = String.format("SAC-CSE-%03d", i + 1);

            Document user = new Document("_id", userId)
                    .append("fullName", fullName)
                    .append("email", email)
                    .append("passwordHash", demoPasswordHash)
                    .append("role", "STUDENT")
                    .append("status", "ACTIVE")
                    .append("createdAt", now)
                    .append("demoSeed", SEED_TAG);
            userDocs.add(user);

            Document student = new Document("_id", studentId)
                    .append("userId", userId)
                    .append("studentNumber", studentNumber)
                    .append("fullName", fullName)
                    .append("program", "B.Tech Computer Science and Engineering")
                    .append("department", "Computer Science and Engineering")
                    .append("yearOfStudy", 3)
                    .append("section", "A")
                    .append("createdAt", now)
                    .append("demoSeed", SEED_TAG);
            studentDocs.add(student);
        }

        // Generate 15 IT Students
        for (int i = 0; i < 15; i++) {
            ObjectId userId = new ObjectId();
            ObjectId studentId = new ObjectId();
            itStudentUserIds.add(userId);
            itStudentIds.add(studentId);

            String fullName = itFirstNames.get(i) + " " + itLastNames.get(i);
            String email = String.format("student%02d.it@example.com", i + 1);
            String studentNumber = String.format("SAC-IT-%03d", i + 1);

            Document user = new Document("_id", userId)
                    .append("fullName", fullName)
                    .append("email", email)
                    .append("passwordHash", demoPasswordHash)
                    .append("role", "STUDENT")
                    .append("status", "ACTIVE")
                    .append("createdAt", now)
                    .append("demoSeed", SEED_TAG);
            userDocs.add(user);

            Document student = new Document("_id", studentId)
                    .append("userId", userId)
                    .append("studentNumber", studentNumber)
                    .append("fullName", fullName)
                    .append("program", "B.Tech Information Technology")
                    .append("department", "Information Technology")
                    .append("yearOfStudy", 3)
                    .append("section", "B")
                    .append("createdAt", now)
                    .append("demoSeed", SEED_TAG);
            studentDocs.add(student);
        }

        // 5. Semester
        ObjectId semesterId = new ObjectId();
        Document semesterDoc = new Document("_id", semesterId)
                .append("name", "Fall Semester 2026")
                .append("code", "FALL-2026")
                .append("academicYear", "2025–26")
                .append("term", "FALL")
                .append("startDate", new Date(1782864000000L)) // 2026-07-01
                .append("endDate", new Date(1797292800000L))   // 2026-12-15
                .append("status", "ACTIVE")
                .append("demoSeed", SEED_TAG);

        // 6. Courses (8 unique subjects total: 4 CSE, 4 IT)
        // [code, name, credits, evaluationType, dept, teacherIndex]
        List<Object[]> courseInfo = List.of(
                // 4 CSE Courses (Teacher indices 0..3)
                new Object[]{"BCSE203", "Database Systems", 4.0, "THEORY", "Computer Science and Engineering", 0},
                new Object[]{"BCSE204", "Data Structures and Algorithms", 4.0, "THEORY", "Computer Science and Engineering", 1},
                new Object[]{"BCSE207", "Operating Systems", 4.0, "THEORY", "Computer Science and Engineering", 2},
                new Object[]{"BCSE205", "Object Oriented Programming", 3.0, "EMBEDDED", "Computer Science and Engineering", 3},
                // 4 IT Courses (Teacher indices 4..7)
                new Object[]{"BITC201", "Computer Networks", 3.0, "THEORY", "Information Technology", 4},
                new Object[]{"BITC202", "Web Technologies & Software Systems", 3.0, "THEORY", "Information Technology", 5},
                new Object[]{"BITC203", "Cloud Computing & Architecture", 3.0, "THEORY", "Information Technology", 6},
                new Object[]{"BITC204", "Probability and Statistics for IT", 3.0, "THEORY", "Information Technology", 7}
        );

        List<Document> courseDocs = new ArrayList<>();
        Map<String, ObjectId> courseIdByCode = new HashMap<>();
        Map<String, Document> courseByCode = new HashMap<>();

        for (Object[] c : courseInfo) {
            String code = (String) c[0];
            String name = (String) c[1];
            Double credits = (Double) c[2];
            String evalType = (String) c[3];
            String dept = (String) c[4];
            int teacherIdx = (Integer) c[5];
            ObjectId teacherId = teacherIds.get(teacherIdx);

            ObjectId courseId = new ObjectId();
            courseIdByCode.put(code, courseId);

            Document course = new Document("_id", courseId)
                    .append("code", code)
                    .append("name", name)
                    .append("title", name)
                    .append("credits", credits)
                    .append("evaluationType", evalType)
                    .append("semesterId", semesterId)
                    .append("semester", "Fall Semester 2026")
                    .append("department", dept)
                    .append("teacherId", teacherId)
                    .append("status", "ACTIVE")
                    .append("createdAt", now)
                    .append("demoSeed", SEED_TAG);
            courseDocs.add(course);
            courseByCode.put(code, course);
        }

        // 7. Classes & Teaching Assignments (8 unique subjects -> 8 classes, 1 teacher each)
        List<Document> classDocs = new ArrayList<>();
        List<Document> teachingAssignmentDocs = new ArrayList<>();
        Map<String, ObjectId> classIdByCode = new HashMap<>();

        // CSE Courses -> Section A Classes
        List<String> cseCourseCodes = List.of("BCSE203", "BCSE204", "BCSE207", "BCSE205");
        for (String code : cseCourseCodes) {
            Document c = courseByCode.get(code);
            ObjectId courseId = c.getObjectId("_id");
            ObjectId teacherId = c.getObjectId("teacherId");
            ObjectId classId = new ObjectId();
            String className = code + "-A";
            classIdByCode.put(code, classId);

            Document clazz = new Document("_id", classId)
                    .append("name", className)
                    .append("courseId", courseId)
                    .append("courseCode", code)
                    .append("courseName", c.getString("name"))
                    .append("semesterId", semesterId)
                    .append("section", "A")
                    .append("capacity", 60)
                    .append("teacherId", teacherId)
                    .append("createdAt", now)
                    .append("demoSeed", SEED_TAG);
            classDocs.add(clazz);

            Document ta = new Document("_id", new ObjectId())
                    .append("teacherId", teacherId)
                    .append("courseId", courseId)
                    .append("classId", classId)
                    .append("semesterId", semesterId)
                    .append("assignedAt", now)
                    .append("status", "ACTIVE")
                    .append("demoSeed", SEED_TAG);
            teachingAssignmentDocs.add(ta);
        }

        // IT Courses -> Section B Classes
        List<String> itCourseCodes = List.of("BITC201", "BITC202", "BITC203", "BITC204");
        for (String code : itCourseCodes) {
            Document c = courseByCode.get(code);
            ObjectId courseId = c.getObjectId("_id");
            ObjectId teacherId = c.getObjectId("teacherId");
            ObjectId classId = new ObjectId();
            String className = code + "-B";
            classIdByCode.put(code, classId);

            Document clazz = new Document("_id", classId)
                    .append("name", className)
                    .append("courseId", courseId)
                    .append("courseCode", code)
                    .append("courseName", c.getString("name"))
                    .append("semesterId", semesterId)
                    .append("section", "B")
                    .append("capacity", 60)
                    .append("teacherId", teacherId)
                    .append("createdAt", now)
                    .append("demoSeed", SEED_TAG);
            classDocs.add(clazz);

            Document ta = new Document("_id", new ObjectId())
                    .append("teacherId", teacherId)
                    .append("courseId", courseId)
                    .append("classId", classId)
                    .append("semesterId", semesterId)
                    .append("assignedAt", now)
                    .append("status", "ACTIVE")
                    .append("demoSeed", SEED_TAG);
            teachingAssignmentDocs.add(ta);
        }

        // 8. Assessments per class (7 assessments per class: CAT 1, CAT 2, Quiz 1, Quiz 2, Assignment 1, Lab 1, FAT)
        // Schema compliant: Lab assessment uses type "OTHER" with "Lab" in title.
        List<Document> assessmentDocs = new ArrayList<>();
        Map<String, List<Document>> assessmentsByCourse = new HashMap<>();

        List<Object[]> assessmentBlueprint = List.of(
                new Object[]{"CAT 1", "CAT", 20.0, 15.0},
                new Object[]{"CAT 2", "CAT", 20.0, 15.0},
                new Object[]{"Quiz 1", "QUIZ", 10.0, 10.0},
                new Object[]{"Quiz 2", "QUIZ", 10.0, 10.0},
                new Object[]{"Assignment 1", "ASSIGNMENT", 10.0, 10.0},
                new Object[]{"Lab Assessment 1", "OTHER", 20.0, 20.0},
                new Object[]{"FAT", "FAT", 100.0, 40.0} // Always pending
        );

        for (Document clazz : classDocs) {
            String courseCode = clazz.getString("courseCode");
            ObjectId courseId = clazz.getObjectId("courseId");
            ObjectId classId = clazz.getObjectId("_id");

            List<Document> classAssessments = new ArrayList<>();
            for (Object[] a : assessmentBlueprint) {
                String aTitle = (String) a[0];
                String aType = (String) a[1];
                Double maxM = (Double) a[2];
                Double weight = (Double) a[3];

                Document assess = new Document("_id", new ObjectId())
                        .append("courseId", courseId)
                        .append("courseCode", courseCode)
                        .append("classId", classId)
                        .append("semesterId", semesterId)
                        .append("title", aTitle)
                        .append("name", aTitle)
                        .append("type", aType)
                        .append("maxMarks", maxM)
                        .append("weightage", weight)
                        .append("date", now)
                        .append("dueDate", now)
                        .append("status", "FAT".equals(aType) ? "PENDING" : "ACTIVE")
                        .append("createdAt", now)
                        .append("demoSeed", SEED_TAG);
                assessmentDocs.add(assess);
                classAssessments.add(assess);
            }
            assessmentsByCourse.put(courseCode, classAssessments);
        }

        // 9. Enrollments, Results, Attendance, Assignments & Submissions
        List<Document> enrollmentDocs = new ArrayList<>();
        List<Document> resultDocs = new ArrayList<>();
        List<Document> attendanceDocs = new ArrayList<>();
        List<Document> assignmentDocs = new ArrayList<>();
        List<Document> submissionDocs = new ArrayList<>();
        List<Document> predictionDocs = new ArrayList<>();
        List<Document> recommendationDocs = new ArrayList<>();
        List<Document> studySessionDocs = new ArrayList<>();

        // Helper to populate enrollments, results, attendance for a cohort
        populateCohort(
                cseStudentIds, cseStudentUserIds, cseCourseCodes, courseByCode, classIdByCode,
                assessmentsByCourse, semesterId, now,
                enrollmentDocs, resultDocs, attendanceDocs, assignmentDocs, submissionDocs,
                studySessionDocs, predictionDocs, recommendationDocs, "CSE"
        );

        populateCohort(
                itStudentIds, itStudentUserIds, itCourseCodes, courseByCode, classIdByCode,
                assessmentsByCourse, semesterId, now,
                enrollmentDocs, resultDocs, attendanceDocs, assignmentDocs, submissionDocs,
                studySessionDocs, predictionDocs, recommendationDocs, "IT"
        );

        // 10. Insert all generated documents into their respective collections
        insertCollection(summary, "users", userDocs);
        insertCollection(summary, "students", studentDocs);
        insertCollection(summary, "teachers", teacherDocs);
        insertCollection(summary, "semesters", List.of(semesterDoc));
        insertCollection(summary, "courses", courseDocs);
        insertCollection(summary, "classes", classDocs);
        insertCollection(summary, "teaching_assignments", teachingAssignmentDocs);
        insertCollection(summary, "enrollments", enrollmentDocs);
        insertCollection(summary, "assessments", assessmentDocs);
        insertCollection(summary, "assignments", assignmentDocs);
        insertCollection(summary, "submissions", submissionDocs);
        insertCollection(summary, "results", resultDocs);
        insertCollection(summary, "attendance", attendanceDocs);
        insertCollection(summary, "study_sessions", studySessionDocs);
        insertCollection(summary, "predictions", predictionDocs);
        insertCollection(summary, "recommendations", recommendationDocs);

        summary.setExecuted(true);
        log.info("Safe synthetic demo data seeding completed successfully: 39 users, 30 students, 8 teachers, 8 courses across 16 collections.");
        return summary;
    }

    private void populateCohort(
            List<ObjectId> studentIds, List<ObjectId> userIds, List<String> courseCodes,
            Map<String, Document> courseByCode, Map<String, ObjectId> classIdByCode,
            Map<String, List<Document>> assessmentsByCourse, ObjectId semesterId, Date now,
            List<Document> enrollmentDocs, List<Document> resultDocs, List<Document> attendanceDocs,
            List<Document> assignmentDocs, List<Document> submissionDocs,
            List<Document> studySessionDocs, List<Document> predictionDocs,
            List<Document> recommendationDocs, String deptTag) {

        for (int si = 0; si < studentIds.size(); si++) {
            ObjectId sId = studentIds.get(si);
            ObjectId uId = userIds.get(si);

            // Academic progression stage:
            // si 0..4 (5 students): Level 1 stage (CAT 1, Quiz 1, Assignment 1 completed)
            // si 5..9 (5 students): Level 2 stage (CAT 1, CAT 2, Quiz 1, Quiz 2, Assignment 1 completed)
            // si 10..14 (5 students): Level 3 stage (CAT 1, CAT 2, Quiz 1, Quiz 2, Assignment 1, Lab 1 completed)
            int stage = (si < 5) ? 1 : (si < 10) ? 2 : 3;

            for (int ci = 0; ci < courseCodes.size(); ci++) {
                String cCode = courseCodes.get(ci);
                Document course = courseByCode.get(cCode);
                ObjectId courseId = course.getObjectId("_id");
                ObjectId classId = classIdByCode.get(cCode);

                ObjectId enrollmentId = new ObjectId();
                Document enrollment = new Document("_id", enrollmentId)
                        .append("studentId", sId)
                        .append("userId", uId)
                        .append("classId", classId)
                        .append("courseId", courseId)
                        .append("courseCode", cCode)
                        .append("semesterId", semesterId)
                        .append("status", "ACTIVE")
                        .append("enrolledAt", now)
                        .append("demoSeed", SEED_TAG);
                enrollmentDocs.add(enrollment);

                // Assessments for course
                List<Document> assessments = assessmentsByCourse.get(cCode);
                for (Document assess : assessments) {
                    String aName = assess.getString("name");
                    String aType = assess.getString("type");
                    Double maxM = assess.getDouble("maxMarks");
                    ObjectId assessId = assess.getObjectId("_id");

                    boolean isCompleted;
                    if ("FAT".equals(aType)) {
                        isCompleted = false; // FAT is NEVER completed
                    } else if (stage == 1) {
                        isCompleted = "CAT 1".equals(aName) || "Quiz 1".equals(aName) || "Assignment 1".equals(aName);
                    } else if (stage == 2) {
                        isCompleted = "CAT 1".equals(aName) || "CAT 2".equals(aName) || "Quiz 1".equals(aName) || "Quiz 2".equals(aName) || "Assignment 1".equals(aName);
                    } else {
                        isCompleted = !"FAT".equals(aType); // All completed except FAT
                    }

                    if (isCompleted) {
                        double basePct = 0.65 + ((si * 7 + ci * 11) % 30) / 100.0;
                        double obtained = Math.round(maxM * basePct * 10.0) / 10.0;
                        String grade = (basePct >= 0.9) ? "S" : (basePct >= 0.8) ? "A" : (basePct >= 0.7) ? "B" : (basePct >= 0.6) ? "C" : "D";

                        // Strict Schema Rule: only completed assessments with non-null numeric marksObtained are inserted!
                        Document result = new Document("_id", new ObjectId())
                                .append("studentId", sId)
                                .append("userId", uId)
                                .append("enrollmentId", enrollmentId)
                                .append("courseId", courseId)
                                .append("courseCode", cCode)
                                .append("assessmentId", assessId)
                                .append("assessmentName", aName)
                                .append("assessmentType", aType)
                                .append("obtainedMarks", obtained)
                                .append("marksObtained", obtained)
                                .append("maxMarks", maxM)
                                .append("status", "COMPLETED")
                                .append("grade", grade)
                                .append("semesterId", semesterId)
                                .append("demoSeed", SEED_TAG);
                        resultDocs.add(result);

                        if ("ASSIGNMENT".equals(aType)) {
                            // Only add assignment definition once per class
                            if (si == 0) {
                                Document assign = new Document("_id", new ObjectId())
                                        .append("courseId", courseId)
                                        .append("classId", classId)
                                        .append("semesterId", semesterId)
                                        .append("title", "Assignment 1")
                                        .append("maxMarks", maxM)
                                        .append("dueDate", now)
                                        .append("createdAt", now)
                                        .append("demoSeed", SEED_TAG);
                                assignmentDocs.add(assign);
                            }

                            Document sub = new Document("_id", new ObjectId())
                                    .append("assignmentId", assessId) // Linked to assignment assessment
                                    .append("studentId", sId)
                                    .append("courseId", courseId)
                                    .append("submittedAt", now)
                                    .append("status", "SUBMITTED")
                                    .append("attemptNumber", 1)
                                    .append("marksObtained", obtained)
                                    .append("demoSeed", SEED_TAG);
                            submissionDocs.add(sub);
                        }
                    }
                }

                // Attendance per enrollment
                int conducted = 40;
                double attRate = Math.min(96.0, Math.max(68.0, 72.0 + ((si * 13 + ci * 7) % 25)));
                int attended = (int) Math.round(conducted * attRate / 100.0);
                double actualPct = Math.round((attended * 100.0 / conducted) * 10.0) / 10.0;

                Document att = new Document("_id", new ObjectId())
                        .append("studentId", sId)
                        .append("userId", uId)
                        .append("enrollmentId", enrollmentId)
                        .append("courseId", courseId)
                        .append("courseCode", cCode)
                        .append("semesterId", semesterId)
                        .append("classesConducted", conducted)
                        .append("classesAttended", attended)
                        .append("classesMissed", conducted - attended)
                        .append("attendancePercentage", actualPct)
                        .append("date", now)
                        .append("status", "PRESENT")
                        .append("updatedAt", now)
                        .append("createdAt", now)
                        .append("demoSeed", SEED_TAG);
                attendanceDocs.add(att);
            }

            // Study Session (1 per student)
            studySessionDocs.add(new Document("_id", new ObjectId())
                    .append("studentId", sId)
                    .append("courseId", courseByCode.get(courseCodes.get(si % courseCodes.size())).getObjectId("_id"))
                    .append("startedAt", now)
                    .append("durationMinutes", 45 + (si % 3) * 15)
                    .append("topic", "Core concepts and practice")
                    .append("notes", "Synthetic demo study session for " + deptTag)
                    .append("demoSeed", SEED_TAG));

            // Prediction (1 per student)
            double predicted = Math.min(95.0, Math.max(55.0, 60.0 + ((si * 17) % 36)));
            String risk = (predicted < 60.0) ? "HIGH" : (predicted < 75.0) ? "MEDIUM" : "LOW";
            predictionDocs.add(new Document("_id", new ObjectId())
                    .append("studentId", sId)
                    .append("userId", uId)
                    .append("semesterId", semesterId)
                    .append("predictedOverallScore", predicted)
                    .append("predictedScore", predicted)
                    .append("riskLevel", risk)
                    .append("modelVersion", "v1.0-synthetic")
                    .append("predictedAt", now)
                    .append("demoSeed", SEED_TAG));

            // Recommendation (1 per student)
            recommendationDocs.add(new Document("_id", new ObjectId())
                    .append("studentId", sId)
                    .append("semesterId", semesterId)
                    .append("title", "HIGH".equals(risk) ? "Academic Recovery Plan" : "Consistency and Practice")
                    .append("description", "HIGH".equals(risk) ? "Focus on lowest-scoring modules and meet instructor." : "Maintain weekly revision schedule.")
                    .append("category", "PERFORMANCE")
                    .append("status", "NEW")
                    .append("createdAt", now)
                    .append("demoSeed", SEED_TAG));
        }
    }

    private void insertCollection(SeedSummary summary, String collectionName, List<Document> docs) {
        if (!docs.isEmpty()) {
            mongoTemplate.insert(docs, collectionName);
            summary.recordInsert(collectionName, docs.size());
            log.info("Inserted {} records into collection '{}'", docs.size(), collectionName);
        }
    }

    public Map<String, Object> seedQuiz3ForDhruvKapoor() {
        log.info("Starting targeted Quiz 3 update for student Dhruv Kapoor (student11.cse@example.com)...");
        Map<String, Object> stats = new LinkedHashMap<>();

        // 1. Resolve user for student11.cse@example.com
        Document user = mongoTemplate.findOne(
                Query.query(Criteria.where("email").is("student11.cse@example.com")),
                Document.class,
                "users"
        );
        if (user == null) {
            stats.put("error", "User student11.cse@example.com not found");
            return stats;
        }

        ObjectId userId = user.getObjectId("_id");

        // 2. Resolve student record
        Document student = mongoTemplate.findOne(
                Query.query(Criteria.where("userId").is(userId)),
                Document.class,
                "students"
        );
        if (student == null) {
            stats.put("error", "Student profile for student11.cse@example.com not found");
            return stats;
        }

        ObjectId studentId = student.getObjectId("_id");
        stats.put("studentNumber", student.getString("studentNumber"));
        stats.put("fullName", student.getString("fullName"));

        // 3. Find active enrollments for this student
        List<Document> enrollments = mongoTemplate.find(
                Query.query(Criteria.where("studentId").is(studentId).and("status").is("ACTIVE")),
                Document.class,
                "enrollments"
        );
        stats.put("enrolledCoursesCount", enrollments.size());

        int assessmentsInserted = 0;
        int assessmentsReused = 0;
        int resultsInserted = 0;
        int resultsReused = 0;
        Date now = new Date();

        Map<String, Double> quiz3Scores = Map.of(
                "BCSE203", 8.5,
                "BCSE204", 8.0,
                "BCSE207", 9.0,
                "BCSE205", 8.5
        );

        for (Document enrollment : enrollments) {
            ObjectId enrollmentId = enrollment.getObjectId("_id");
            ObjectId courseId = enrollment.getObjectId("courseId");
            ObjectId classId = enrollment.getObjectId("classId");
            ObjectId semesterId = enrollment.getObjectId("semesterId");
            String courseCode = enrollment.getString("courseCode");

            // A. Check if Quiz 3 assessment already exists for this class/course
            Query assessQuery = Query.query(
                    Criteria.where("classId").is(classId)
                            .and("type").is("QUIZ")
                            .and("title").is("Quiz 3")
            );
            Document existingAssessment = mongoTemplate.findOne(assessQuery, Document.class, "assessments");
            ObjectId assessmentId;

            if (existingAssessment != null) {
                assessmentId = existingAssessment.getObjectId("_id");
                assessmentsReused++;
                log.info("Reusing existing Quiz 3 assessment '{}' for course {}", assessmentId, courseCode);
            } else {
                assessmentId = new ObjectId();
                Document newAssessment = new Document("_id", assessmentId)
                        .append("courseId", courseId)
                        .append("courseCode", courseCode)
                        .append("classId", classId)
                        .append("semesterId", semesterId)
                        .append("title", "Quiz 3")
                        .append("name", "Quiz 3")
                        .append("type", "QUIZ")
                        .append("maxMarks", 10.0)
                        .append("weightage", 10.0)
                        .append("date", now)
                        .append("status", "ACTIVE")
                        .append("createdAt", now)
                        .append("demoSeed", SEED_TAG);
                mongoTemplate.insert(newAssessment, "assessments");
                assessmentsInserted++;
                log.info("Inserted new Quiz 3 assessment '{}' for course {}", assessmentId, courseCode);
            }

            // B. Check if Quiz 3 result already exists for this student & course
            Query resultQuery = Query.query(
                    Criteria.where("studentId").is(studentId)
                            .and("courseId").is(courseId)
                            .and("assessmentId").is(assessmentId)
            );
            Document existingResult = mongoTemplate.findOne(resultQuery, Document.class, "results");

            if (existingResult != null) {
                resultsReused++;
                log.info("Quiz 3 result already exists for student in course {}", courseCode);
            } else {
                double score = quiz3Scores.getOrDefault(courseCode, 8.5);
                Document newResult = new Document("_id", new ObjectId())
                        .append("studentId", studentId)
                        .append("userId", userId)
                        .append("enrollmentId", enrollmentId)
                        .append("courseId", courseId)
                        .append("courseCode", courseCode)
                        .append("assessmentId", assessmentId)
                        .append("assessmentName", "Quiz 3")
                        .append("assessmentType", "QUIZ")
                        .append("obtainedMarks", score)
                        .append("marksObtained", score)
                        .append("maxMarks", 10.0)
                        .append("status", "COMPLETED")
                        .append("grade", score >= 9.0 ? "S" : score >= 8.0 ? "A" : "B")
                        .append("semesterId", semesterId)
                        .append("demoSeed", SEED_TAG);
                mongoTemplate.insert(newResult, "results");
                resultsInserted++;
                log.info("Inserted Quiz 3 result for student in course {}: {}/10.0", courseCode, score);
            }
        }

        stats.put("assessmentsInserted", assessmentsInserted);
        stats.put("assessmentsReused", assessmentsReused);
        stats.put("resultsInserted", resultsInserted);
        stats.put("resultsReused", resultsReused);
        return stats;
    }
}
