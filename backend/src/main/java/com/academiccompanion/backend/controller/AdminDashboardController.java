package com.academiccompanion.backend.controller;

import com.academiccompanion.backend.model.Course;
import com.academiccompanion.backend.model.Student;
import com.academiccompanion.backend.model.Teacher;
import com.academiccompanion.backend.model.User;
import com.academiccompanion.backend.repository.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.mongodb.core.query.Update;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private MongoTemplate mongoTemplate;
    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private StudentRepository studentRepository;
    private TeacherRepository teacherRepository;
    private EnrollmentRepository enrollmentRepository;
    private PasswordEncoder passwordEncoder;

    public AdminDashboardController() {
    }

    @Autowired
    public AdminDashboardController(
            MongoTemplate mongoTemplate,
            UserRepository userRepository,
            CourseRepository courseRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            EnrollmentRepository enrollmentRepository,
            PasswordEncoder passwordEncoder) {
        this.mongoTemplate = mongoTemplate;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private User getAuthenticatedAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        if (user.getRole() != User.Role.ADMIN) {
            return null;
        }
        return user;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "CONNECTED");
        response.put("userId", user.getId());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview(Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        if (mongoTemplate == null) {
            return ResponseEntity.ok(Map.of("status", "CONNECTED"));
        }

        long totalStudents = mongoTemplate.count(new Query(), "students");
        long activeStudents = mongoTemplate.count(
                new Query(Criteria.where("role").is("STUDENT").and("status").is("ACTIVE")),
                "users"
        );
        long pendingStudents = mongoTemplate.count(
                new Query(Criteria.where("role").is("STUDENT").and("status").is("PENDING")),
                "users"
        );

        long totalTeachers = mongoTemplate.count(new Query(), "teachers");
        long activeTeachers = mongoTemplate.count(
                new Query(Criteria.where("role").is("TEACHER").and("status").is("ACTIVE")),
                "users"
        );
        long pendingTeachers = mongoTemplate.count(
                new Query(Criteria.where("role").is("TEACHER").and("status").is("PENDING")),
                "users"
        );

        long totalCourses = mongoTemplate.count(new Query(), "courses");
        long totalClasses = mongoTemplate.count(new Query(), "classes");
        long totalEnrollments = mongoTemplate.count(new Query(), "enrollments");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("activeStudents", activeStudents);
        stats.put("pendingStudents", pendingStudents);
        stats.put("totalTeachers", totalTeachers);
        stats.put("activeTeachers", activeTeachers);
        stats.put("pendingTeachers", pendingTeachers);
        stats.put("totalCourses", totalCourses);
        stats.put("totalClasses", totalClasses);
        stats.put("totalEnrollments", totalEnrollments);

        // Recent activity summary
        List<Map<String, String>> recentActivity = List.of(
                Map.of("title", "Database Verified", "detail", "Spring 2026 academic catalog active"),
                Map.of("title", "Enrolled Students Active", "detail", totalStudents + " students across " + totalCourses + " courses"),
                Map.of("title", "Faculty Assigned", "detail", totalTeachers + " teachers registered"),
                Map.of("title", "Standard Classes", "detail", totalClasses + " course sections active")
        );
        stats.put("recentActivity", recentActivity);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        List<Document> userDocs = mongoTemplate.find(new Query(), Document.class, "users");
        List<Document> studentDocs = mongoTemplate.find(new Query(), Document.class, "students");
        List<Document> teacherDocs = mongoTemplate.find(new Query(), Document.class, "teachers");

        Map<String, Document> studentsByUserId = new HashMap<>();
        for (Document s : studentDocs) {
            ObjectId uid = s.getObjectId("userId");
            if (uid != null) studentsByUserId.put(uid.toHexString(), s);
        }

        Map<String, Document> teachersByUserId = new HashMap<>();
        for (Document t : teacherDocs) {
            ObjectId uid = t.getObjectId("userId");
            if (uid != null) teachersByUserId.put(uid.toHexString(), t);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Document u : userDocs) {
            ObjectId uid = u.getObjectId("_id");
            String uidHex = uid != null ? uid.toHexString() : "";

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", uidHex);
            map.put("name", u.getString("fullName"));
            map.put("email", u.getString("email"));
            map.put("role", u.getString("role"));
            map.put("status", u.getString("status") != null ? u.getString("status") : "ACTIVE");

            Date createdAt = u.getDate("createdAt");
            map.put("date", createdAt != null ? createdAt.toInstant().toString().substring(0, 10) : "2026-01-01");

            if ("STUDENT".equalsIgnoreCase(u.getString("role"))) {
                Document s = studentsByUserId.get(uidHex);
                if (s != null) {
                    map.put("studentId", s.getObjectId("_id") != null ? s.getObjectId("_id").toHexString() : null);
                    map.put("studentNumber", s.getString("studentNumber"));
                    map.put("department", s.getString("department"));
                    map.put("section", s.getString("section"));
                    map.put("previousCgpa", toDouble(s.get("previousCgpa")));
                    Date cgpaUpdated = s.getDate("previousCgpaUpdatedAt");
                    map.put("previousCgpaUpdatedAt", cgpaUpdated != null ? cgpaUpdated.toInstant().toString() : null);
                    map.put("previousCgpaUpdatedBy", s.getString("previousCgpaUpdatedBy"));
                }
            } else if ("TEACHER".equalsIgnoreCase(u.getString("role"))) {
                Document t = teachersByUserId.get(uidHex);
                if (t != null) {
                    map.put("employeeNumber", t.getString("employeeNumber"));
                    map.put("department", t.getString("department"));
                }
            }

            result.add(map);
        }

        return ResponseEntity.ok(Map.of("users", result));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        String name = payload.get("name");
        String email = payload.get("email");
        String rawRole = payload.get("role");
        String password = payload.get("password");

        if (name == null || name.trim().isEmpty() || email == null || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid name and email are required"));
        }

        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        boolean exists = mongoTemplate.exists(new Query(Criteria.where("email").is(cleanEmail)), "users");
        if (exists) {
            return ResponseEntity.badRequest().body(Map.of("error", "A user with this email already exists"));
        }

        User.Role role = User.Role.STUDENT;
        if (rawRole != null) {
            try {
                role = User.Role.valueOf(rawRole.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }

        String passToHash = (password != null && !password.trim().isEmpty()) ? password.trim() : "Welcome@2026!";
        String passwordHash = passwordEncoder != null ? passwordEncoder.encode(passToHash) : passToHash;

        Document userDoc = new Document()
                .append("fullName", name.trim())
                .append("email", cleanEmail)
                .append("passwordHash", passwordHash)
                .append("role", role.name())
                .append("status", "ACTIVE")
                .append("createdAt", new Date())
                .append("demoSeed", "sac-demo-v1");

        mongoTemplate.insert(userDoc, "users");
        ObjectId newUserId = userDoc.getObjectId("_id");

        if (role == User.Role.STUDENT) {
            long count = mongoTemplate.count(new Query(), "students") + 1;
            String studentNum = payload.get("studentNumber");
            if (studentNum == null || studentNum.trim().isEmpty()) {
                studentNum = String.format("25BCE%04d", 2500 + count);
            }
            Document studentDoc = new Document()
                    .append("userId", newUserId)
                    .append("studentNumber", studentNum.trim())
                    .append("fullName", name.trim())
                    .append("program", "B.Tech Computer Science and Engineering")
                    .append("department", "Computer Science")
                    .append("yearOfStudy", 2)
                    .append("section", payload.getOrDefault("section", "A"))
                    .append("createdAt", new Date())
                    .append("demoSeed", "sac-demo-v1");
            mongoTemplate.insert(studentDoc, "students");
        } else if (role == User.Role.TEACHER) {
            long count = mongoTemplate.count(new Query(), "teachers") + 1;
            String empNum = payload.get("employeeNumber");
            if (empNum == null || empNum.trim().isEmpty()) {
                empNum = String.format("EMP%04d", 1000 + count);
            }
            Document teacherDoc = new Document()
                    .append("userId", newUserId)
                    .append("employeeNumber", empNum.trim())
                    .append("fullName", name.trim())
                    .append("department", payload.getOrDefault("department", "Computer Science"))
                    .append("designation", payload.getOrDefault("designation", "Assistant Professor"))
                    .append("createdAt", new Date())
                    .append("demoSeed", "sac-demo-v1");
            mongoTemplate.insert(teacherDoc, "teachers");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "userId", newUserId.toHexString(),
                "email", cleanEmail,
                "role", role.name()
        ));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, String> payload,
            Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        if (!ObjectId.isValid(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user ID"));
        }

        Document userDoc = mongoTemplate.findById(new ObjectId(id), Document.class, "users");
        if (userDoc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        String currentStatus = userDoc.getString("status");
        String nextStatus;
        if (payload != null && payload.containsKey("status")) {
            String requested = payload.get("status").trim().toUpperCase(Locale.ROOT);
            if ("INACTIVE".equals(requested) || "SUSPENDED".equals(requested)) {
                nextStatus = "SUSPENDED";
            } else if ("ACTIVE".equals(requested)) {
                nextStatus = "ACTIVE";
            } else if ("PENDING".equals(requested)) {
                nextStatus = "PENDING";
            } else {
                nextStatus = requested;
            }
        } else {
            nextStatus = "ACTIVE".equalsIgnoreCase(currentStatus) ? "SUSPENDED" : "ACTIVE";
        }

        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(new ObjectId(id))),
                new org.springframework.data.mongodb.core.query.Update().set("status", nextStatus),
                "users"
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "id", id,
                "status", nextStatus
        ));
    }

    @GetMapping("/courses")
    public ResponseEntity<?> getCourses(Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        List<Document> courses = mongoTemplate.find(new Query(), Document.class, "courses");
        List<Document> classes = mongoTemplate.find(new Query(), Document.class, "classes");
        List<Document> enrollments = mongoTemplate.find(new Query(), Document.class, "enrollments");
        List<Document> teachers = mongoTemplate.find(new Query(), Document.class, "teachers");

        Map<String, String> teacherNames = new HashMap<>();
        for (Document t : teachers) {
            ObjectId tid = t.getObjectId("_id");
            if (tid != null) teacherNames.put(tid.toHexString(), t.getString("fullName"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Document c : courses) {
            ObjectId cid = c.getObjectId("_id");
            String code = c.getString("code");

            // Count classes
            long classCount = classes.stream()
                    .filter(cls -> code.equalsIgnoreCase(cls.getString("courseCode")) ||
                            (cid != null && cid.equals(cls.getObjectId("courseId"))))
                    .count();

            // Count enrolled students
            long enrolledCount = enrollments.stream()
                    .filter(e -> code.equalsIgnoreCase(e.getString("courseCode")) ||
                            (cid != null && cid.equals(e.getObjectId("courseId"))))
                    .count();

            // Find assigned teachers for these classes
            Set<String> assignedTeacherNames = new LinkedHashSet<>();
            for (Document cls : classes) {
                if (code.equalsIgnoreCase(cls.getString("courseCode")) ||
                        (cid != null && cid.equals(cls.getObjectId("courseId")))) {
                    ObjectId tid = cls.getObjectId("teacherId");
                    if (tid != null && teacherNames.containsKey(tid.toHexString())) {
                        assignedTeacherNames.add(teacherNames.get(tid.toHexString()));
                    }
                }
            }

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", cid != null ? cid.toHexString() : null);
            map.put("code", code);
            map.put("name", c.getString("name"));
            map.put("credits", c.get("credits"));
            map.put("department", c.getString("department"));
            map.put("evaluationType", c.getString("evaluationType"));
            map.put("description", c.getString("description"));
            map.put("classes", classCount);
            map.put("enrolled", enrolledCount);
            map.put("teacher", assignedTeacherNames.isEmpty() ? "Unassigned" : String.join(", ", assignedTeacherNames));
            map.put("status", "Active");

            result.add(map);
        }

        return ResponseEntity.ok(Map.of("courses", result));
    }

    @PostMapping("/courses")
    public ResponseEntity<?> createCourse(
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        String code = (String) payload.get("code");
        String name = (String) payload.get("name");
        String department = (String) payload.getOrDefault("department", "Computer Science");
        Object creditsObj = payload.getOrDefault("credits", 4);
        int credits = creditsObj instanceof Number ? ((Number) creditsObj).intValue() : 4;
        String desc = (String) payload.getOrDefault("description", name);

        if (code == null || code.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Course code and name are required"));
        }

        String cleanCode = code.trim().toUpperCase(Locale.ROOT);
        boolean exists = mongoTemplate.exists(new Query(Criteria.where("code").is(cleanCode)), "courses");
        if (exists) {
            return ResponseEntity.badRequest().body(Map.of("error", "A course with this code already exists"));
        }

        Document doc = new Document()
                .append("code", cleanCode)
                .append("name", name.trim())
                .append("credits", credits)
                .append("department", department.trim())
                .append("evaluationType", "THEORY_AND_LAB")
                .append("description", desc.trim())
                .append("createdAt", new Date())
                .append("demoSeed", "sac-demo-v1");

        mongoTemplate.insert(doc, "courses");

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "courseId", doc.getObjectId("_id").toHexString(),
                "code", cleanCode,
                "name", name.trim()
        ));
    }

    @GetMapping("/teachers/available")
    public ResponseEntity<?> getAvailableTeachers(Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        List<Document> teachers = mongoTemplate.find(new Query(), Document.class, "teachers");
        List<Document> classes = mongoTemplate.find(new Query(), Document.class, "classes");

        Map<String, Long> teacherClassCounts = new HashMap<>();
        for (Document c : classes) {
            ObjectId tid = c.getObjectId("teacherId");
            if (tid != null) {
                teacherClassCounts.put(tid.toHexString(), teacherClassCounts.getOrDefault(tid.toHexString(), 0L) + 1);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Document t : teachers) {
            ObjectId tid = t.getObjectId("_id");
            String tidHex = tid != null ? tid.toHexString() : "";
            long assignedCount = teacherClassCounts.getOrDefault(tidHex, 0L);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", tidHex);
            map.put("name", t.getString("fullName"));
            map.put("employeeNumber", t.getString("employeeNumber"));
            map.put("department", t.getString("department"));
            map.put("assignedClassesCount", assignedCount);
            map.put("isAvailable", assignedCount == 0);

            result.add(map);
        }

        return ResponseEntity.ok(Map.of("teachers", result));
    }

    @GetMapping("/classes")
    public ResponseEntity<?> getClasses(Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        List<Document> classes = mongoTemplate.find(new Query(), Document.class, "classes");
        List<Document> enrollments = mongoTemplate.find(new Query(), Document.class, "enrollments");
        List<Document> teachers = mongoTemplate.find(new Query(), Document.class, "teachers");

        Map<String, String> teacherNames = new HashMap<>();
        for (Document t : teachers) {
            ObjectId tid = t.getObjectId("_id");
            if (tid != null) teacherNames.put(tid.toHexString(), t.getString("fullName"));
        }

        Map<String, Long> classEnrollmentCounts = new HashMap<>();
        for (Document e : enrollments) {
            ObjectId cid = e.getObjectId("classId");
            if (cid != null) {
                classEnrollmentCounts.put(cid.toHexString(), classEnrollmentCounts.getOrDefault(cid.toHexString(), 0L) + 1);
            }
        }

        Document activeSemester = mongoTemplate.findOne(new Query(Criteria.where("status").is("ACTIVE")), Document.class, "semesters");
        if (activeSemester == null) {
            activeSemester = mongoTemplate.findOne(new Query(), Document.class, "semesters");
        }
        String activeYear = (activeSemester != null && activeSemester.getString("academicYear") != null)
                ? activeSemester.getString("academicYear")
                : "2025–26";
        String activeSemName = (activeSemester != null && activeSemester.getString("name") != null)
                ? activeSemester.getString("name")
                : "Fall Semester 2026";

        List<Map<String, Object>> result = new ArrayList<>();
        for (Document c : classes) {
            ObjectId cid = c.getObjectId("_id");
            String cidHex = cid != null ? cid.toHexString() : "";
            ObjectId tid = c.getObjectId("teacherId");
            String teacherName = (tid != null && teacherNames.containsKey(tid.toHexString()))
                    ? teacherNames.get(tid.toHexString())
                    : "Unassigned";

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("classId", cidHex);
            map.put("name", c.getString("name"));
            map.put("courseCode", c.getString("courseCode"));
            map.put("courseName", c.getString("courseName"));
            map.put("section", c.getString("section"));
            map.put("year", activeYear);
            map.put("semester", activeSemName);
            map.put("department", "Computer Science");
            map.put("students", classEnrollmentCounts.getOrDefault(cidHex, 0L));
            map.put("advisor", teacherName);
            map.put("status", "Active");

            result.add(map);
        }

        return ResponseEntity.ok(Map.of("classes", result));
    }

    @PostMapping("/classes")
    public ResponseEntity<?> createClass(
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        String courseCode = (String) payload.get("courseCode");
        String section = (String) payload.get("section");
        String teacherIdStr = (String) payload.get("teacherId");

        if (courseCode == null || courseCode.trim().isEmpty() || section == null || section.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Course code and section are required"));
        }

        Document course = mongoTemplate.findOne(
                new Query(Criteria.where("code").is(courseCode.trim().toUpperCase(Locale.ROOT))),
                Document.class,
                "courses"
        );
        if (course == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Course not found with code: " + courseCode));
        }

        Document semester = mongoTemplate.findOne(new Query(), Document.class, "semesters");
        ObjectId semId = semester != null ? semester.getObjectId("_id") : new ObjectId();

        ObjectId teacherId = (teacherIdStr != null && ObjectId.isValid(teacherIdStr))
                ? new ObjectId(teacherIdStr)
                : null;

        String className = course.getString("name") + " - Section " + section.trim().toUpperCase(Locale.ROOT);

        Document classDoc = new Document()
                .append("name", className)
                .append("courseId", course.getObjectId("_id"))
                .append("courseCode", course.getString("code"))
                .append("courseName", course.getString("name"))
                .append("semesterId", semId)
                .append("section", section.trim().toUpperCase(Locale.ROOT))
                .append("teacherId", teacherId)
                .append("createdAt", new Date())
                .append("demoSeed", "sac-demo-v1");

        mongoTemplate.insert(classDoc, "classes");
        ObjectId classId = classDoc.getObjectId("_id");

        if (teacherId != null) {
            Document assignDoc = new Document()
                    .append("teacherId", teacherId)
                    .append("classId", classId)
                    .append("courseId", course.getObjectId("_id"))
                    .append("role", "PRIMARY_INSTRUCTOR")
                    .append("createdAt", new Date())
                    .append("demoSeed", "sac-demo-v1");
            mongoTemplate.insert(assignDoc, "teaching_assignments");
        }

        // Initialize standard 16 assessments for this new class
        initializeAssessmentsForClass(classId, course.getObjectId("_id"), course.getString("code"));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "classId", classId.toHexString(),
                "name", className
        ));
    }

    private void initializeAssessmentsForClass(ObjectId classId, ObjectId courseId, String courseCode) {
        List<Document> assessments = new ArrayList<>();
        // CAT 1 & 2
        assessments.add(createAssessmentDoc("Continuous Assessment Test 1", "CAT", 50.0, classId, courseId, courseCode));
        assessments.add(createAssessmentDoc("Continuous Assessment Test 2", "CAT", 50.0, classId, courseId, courseCode));
        // Quizzes 1-3
        assessments.add(createAssessmentDoc("Quiz 1", "QUIZ", 10.0, classId, courseId, courseCode));
        assessments.add(createAssessmentDoc("Quiz 2", "QUIZ", 10.0, classId, courseId, courseCode));
        assessments.add(createAssessmentDoc("Quiz 3", "QUIZ", 10.0, classId, courseId, courseCode));
        // Lab 1-10
        for (int i = 1; i <= 10; i++) {
            assessments.add(createAssessmentDoc("Lab Assessment " + i, "LAB", 10.0, classId, courseId, courseCode));
        }
        // FAT
        assessments.add(createAssessmentDoc("Final Assessment Test", "FAT", 100.0, classId, courseId, courseCode));

        mongoTemplate.insert(assessments, "assessments");
    }

    private Document createAssessmentDoc(String name, String type, double maxMarks, ObjectId classId, ObjectId courseId, String courseCode) {
        return new Document()
                .append("name", name)
                .append("type", type)
                .append("classId", classId)
                .append("courseId", courseId)
                .append("courseCode", courseCode)
                .append("maxMarks", maxMarks)
                .append("weightage", type.equals("CAT") ? 15.0 : type.equals("QUIZ") ? 10.0 : type.equals("FAT") ? 40.0 : 2.5)
                .append("status", "ACTIVE")
                .append("classAverage", null) // initially unentered
                .append("createdAt", new Date())
                .append("demoSeed", "sac-demo-v1");
    }

    @GetMapping("/students/eligible")
    public ResponseEntity<?> getEligibleStudents(Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        List<Document> students = mongoTemplate.find(new Query(), Document.class, "students");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document s : students) {
            ObjectId sid = s.getObjectId("_id");
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("studentId", sid != null ? sid.toHexString() : "");
            map.put("studentNumber", s.getString("studentNumber"));
            map.put("fullName", s.getString("fullName"));
            result.add(map);
        }
        return ResponseEntity.ok(Map.of("students", result));
    }

    @PostMapping("/enrollments")
    public ResponseEntity<?> enrollStudent(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        String studentIdStr = payload.get("studentId");
        String classIdStr = payload.get("classId");

        if (studentIdStr == null || !ObjectId.isValid(studentIdStr) || classIdStr == null || !ObjectId.isValid(classIdStr)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid studentId and classId are required"));
        }

        ObjectId studentId = new ObjectId(studentIdStr);
        ObjectId classId = new ObjectId(classIdStr);

        Document student = mongoTemplate.findById(studentId, Document.class, "students");
        if (student == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Student not found"));
        }

        Document cls = mongoTemplate.findById(classId, Document.class, "classes");
        if (cls == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Class not found"));
        }

        ObjectId courseId = cls.getObjectId("courseId");
        String courseCode = cls.getString("courseCode");

        // Check if student is already enrolled in this class
        boolean alreadyEnrolled = mongoTemplate.exists(
                new Query(Criteria.where("studentId").is(studentId).and("classId").is(classId)),
                "enrollments"
        );
        if (alreadyEnrolled) {
            return ResponseEntity.badRequest().body(Map.of("error", "Student is already enrolled in this class"));
        }

        Document semester = mongoTemplate.findOne(new Query(), Document.class, "semesters");
        ObjectId semId = semester != null ? semester.getObjectId("_id") : new ObjectId();

        Document enrollDoc = new Document()
                .append("studentId", studentId)
                .append("userId", student.getObjectId("userId"))
                .append("classId", classId)
                .append("courseId", courseId)
                .append("courseCode", courseCode)
                .append("semesterId", semId)
                .append("enrollmentDate", new Date())
                .append("status", "ENROLLED")
                .append("demoSeed", "sac-demo-v1");

        mongoTemplate.insert(enrollDoc, "enrollments");

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "enrollmentId", enrollDoc.getObjectId("_id").toHexString(),
                "message", "Student enrolled successfully"
        ));
    }

    @PutMapping("/students/{studentId}/cgpa")
    public ResponseEntity<?> updateStudentCgpa(
            @PathVariable("studentId") String studentIdStr,
            @RequestBody(required = false) Map<String, Object> payload,
            Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: admin role required"));
        }

        if (studentIdStr == null || studentIdStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Student identifier is required"));
        }
        String cleanId = studentIdStr.trim();

        Document studentDoc = null;
        if (ObjectId.isValid(cleanId)) {
            ObjectId objId = new ObjectId(cleanId);
            studentDoc = mongoTemplate.findOne(
                    new Query(new Criteria().orOperator(
                            Criteria.where("_id").is(objId),
                            Criteria.where("userId").is(objId)
                    )),
                    Document.class,
                    "students"
            );
        }
        if (studentDoc == null) {
            studentDoc = mongoTemplate.findOne(
                    new Query(Criteria.where("studentNumber").is(cleanId)),
                    Document.class,
                    "students"
            );
        }

        if (studentDoc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Student record not found"));
        }

        Double finalCgpa = null;
        if (payload != null && payload.containsKey("previousCgpa")) {
            Object rawVal = payload.get("previousCgpa");
            if (rawVal != null && !rawVal.toString().trim().isEmpty()) {
                String strVal = rawVal.toString().trim();
                BigDecimal bd;
                try {
                    bd = new BigDecimal(strVal);
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Previous CGPA must be a valid numeric value between 0.00 and 10.00"));
                }

                if (bd.compareTo(BigDecimal.ZERO) < 0 || bd.compareTo(BigDecimal.valueOf(10.0)) > 0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Previous CGPA must be between 0.00 and 10.00"));
                }

                BigDecimal stripped = bd.stripTrailingZeros();
                if (stripped.scale() > 2) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Previous CGPA cannot have more than two decimal places"));
                }

                finalCgpa = bd.setScale(2, RoundingMode.HALF_UP).doubleValue();
            }
        }

        Date now = new Date();
        Update update = new Update()
                .set("previousCgpa", finalCgpa)
                .set("previousCgpaUpdatedAt", now)
                .set("previousCgpaUpdatedBy", admin.getEmail());

        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(studentDoc.getObjectId("_id"))),
                update,
                "students"
        );

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("studentId", studentDoc.getObjectId("_id").toHexString());
        resp.put("studentNumber", studentDoc.getString("studentNumber"));
        resp.put("previousCgpa", finalCgpa);
        resp.put("previousCgpaUpdatedAt", now.toInstant().toString());
        resp.put("previousCgpaUpdatedBy", admin.getEmail());
        resp.put("message", finalCgpa != null
                ? "Previous CGPA updated successfully"
                : "Previous CGPA cleared");

        return ResponseEntity.ok(resp);
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
