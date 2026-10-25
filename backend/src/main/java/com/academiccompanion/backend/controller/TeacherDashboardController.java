package com.academiccompanion.backend.controller;

import com.academiccompanion.backend.model.Assessment;
import com.academiccompanion.backend.model.Teacher;
import com.academiccompanion.backend.model.User;
import com.academiccompanion.backend.repository.AssessmentRepository;
import com.academiccompanion.backend.repository.TeacherRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/teacher")
public class TeacherDashboardController {

    private final TeacherRepository teacherRepository;
    private final AssessmentRepository assessmentRepository;
    private final MongoTemplate mongoTemplate;

    public TeacherDashboardController(
            TeacherRepository teacherRepository,
            AssessmentRepository assessmentRepository,
            MongoTemplate mongoTemplate) {
        this.teacherRepository = teacherRepository;
        this.assessmentRepository = assessmentRepository;
        this.mongoTemplate = mongoTemplate;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }

    private Teacher resolveTeacher(User user) {
        if (user == null || user.getId() == null || !ObjectId.isValid(user.getId())) {
            return null;
        }
        return teacherRepository.findByUserId(new ObjectId(user.getId())).orElse(null);
    }

    private Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isTeacherAssignedToClass(User user, ObjectId classId) {
        if (user.getRole() == User.Role.ADMIN) {
            return true;
        }
        if (user.getRole() != User.Role.TEACHER) {
            return false;
        }
        Teacher teacher = resolveTeacher(user);
        if (teacher == null || teacher.getId() == null || !ObjectId.isValid(teacher.getId())) {
            return false;
        }

        ObjectId teacherObjectId = new ObjectId(teacher.getId());

        boolean assigned = mongoTemplate.exists(
                new Query(new Criteria().orOperator(
                        Criteria.where("_id").is(classId).and("teacherId").is(teacherObjectId),
                        Criteria.where("classId").is(classId).and("teacherId").is(teacherObjectId)
                )),
                "classes"
        );

        if (!assigned) {
            assigned = mongoTemplate.exists(
                    new Query(Criteria.where("classId").is(classId).and("teacherId").is(teacherObjectId)),
                    "teaching_assignments"
            );
        }

        return assigned;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: teacher role required"));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "CONNECTED");
        response.put("userId", user.getId());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());

        Teacher teacher = resolveTeacher(user);
        if (teacher != null) {
            response.put("teacherId", teacher.getId());
            response.put("employeeNumber", teacher.getEmployeeNumber());
            response.put("department", teacher.getDepartment());
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
        response.put("academicYear", activeYear);
        response.put("semester", activeSemName);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/classes")
    public ResponseEntity<?> getAssignedClasses(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: teacher role required"));
        }

        Teacher teacher = resolveTeacher(user);
        boolean isAdmin = user.getRole() == User.Role.ADMIN;

        Query classQuery = new Query();
        if (!isAdmin) {
            if (teacher == null || teacher.getId() == null || !ObjectId.isValid(teacher.getId())) {
                return ResponseEntity.ok(Map.of("classes", List.of()));
            }
            ObjectId teacherObjectId = new ObjectId(teacher.getId());

            List<Document> assignments = mongoTemplate.find(
                    new Query(Criteria.where("teacherId").is(teacherObjectId)),
                    Document.class,
                    "teaching_assignments"
            );

            Set<ObjectId> classIds = new HashSet<>();
            for (Document a : assignments) {
                ObjectId cid = a.getObjectId("classId");
                if (cid != null) classIds.add(cid);
            }

            classQuery.addCriteria(new Criteria().orOperator(
                    Criteria.where("teacherId").is(teacherObjectId),
                    Criteria.where("_id").in(classIds)
            ));
        }

        List<Document> classDocs = mongoTemplate.find(classQuery, Document.class, "classes");
        List<Map<String, Object>> resultClasses = new ArrayList<>();

        for (Document c : classDocs) {
            ObjectId classId = c.getObjectId("_id");
            Map<String, Object> cMap = new LinkedHashMap<>();
            cMap.put("classId", classId != null ? classId.toHexString() : null);
            cMap.put("className", c.getString("name"));
            cMap.put("courseCode", c.getString("courseCode"));
            cMap.put("courseName", c.getString("courseName"));
            cMap.put("section", c.getString("section"));

            long enrolledCount = (classId != null)
                    ? mongoTemplate.count(new Query(Criteria.where("classId").is(classId)), "enrollments")
                    : 0;
            cMap.put("enrolledCount", enrolledCount);

            // Fetch assessments for this class
            List<Assessment> assessments = classId != null
                    ? assessmentRepository.findByClassId(classId)
                    : List.of();

            List<Map<String, Object>> assessList = new ArrayList<>();
            for (Assessment a : assessments) {
                Map<String, Object> aMap = new LinkedHashMap<>();
                aMap.put("id", a.getId());
                aMap.put("name", a.getName());
                aMap.put("type", a.getType());
                aMap.put("maxMarks", a.getMaxMarks());

                double rawMax = getRawMaxScale(a.getName(), a.getType(), a.getMaxMarks());
                aMap.put("rawMaxMarks", rawMax);
                aMap.put("classAverage", a.getClassAverage());
                aMap.put("status", a.getStatus());
                assessList.add(aMap);
            }

            cMap.put("assessments", assessList);
            resultClasses.add(cMap);
        }

        return ResponseEntity.ok(Map.of("classes", resultClasses));
    }

    @GetMapping("/classes/{classId}/students")
    public ResponseEntity<?> getClassStudents(
            @PathVariable("classId") String classIdStr,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        if (!ObjectId.isValid(classIdStr)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid classId"));
        }
        ObjectId classId = new ObjectId(classIdStr);
        if (!isTeacherAssignedToClass(user, classId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: You do not teach this class"));
        }

        Document classDoc = mongoTemplate.findById(classId, Document.class, "classes");
        if (classDoc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Class not found"));
        }

        List<Document> enrollments = mongoTemplate.find(
                new Query(Criteria.where("classId").is(classId)),
                Document.class,
                "enrollments"
        );

        Set<ObjectId> studentIds = new HashSet<>();
        Map<String, Document> enrollmentByStudentId = new HashMap<>();
        for (Document e : enrollments) {
            ObjectId sid = e.getObjectId("studentId");
            if (sid != null) {
                studentIds.add(sid);
                enrollmentByStudentId.put(sid.toHexString(), e);
            }
        }

        List<Document> students = mongoTemplate.find(
                new Query(Criteria.where("_id").in(studentIds)),
                Document.class,
                "students"
        );

        Set<ObjectId> userIds = new HashSet<>();
        for (Document s : students) {
            ObjectId uid = s.getObjectId("userId");
            if (uid != null) userIds.add(uid);
        }

        Map<String, String> emailByUserId = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<Document> users = mongoTemplate.find(
                    new Query(Criteria.where("_id").in(userIds)),
                    Document.class,
                    "users"
            );
            for (Document u : users) {
                ObjectId uid = u.getObjectId("_id");
                if (uid != null) emailByUserId.put(uid.toHexString(), u.getString("email"));
            }
        }

        // Fetch attendance records for this course/class
        ObjectId courseId = classDoc.getObjectId("courseId");
        List<Document> attendanceRecords = mongoTemplate.find(
                new Query(Criteria.where("studentId").in(studentIds).and("courseId").is(courseId)),
                Document.class,
                "attendance"
        );
        Map<String, Double> attendanceByStudentId = new HashMap<>();
        for (Document a : attendanceRecords) {
            ObjectId sid = a.getObjectId("studentId");
            if (sid != null) {
                attendanceByStudentId.put(sid.toHexString(), toDouble(a.get("attendancePercentage")));
            }
        }

        students.sort(Comparator.comparing(s -> s.getString("studentNumber")));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Document s : students) {
            ObjectId sid = s.getObjectId("_id");
            String sidHex = sid != null ? sid.toHexString() : "";
            ObjectId uid = s.getObjectId("userId");
            String uidHex = uid != null ? uid.toHexString() : "";

            Document enroll = enrollmentByStudentId.get(sidHex);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("studentId", sidHex);
            map.put("studentNumber", s.getString("studentNumber"));
            map.put("fullName", s.getString("fullName"));
            map.put("email", emailByUserId.getOrDefault(uidHex, ""));
            map.put("section", classDoc.getString("section"));
            map.put("attendancePercentage", attendanceByStudentId.get(sidHex));
            map.put("previousCgpa", toDouble(s.get("previousCgpa")));
            map.put("enrollmentId", enroll != null && enroll.getObjectId("_id") != null
                    ? enroll.getObjectId("_id").toHexString() : null);

            result.add(map);
        }

        return ResponseEntity.ok(Map.of("students", result));
    }

    @GetMapping("/classes/{classId}/assessments")
    public ResponseEntity<?> getClassAssessments(
            @PathVariable("classId") String classIdStr,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        if (!ObjectId.isValid(classIdStr)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid classId"));
        }
        ObjectId classId = new ObjectId(classIdStr);
        if (!isTeacherAssignedToClass(user, classId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: You do not teach this class"));
        }

        List<Document> enrollments = mongoTemplate.find(
                new Query(Criteria.where("classId").is(classId)),
                Document.class,
                "enrollments"
        );
        Set<ObjectId> studentIds = new HashSet<>();
        for (Document e : enrollments) {
            ObjectId sid = e.getObjectId("studentId");
            if (sid != null) studentIds.add(sid);
        }
        long totalEnrolled = studentIds.size();

        List<Assessment> assessments = assessmentRepository.findByClassId(classId);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Assessment a : assessments) {
            ObjectId aid = a.getId() != null && ObjectId.isValid(a.getId()) ? new ObjectId(a.getId()) : null;
            double rawMax = getRawMaxScale(a.getName(), a.getType(), a.getMaxMarks());

            // Compute dynamic calculated average from persisted valid marks for enrolled students
            Double calculatedAvg = null;
            int validSubmissionsCount = 0;
            if (aid != null && !studentIds.isEmpty()) {
                Criteria crit = new Criteria().orOperator(
                        Criteria.where("assessmentId").is(aid).and("studentId").in(studentIds),
                        Criteria.where("assessmentId").is(aid).and("classId").is(classId)
                );
                List<Document> completedResults = mongoTemplate.find(
                        new Query(crit),
                        Document.class,
                        "results"
                );
                double sum = 0.0;
                for (Document r : completedResults) {
                    String status = r.getString("status");
                    if ("PENDING".equalsIgnoreCase(status)) continue;

                    Double marks = toDouble(r.get("obtainedMarks"));
                    if (marks == null) marks = toDouble(r.get("marksObtained"));

                    if (marks != null && marks >= 0.0 && marks <= rawMax) {
                        sum += marks;
                        validSubmissionsCount++;
                    }
                }
                if (validSubmissionsCount > 0) {
                    calculatedAvg = Math.round((sum / validSubmissionsCount) * 10.0) / 10.0;
                }
            }

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("name", a.getName());
            map.put("type", a.getType());
            map.put("maxMarks", a.getMaxMarks());
            map.put("rawMaxMarks", rawMax);
            map.put("officialClassAverage", a.getClassAverage()); // Teacher entered
            map.put("calculatedAverage", calculatedAvg); // Dynamic average from student marks
            map.put("submissionsCount", validSubmissionsCount);
            map.put("enrolledCount", totalEnrolled);
            map.put("totalStudents", totalEnrolled);
            map.put("status", a.getStatus());

            list.add(map);
        }

        Map<String, Object> cumulativeCourseAverageMap = computeClassCumulativeAverage(classId, studentIds, assessments);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("assessments", list);
        resp.put("cumulativeCourseAverage", cumulativeCourseAverageMap);

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/classes/{classId}/marks")
    public ResponseEntity<?> getClassMarks(
            @PathVariable("classId") String classIdStr,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        if (!ObjectId.isValid(classIdStr)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid classId"));
        }
        ObjectId classId = new ObjectId(classIdStr);
        if (!isTeacherAssignedToClass(user, classId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: You do not teach this class"));
        }

        List<Document> enrollments = mongoTemplate.find(
                new Query(Criteria.where("classId").is(classId)),
                Document.class,
                "enrollments"
        );

        Set<ObjectId> studentIds = new HashSet<>();
        Set<ObjectId> enrollmentIds = new HashSet<>();
        for (Document e : enrollments) {
            ObjectId sid = e.getObjectId("studentId");
            if (sid != null) studentIds.add(sid);
            ObjectId eid = e.getObjectId("_id");
            if (eid != null) enrollmentIds.add(eid);
        }

        List<Document> students = mongoTemplate.find(
                new Query(Criteria.where("_id").in(studentIds)),
                Document.class,
                "students"
        );
        students.sort(Comparator.comparing(s -> s.getString("studentNumber")));

        List<Assessment> assessments = assessmentRepository.findByClassId(classId);

        // Prepare sets of keys to match ObjectId and String representations
        Set<Object> assessmentKeys = new HashSet<>();
        Map<String, Double> rawMaxByAssessment = new HashMap<>();
        for (Assessment a : assessments) {
            if (a.getId() != null) {
                assessmentKeys.add(a.getId());
                if (ObjectId.isValid(a.getId())) {
                    assessmentKeys.add(new ObjectId(a.getId()));
                }
                rawMaxByAssessment.put(a.getId(), getRawMaxScale(a.getName(), a.getType(), a.getMaxMarks()));
            }
        }

        Set<Object> studentKeys = new HashSet<>();
        for (ObjectId sid : studentIds) {
            studentKeys.add(sid);
            studentKeys.add(sid.toHexString());
        }

        Set<Object> enrollmentKeys = new HashSet<>();
        for (ObjectId eid : enrollmentIds) {
            enrollmentKeys.add(eid);
            enrollmentKeys.add(eid.toHexString());
        }

        // Query results for this class
        List<Document> results = new ArrayList<>();
        if (!assessmentKeys.isEmpty() && (!studentKeys.isEmpty() || !enrollmentKeys.isEmpty())) {
            Criteria criteria = new Criteria().orOperator(
                    Criteria.where("assessmentId").in(assessmentKeys).and("studentId").in(studentKeys),
                    Criteria.where("assessmentId").in(assessmentKeys).and("enrollmentId").in(enrollmentKeys),
                    Criteria.where("classId").is(classId),
                    Criteria.where("classId").is(classIdStr)
            );
            results = mongoTemplate.find(new Query(criteria), Document.class, "results");
        }

        // Map marks by studentIdHex -> assessmentIdHex -> obtainedMarks
        Map<String, Map<String, Double>> marksMap = new HashMap<>();
        Map<String, List<Double>> marksByAssessment = new HashMap<>();

        for (Document r : results) {
            Object sidObj = r.get("studentId");
            String sidHex = sidObj instanceof ObjectId oid ? oid.toHexString() : (sidObj != null ? sidObj.toString() : null);

            Object aidObj = r.get("assessmentId");
            String aidHex = aidObj instanceof ObjectId oid ? oid.toHexString() : (aidObj != null ? aidObj.toString() : null);

            String status = r.getString("status");
            if (sidHex != null && aidHex != null && !"PENDING".equalsIgnoreCase(status)) {
                Double marks = toDouble(r.get("obtainedMarks"));
                if (marks == null) marks = toDouble(r.get("marksObtained"));

                Double rawMax = rawMaxByAssessment.get(aidHex);
                double maxScale = rawMax != null ? rawMax : 100.0;

                if (marks != null && marks >= 0.0 && marks <= maxScale) {
                    marksMap.computeIfAbsent(sidHex, k -> new HashMap<>())
                            .put(aidHex, marks);
                    marksByAssessment.computeIfAbsent(aidHex, k -> new ArrayList<>())
                            .add(marks);
                }
            }
        }

        // Build assessments response with calculated averages
        List<Map<String, Object>> assessList = new ArrayList<>();
        for (Assessment a : assessments) {
            String aidHex = a.getId();
            List<Double> marksList = marksByAssessment.getOrDefault(aidHex, List.of());
            Double calculatedAvg = null;
            if (!marksList.isEmpty()) {
                double sum = 0.0;
                for (Double m : marksList) sum += m;
                calculatedAvg = Math.round((sum / marksList.size()) * 10.0) / 10.0;
            }

            Map<String, Object> aMap = new LinkedHashMap<>();
            aMap.put("id", a.getId());
            aMap.put("name", a.getName());
            aMap.put("type", a.getType());
            aMap.put("maxMarks", a.getMaxMarks());
            aMap.put("rawMaxMarks", getRawMaxScale(a.getName(), a.getType(), a.getMaxMarks()));
            aMap.put("officialClassAverage", a.getClassAverage());
            aMap.put("calculatedAverage", calculatedAvg);
            aMap.put("submissionsCount", marksList.size());
            aMap.put("enrolledCount", students.size());
            aMap.put("totalStudents", students.size());
            assessList.add(aMap);
        }

        // Determine whether class course is embedded
        Document classDoc = mongoTemplate.findById(classId, Document.class, "classes");
        String courseCode = classDoc != null ? classDoc.getString("courseCode") : null;
        boolean isEmbedded = true;
        if (courseCode != null) {
            Document cDoc = mongoTemplate.findOne(new Query(Criteria.where("code").is(courseCode)), Document.class, "courses");
            if (cDoc != null && cDoc.getString("evaluationType") != null) {
                String et = cDoc.getString("evaluationType").toUpperCase(Locale.ROOT);
                isEmbedded = (et.contains("LAB") && et.contains("THEORY")) || et.equals("EMBEDDED");
            }
        }

        List<Double> cumulativeScores = new ArrayList<>();
        // Build students response
        List<Map<String, Object>> studentList = new ArrayList<>();
        for (Document s : students) {
            ObjectId sid = s.getObjectId("_id");
            String sidHex = sid != null ? sid.toHexString() : "";

            Map<String, Double> sMarks = marksMap.getOrDefault(sidHex, Collections.emptyMap());
            Double score = calculateStudentCourseScore(sMarks, assessments, isEmbedded);
            if (score != null) {
                cumulativeScores.add(score);
            }

            Map<String, Object> sMap = new LinkedHashMap<>();
            sMap.put("studentId", sidHex);
            sMap.put("studentNumber", s.getString("studentNumber"));
            sMap.put("fullName", s.getString("fullName"));
            sMap.put("marks", sMarks);
            sMap.put("cumulativeScore", score);
            sMap.put("completedAssessmentsCount", sMarks.size());
            studentList.add(sMap);
        }

        Double classCumulativeAverage = null;
        if (!cumulativeScores.isEmpty()) {
            double cSum = 0.0;
            for (Double d : cumulativeScores) cSum += d;
            classCumulativeAverage = Math.round((cSum / cumulativeScores.size()) * 10.0) / 10.0;
        }

        Map<String, Object> cumulativeCourseAverageMap = new LinkedHashMap<>();
        cumulativeCourseAverageMap.put("calculatedAverage", classCumulativeAverage);
        cumulativeCourseAverageMap.put("gradedStudentsCount", cumulativeScores.size());
        cumulativeCourseAverageMap.put("totalEnrolled", students.size());
        cumulativeCourseAverageMap.put("formula", "Weighted score according to official scheme (CAT 1: 15%, CAT 2: 15%, Quizzes: 10% each, FAT: 40%, Labs: 25% embedded) normalized over completed assessments");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("classId", classIdStr);
        response.put("cumulativeCourseAverage", cumulativeCourseAverageMap);
        response.put("assessments", assessList);
        response.put("students", studentList);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/classes/{classId}/marks")
    public ResponseEntity<?> updateClassMarks(
            @PathVariable("classId") String classIdStr,
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        if (!ObjectId.isValid(classIdStr)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid classId"));
        }
        ObjectId classId = new ObjectId(classIdStr);
        if (!isTeacherAssignedToClass(user, classId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: You do not teach this class"));
        }

        Document classDoc = mongoTemplate.findById(classId, Document.class, "classes");
        if (classDoc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Class not found"));
        }

        // Support either single entry: { studentId, assessmentId, marks }
        // or batch entries: { entries: [ { studentId, assessmentId, marks } ] }
        List<Map<String, Object>> entries = new ArrayList<>();
        if (payload.containsKey("entries") && payload.get("entries") instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> casted = (Map<String, Object>) m;
                    entries.add(casted);
                }
            }
        } else if (payload.containsKey("studentId") && payload.containsKey("assessmentId")) {
            entries.add(payload);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "studentId and assessmentId are required"));
        }

        int updatedCount = 0;
        for (Map<String, Object> entry : entries) {
            String studentIdStr = (String) entry.get("studentId");
            String assessmentIdStr = (String) entry.get("assessmentId");
            Object marksObj = entry.get("marks");

            if (studentIdStr == null || !ObjectId.isValid(studentIdStr) ||
                    assessmentIdStr == null || !ObjectId.isValid(assessmentIdStr)) {
                continue;
            }

            ObjectId studentId = new ObjectId(studentIdStr);
            ObjectId assessmentId = new ObjectId(assessmentIdStr);

            Assessment assessment = assessmentRepository.findById(assessmentIdStr).orElse(null);
            if (assessment == null) continue;

            double rawMax = getRawMaxScale(assessment.getName(), assessment.getType(), assessment.getMaxMarks());

            Double obtainedMarks = null;
            if (marksObj != null && !marksObj.toString().trim().isEmpty()) {
                try {
                    obtainedMarks = Double.parseDouble(marksObj.toString().trim());
                    if (obtainedMarks < 0.0 || obtainedMarks > rawMax) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", String.format(Locale.ROOT, "Marks for %s must be between 0 and %.1f", assessment.getName(), rawMax)
                        ));
                    }
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Marks must be numeric"));
                }
            }

            // Find enrollment
            Document enrollment = mongoTemplate.findOne(
                    new Query(Criteria.where("studentId").is(studentId).and("classId").is(classId)),
                    Document.class,
                    "enrollments"
            );
            if (enrollment == null) continue;

            ObjectId enrollmentId = enrollment.getObjectId("_id");
            ObjectId userId = enrollment.getObjectId("userId");

            // Upsert in results collection
            Query resultQuery = new Query(new Criteria().orOperator(
                    Criteria.where("enrollmentId").is(enrollmentId).and("assessmentId").is(assessmentId),
                    Criteria.where("studentId").is(studentId).and("assessmentId").is(assessmentId)
            ));
            Document existingResult = mongoTemplate.findOne(resultQuery, Document.class, "results");

            if (obtainedMarks == null) {
                // If marks is null, mark as PENDING
                if (existingResult != null) {
                    Update update = new Update()
                            .set("obtainedMarks", null)
                            .set("marksObtained", null)
                            .set("status", "PENDING")
                            .set("classId", classId)
                            .set("updatedAt", new Date());
                    mongoTemplate.updateFirst(resultQuery, update, "results");
                    updatedCount++;
                }
            } else {
                if (existingResult != null) {
                    Update update = new Update()
                            .set("obtainedMarks", obtainedMarks)
                            .set("marksObtained", obtainedMarks)
                            .set("status", "COMPLETED")
                            .set("classId", classId)
                            .set("updatedAt", new Date());
                    mongoTemplate.updateFirst(resultQuery, update, "results");
                } else {
                    Document newResult = new Document()
                            .append("studentId", studentId)
                            .append("userId", userId)
                            .append("enrollmentId", enrollmentId)
                            .append("classId", classId)
                            .append("courseId", classDoc.getObjectId("courseId"))
                            .append("courseCode", classDoc.getString("courseCode"))
                            .append("assessmentId", assessmentId)
                            .append("assessmentName", assessment.getName())
                            .append("assessmentType", assessment.getType())
                            .append("maxMarks", assessment.getMaxMarks())
                            .append("obtainedMarks", obtainedMarks)
                            .append("marksObtained", obtainedMarks)
                            .append("status", "COMPLETED")
                            .append("createdAt", new Date())
                            .append("demoSeed", "sac-demo-v1");
                    mongoTemplate.insert(newResult, "results");
                }
                updatedCount++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "updatedCount", updatedCount,
                "message", "Marks updated successfully"
        ));
    }

    @PutMapping("/assessments/{assessmentId}/class-average")
    public ResponseEntity<?> updateClassAverage(
            @PathVariable("assessmentId") String assessmentId,
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        if (user.getRole() != User.Role.TEACHER && user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied: teacher role required"));
        }

        Optional<Assessment> optAssess = assessmentRepository.findById(assessmentId);
        if (optAssess.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Assessment not found with id: " + assessmentId));
        }

        Assessment assess = optAssess.get();
        boolean isAdmin = user.getRole() == User.Role.ADMIN;

        // Verify teacher assignment to this assessment's class
        if (!isAdmin) {
            Teacher teacher = resolveTeacher(user);
            if (teacher == null || teacher.getId() == null || !ObjectId.isValid(teacher.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Teacher record not found for your account"));
            }

            ObjectId teacherObjectId = new ObjectId(teacher.getId());
            ObjectId classId = assess.getClassId();

            if (classId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Assessment is not linked to any class offering"));
            }

            // Verify teacher teaches this class
            boolean assigned = mongoTemplate.exists(
                    new Query(new Criteria().orOperator(
                            Criteria.where("_id").is(classId).and("teacherId").is(teacherObjectId),
                            Criteria.where("classId").is(classId).and("teacherId").is(teacherObjectId)
                    )),
                    "classes"
            );

            if (!assigned) {
                assigned = mongoTemplate.exists(
                        new Query(Criteria.where("classId").is(classId).and("teacherId").is(teacherObjectId)),
                        "teaching_assignments"
                );
            }

            if (!assigned) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You are not assigned to teach this class"));
            }
        }

        // Validate classAverage value
        Object avgObj = payload.get("classAverage");
        Double newAverage = null;
        if (avgObj != null && !avgObj.toString().trim().isEmpty()) {
            try {
                newAverage = Double.parseDouble(avgObj.toString().trim());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Class average must be a valid numeric value"));
            }

            double rawMax = getRawMaxScale(assess.getName(), assess.getType(), assess.getMaxMarks());
            if (newAverage < 0.0 || newAverage > rawMax) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", String.format(
                                Locale.ROOT,
                                "Class average must be between 0.0 and %.1f for assessment '%s'",
                                rawMax,
                                assess.getName()
                        )));
            }
        }

        assess.setClassAverage(newAverage);
        assessmentRepository.save(assess);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("assessmentId", assessmentId);
        response.put("assessmentName", assess.getName());
        response.put("classAverage", newAverage);
        response.put("message", newAverage != null
                ? "Class average updated to " + newAverage
                : "Class average cleared");

        return ResponseEntity.ok(response);
    }

    private double getRawMaxScale(String name, String type, Double storedMax) {
        String n = (name != null ? name : "").toUpperCase(Locale.ROOT);
        String t = (type != null ? type : "").toUpperCase(Locale.ROOT);

        if (n.contains("CAT 1") || n.equals("CAT1") || (t.equals("CAT") && n.contains("1"))) {
            return 50.0;
        }
        if (n.contains("CAT 2") || n.equals("CAT2") || (t.equals("CAT") && n.contains("2"))) {
            return 50.0;
        }
        if (n.contains("QUIZ 1") || n.equals("QUIZ1") || (t.equals("QUIZ") && n.contains("1"))) {
            return 10.0;
        }
        if (n.contains("QUIZ 2") || n.equals("QUIZ2") || (t.equals("QUIZ") && n.contains("2"))) {
            return 10.0;
        }
        if (n.contains("QUIZ 3") || n.equals("QUIZ3") || (t.equals("QUIZ") && n.contains("3"))) {
            return 10.0;
        }
        if (n.contains("FAT") || t.contains("FAT") || n.contains("FINAL")) {
            return 100.0;
        }
        if (t.equals("LAB") || n.contains("LAB") || t.equals("OTHER")) {
            return 10.0;
        }
        return storedMax != null && storedMax > 0 ? storedMax : 100.0;
    }

    private Double calculateStudentCourseScore(Map<String, Double> studentMarks, List<Assessment> assessments, boolean isEmbedded) {
        if (studentMarks == null || studentMarks.isEmpty()) return null;

        double earnedTheory = 0.0;
        double completedTheoryWeight = 0.0;
        double labSum = 0.0;
        int labCount = 0;

        for (Assessment a : assessments) {
            String aid = a.getId();
            if (aid == null || !studentMarks.containsKey(aid)) continue;
            Double marks = studentMarks.get(aid);
            if (marks == null) continue;

            String name = (a.getName() != null ? a.getName() : "").toUpperCase(Locale.ROOT);
            String type = (a.getType() != null ? a.getType() : "").toUpperCase(Locale.ROOT);

            if (name.contains("CAT 1") || name.equals("CAT1") || (type.equals("CAT") && name.contains("1"))) {
                earnedTheory += (marks / 50.0) * 15.0;
                completedTheoryWeight += 15.0;
            } else if (name.contains("CAT 2") || name.equals("CAT2") || (type.equals("CAT") && name.contains("2"))) {
                earnedTheory += (marks / 50.0) * 15.0;
                completedTheoryWeight += 15.0;
            } else if (name.contains("QUIZ 1") || name.equals("QUIZ1") || (type.equals("QUIZ") && name.contains("1"))) {
                earnedTheory += (marks / 10.0) * 10.0;
                completedTheoryWeight += 10.0;
            } else if (name.contains("QUIZ 2") || name.equals("QUIZ2") || (type.equals("QUIZ") && name.contains("2"))) {
                earnedTheory += (marks / 10.0) * 10.0;
                completedTheoryWeight += 10.0;
            } else if (name.contains("QUIZ 3") || name.equals("QUIZ3") || (type.equals("QUIZ") && name.contains("3"))) {
                earnedTheory += (marks / 10.0) * 10.0;
                completedTheoryWeight += 10.0;
            } else if (name.contains("FAT") || type.contains("FAT") || name.contains("FINAL")) {
                earnedTheory += (marks / 100.0) * 40.0;
                completedTheoryWeight += 40.0;
            } else if (type.equals("LAB") || name.contains("LAB") || type.equals("OTHER")) {
                labSum += marks;
                labCount++;
            }
        }

        Double theoryScore = completedTheoryWeight > 0 ? (earnedTheory / completedTheoryWeight) * 100.0 : null;
        Double labScore = labCount > 0 ? (labSum / (labCount * 10.0)) * 100.0 : null;

        if (theoryScore == null && labScore == null) return null;

        double overall;
        if (isEmbedded && theoryScore != null && labScore != null) {
            overall = theoryScore * 0.75 + labScore * 0.25;
        } else if (theoryScore != null) {
            overall = theoryScore;
        } else {
            overall = labScore != null ? labScore : 0.0;
        }

        return Math.round(overall * 10.0) / 10.0;
    }

    private Map<String, Object> computeClassCumulativeAverage(ObjectId classId, Set<ObjectId> studentIds, List<Assessment> assessments) {
        if (studentIds.isEmpty() || assessments.isEmpty()) {
            Map<String, Object> emptyMap = new LinkedHashMap<>();
            emptyMap.put("calculatedAverage", null);
            emptyMap.put("gradedStudentsCount", 0);
            emptyMap.put("totalEnrolled", studentIds.size());
            emptyMap.put("formula", "Weighted score according to official scheme (CAT 1: 15%, CAT 2: 15%, Quizzes: 10% each, FAT: 40%, Labs: 25% embedded) normalized over completed assessments");
            return emptyMap;
        }

        Document classDoc = mongoTemplate.findById(classId, Document.class, "classes");
        String courseCode = classDoc != null ? classDoc.getString("courseCode") : null;
        boolean isEmbedded = true;
        if (courseCode != null) {
            Document cDoc = mongoTemplate.findOne(new Query(Criteria.where("code").is(courseCode)), Document.class, "courses");
            if (cDoc != null && cDoc.getString("evaluationType") != null) {
                String et = cDoc.getString("evaluationType").toUpperCase(Locale.ROOT);
                isEmbedded = (et.contains("LAB") && et.contains("THEORY")) || et.equals("EMBEDDED");
            }
        }

        Set<String> aidHexes = new HashSet<>();
        Set<Object> aidKeys = new HashSet<>();
        for (Assessment a : assessments) {
            if (a.getId() != null) {
                aidHexes.add(a.getId());
                aidKeys.add(a.getId());
                if (ObjectId.isValid(a.getId())) aidKeys.add(new ObjectId(a.getId()));
            }
        }

        List<Document> results = mongoTemplate.find(
                new Query(new Criteria().orOperator(
                        Criteria.where("assessmentId").in(aidKeys).and("studentId").in(studentIds),
                        Criteria.where("classId").is(classId)
                )),
                Document.class,
                "results"
        );

        Map<String, Map<String, Double>> marksMap = new HashMap<>();
        for (Document r : results) {
            if ("PENDING".equalsIgnoreCase(r.getString("status"))) continue;
            Object sidObj = r.get("studentId");
            String sidHex = sidObj instanceof ObjectId oid ? oid.toHexString() : (sidObj != null ? sidObj.toString() : null);
            Object aidObj = r.get("assessmentId");
            String aidHex = aidObj instanceof ObjectId oid ? oid.toHexString() : (aidObj != null ? aidObj.toString() : null);
            if (sidHex != null && aidHex != null && aidHexes.contains(aidHex)) {
                Double m = toDouble(r.get("obtainedMarks"));
                if (m == null) m = toDouble(r.get("marksObtained"));
                if (m != null) {
                    marksMap.computeIfAbsent(sidHex, k -> new HashMap<>()).put(aidHex, m);
                }
            }
        }

        List<Double> cumulativeScores = new ArrayList<>();
        for (ObjectId sid : studentIds) {
            String sidHex = sid.toHexString();
            Map<String, Double> sMarks = marksMap.getOrDefault(sidHex, Collections.emptyMap());
            Double score = calculateStudentCourseScore(sMarks, assessments, isEmbedded);
            if (score != null) {
                cumulativeScores.add(score);
            }
        }

        Double classCumulativeAverage = null;
        if (!cumulativeScores.isEmpty()) {
            double cSum = 0.0;
            for (Double d : cumulativeScores) cSum += d;
            classCumulativeAverage = Math.round((cSum / cumulativeScores.size()) * 10.0) / 10.0;
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("calculatedAverage", classCumulativeAverage);
        resp.put("gradedStudentsCount", cumulativeScores.size());
        resp.put("totalEnrolled", studentIds.size());
        resp.put("formula", "Weighted score according to official scheme (CAT 1: 15%, CAT 2: 15%, Quizzes: 10% each, FAT: 40%, Labs: 25% embedded) normalized over completed assessments");
        return resp;
    }
}
