package com.academiccompanion.backend.controller;

import com.academiccompanion.backend.dto.CourseLevelAvailabilityDto;
import com.academiccompanion.backend.dto.CoursePredictionDto;
import com.academiccompanion.backend.dto.PredictRequestDto;
import com.academiccompanion.backend.dto.PredictionAvailabilityResponse;
import com.academiccompanion.backend.dto.PredictionResponse;
import com.academiccompanion.backend.model.*;
import com.academiccompanion.backend.repository.*;
import com.academiccompanion.backend.service.MlPredictionService;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/student")
public class StudentDashboardController {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final AssessmentResultRepository resultRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final AssessmentRepository assessmentRepository;
    private final MlPredictionService mlPredictionService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    public StudentDashboardController(
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            AssessmentResultRepository resultRepository,
            AttendanceRecordRepository attendanceRepository,
            AssessmentRepository assessmentRepository,
            MlPredictionService mlPredictionService) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.resultRepository = resultRepository;
        this.attendanceRepository = attendanceRepository;
        this.assessmentRepository = assessmentRepository;
        this.mlPredictionService = mlPredictionService;
    }

    private Student resolveAuthenticatedStudent(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof User user)
                || user.getRole() != User.Role.STUDENT
                || user.getId() == null
                || !ObjectId.isValid(user.getId())) {
            return null;
        }

        return studentRepository.findByUserId(new ObjectId(user.getId())).orElse(null);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication authentication) {
        Student student = resolveAuthenticatedStudent(authentication);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "A valid student login is required"));
        }

        ObjectId studentObjectId = student.getId() != null && ObjectId.isValid(student.getId()) ? new ObjectId(student.getId()) : null;
        ObjectId userObjectId = student.getUserId();

        List<Enrollment> enrollments = findStudentEnrollments(studentObjectId, userObjectId);
        List<AttendanceRecord> attendanceRecords = findStudentAttendance(studentObjectId, userObjectId);

        Double averageAttendance = null;
        if (!attendanceRecords.isEmpty()) {
            double sum = 0.0;
            int count = 0;
            for (AttendanceRecord ar : attendanceRecords) {
                Double pct = ar.getAttendancePercentage();
                if (pct != null) {
                    sum += pct;
                    count++;
                }
            }
            if (count > 0) {
                averageAttendance = Math.round((sum / count) * 10.0) / 10.0;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", student.getId());
        result.put("studentNumber", student.getStudentNumber());
        result.put("fullName", student.getFullName());
        result.put("program", student.getProgram());
        result.put("department", student.getDepartment());
        result.put("yearOfStudy", student.getYearOfStudy());
        result.put("section", student.getSection());
        result.put("totalCourses", enrollments.size());
        result.put("attendancePercentage", averageAttendance);
        result.put("currentSemesterSgpa", null);
        result.put("previousCgpa", student.getPreviousCgpa());
        result.put("academicDataAvailable", !enrollments.isEmpty());

        String activeAcademicYear = "2025–26";
        String activeSemesterName = "Fall Semester 2026";
        if (mongoTemplate != null) {
            org.bson.Document sem = mongoTemplate.findOne(
                    new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("status").is("ACTIVE")
                    ),
                    org.bson.Document.class,
                    "semesters"
            );
            if (sem == null) {
                sem = mongoTemplate.findOne(
                        new org.springframework.data.mongodb.core.query.Query(),
                        org.bson.Document.class,
                        "semesters"
                );
            }
            if (sem != null) {
                if (sem.getString("academicYear") != null) {
                    activeAcademicYear = sem.getString("academicYear");
                }
                if (sem.getString("name") != null) {
                    activeSemesterName = sem.getString("name");
                }
            }
        }
        result.put("academicYear", activeAcademicYear);
        result.put("semester", activeSemesterName);

        return ResponseEntity.ok(result);
    }

    @GetMapping({"/prediction/availability", "/predictions/availability"})
    public ResponseEntity<?> predictionAvailability(Authentication authentication) {
        Student student = resolveAuthenticatedStudent(authentication);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "A valid student login is required"));
        }

        ObjectId studentObjectId = student.getId() != null && ObjectId.isValid(student.getId()) ? new ObjectId(student.getId()) : null;
        ObjectId userObjectId = student.getUserId();

        List<Enrollment> enrollments = findStudentEnrollments(studentObjectId, userObjectId);
        List<AssessmentResult> allResults = findStudentResults(studentObjectId, userObjectId);

        boolean healthy = mlPredictionService.isHealthy();
        Set<String> availableModels = healthy ? mlPredictionService.getAvailableModelLevels() : Collections.emptySet();

        List<CourseLevelAvailabilityDto> courseAvailabilities = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Course course = resolveCourse(enrollment);
            List<AssessmentResult> courseResults = filterResultsForCourse(allResults, enrollment, course);
            courseAvailabilities.add(mlPredictionService.getCourseLevelAvailability(course, courseResults, availableModels));
        }

        PredictionAvailabilityResponse response = new PredictionAvailabilityResponse(
                student.getId(),
                student.getStudentNumber(),
                student.getFullName(),
                healthy,
                new ArrayList<>(availableModels),
                courseAvailabilities
        );

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> prediction(Authentication authentication) {
        return prediction(authentication, null, null);
    }

    @GetMapping("/prediction")
    public ResponseEntity<?> prediction(
            Authentication authentication,
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @RequestParam(value = "level", required = false) String level) {
        Student student = resolveAuthenticatedStudent(authentication);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "A valid student login is required"));
        }

        ObjectId studentObjectId = student.getId() != null && ObjectId.isValid(student.getId()) ? new ObjectId(student.getId()) : null;
        ObjectId userObjectId = student.getUserId();

        List<Enrollment> enrollments = findStudentEnrollments(studentObjectId, userObjectId);
        List<AssessmentResult> allResults = findStudentResults(studentObjectId, userObjectId);
        List<AttendanceRecord> allAttendance = findStudentAttendance(studentObjectId, userObjectId);

        boolean healthy = mlPredictionService.isHealthy();

        // If specific courseCode and level requested
        if (courseCode != null && !courseCode.isBlank() && level != null && !level.isBlank()) {
            Enrollment matchedEnrollment = null;
            Course matchedCourse = null;
            for (Enrollment enrollment : enrollments) {
                Course course = resolveCourse(enrollment);
                if (course != null && course.getCode() != null && course.getCode().equalsIgnoreCase(courseCode.trim())) {
                    matchedEnrollment = enrollment;
                    matchedCourse = course;
                    break;
                }
            }
            if (matchedCourse == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Student is not enrolled in course " + courseCode));
            }
            List<AssessmentResult> courseResults = filterResultsForCourse(allResults, matchedEnrollment, matchedCourse);
            AttendanceRecord courseAttendance = filterAttendanceForCourse(allAttendance, matchedEnrollment, matchedCourse);
            CoursePredictionDto pred = mlPredictionService.predictForCourseWithLevel(
                    matchedCourse,
                    courseResults,
                    courseAttendance,
                    student.getPreviousCgpa(),
                    level.trim(),
                    null
            );
            return ResponseEntity.ok(pred);
        }

        List<CoursePredictionDto> predictions = new ArrayList<>();
        int predictableCount = 0;

        for (Enrollment enrollment : enrollments) {
            Course course = resolveCourse(enrollment);

            List<AssessmentResult> courseResults = filterResultsForCourse(allResults, enrollment, course);
            AttendanceRecord courseAttendance = filterAttendanceForCourse(allAttendance, enrollment, course);

            CoursePredictionDto pred;
            if (level != null && !level.isBlank()) {
                pred = mlPredictionService.predictForCourseWithLevel(
                        course,
                        courseResults,
                        courseAttendance,
                        student.getPreviousCgpa(),
                        level.trim().toLowerCase(Locale.ROOT),
                        null
                );
            } else {
                pred = mlPredictionService.predictForCourse(course, courseResults, courseAttendance, student.getPreviousCgpa());
            }

            if ("AVAILABLE".equals(pred.getPredictionStatus())) {
                predictableCount++;
            }
            predictions.add(pred);
        }

        PredictionResponse response = new PredictionResponse(
                student.getId(),
                student.getStudentNumber(),
                student.getFullName(),
                healthy,
                enrollments.size(),
                predictableCount,
                predictions,
                "Predictions are based on synthetic demonstration models. Values represent predicted overall course score out of 100, not FAT marks."
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/prediction")
    public ResponseEntity<?> predictLevel(
            Authentication authentication,
            @RequestBody PredictRequestDto request) {
        Student student = resolveAuthenticatedStudent(authentication);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "A valid student login is required"));
        }

        if (request == null || request.getCourseCode() == null || request.getLevel() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "courseCode and level are required."));
        }

        ObjectId studentObjectId = student.getId() != null && ObjectId.isValid(student.getId()) ? new ObjectId(student.getId()) : null;
        ObjectId userObjectId = student.getUserId();

        List<Enrollment> enrollments = findStudentEnrollments(studentObjectId, userObjectId);
        Enrollment matchedEnrollment = null;
        Course matchedCourse = null;

        for (Enrollment enrollment : enrollments) {
            Course course = resolveCourse(enrollment);
            if (course != null && course.getCode() != null && course.getCode().equalsIgnoreCase(request.getCourseCode().trim())) {
                matchedEnrollment = enrollment;
                matchedCourse = course;
                break;
            }
        }

        if (matchedCourse == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Student is not enrolled in course " + request.getCourseCode()));
        }

        List<AssessmentResult> allResults = findStudentResults(studentObjectId, userObjectId);
        List<AttendanceRecord> allAttendance = findStudentAttendance(studentObjectId, userObjectId);

        List<AssessmentResult> courseResults = filterResultsForCourse(allResults, matchedEnrollment, matchedCourse);
        AttendanceRecord courseAttendance = filterAttendanceForCourse(allAttendance, matchedEnrollment, matchedCourse);

        CoursePredictionDto prediction = mlPredictionService.predictForCourseWithLevel(
                matchedCourse,
                courseResults,
                courseAttendance,
                student.getPreviousCgpa(),
                request.getLevel().trim(),
                request.getWhatIfMarks()
        );

        return ResponseEntity.ok(prediction);
    }

    @GetMapping("/marks")
    public ResponseEntity<?> marks(Authentication authentication) {
        Student student = resolveAuthenticatedStudent(authentication);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "A valid student login is required"));
        }

        ObjectId studentObjectId = student.getId() != null && ObjectId.isValid(student.getId()) ? new ObjectId(student.getId()) : null;
        ObjectId userObjectId = student.getUserId();

        List<Enrollment> enrollments = findStudentEnrollments(studentObjectId, userObjectId);
        List<AssessmentResult> allResults = findStudentResults(studentObjectId, userObjectId);

        // Fetch Assessment documents to retrieve class averages
        Set<String> assessmentIds = new HashSet<>();
        for (AssessmentResult r : allResults) {
            if (r.getAssessmentId() != null) {
                assessmentIds.add(r.getAssessmentId().toHexString());
            }
        }
        Map<String, Assessment> assessmentMap = new HashMap<>();
        if (!assessmentIds.isEmpty()) {
            for (Assessment a : assessmentRepository.findAllById(assessmentIds)) {
                if (a.getId() != null) {
                    assessmentMap.put(a.getId(), a);
                }
            }
        }

        Map<String, Double> calculatedAvgMap = new HashMap<>();
        Map<String, Integer> submissionsCountMap = new HashMap<>();
        if (mongoTemplate != null && !assessmentIds.isEmpty()) {
            Set<ObjectId> aidObjectIds = new HashSet<>();
            for (String aid : assessmentIds) {
                if (ObjectId.isValid(aid)) aidObjectIds.add(new ObjectId(aid));
            }
            if (!aidObjectIds.isEmpty()) {
                org.springframework.data.mongodb.core.query.Query q = new org.springframework.data.mongodb.core.query.Query(
                        org.springframework.data.mongodb.core.query.Criteria.where("assessmentId").in(aidObjectIds)
                );
                List<org.bson.Document> classResults = mongoTemplate.find(q, org.bson.Document.class, "results");
                Map<String, List<Double>> marksByAid = new HashMap<>();
                for (org.bson.Document doc : classResults) {
                    if ("PENDING".equalsIgnoreCase(doc.getString("status"))) continue;
                    Double m = toDouble(doc.get("obtainedMarks"));
                    if (m == null) m = toDouble(doc.get("marksObtained"));
                    Object aObj = doc.get("assessmentId");
                    String aStr = aObj instanceof ObjectId oid ? oid.toHexString() : (aObj != null ? aObj.toString() : null);
                    if (m != null && aStr != null) {
                        marksByAid.computeIfAbsent(aStr, k -> new ArrayList<>()).add(m);
                    }
                }
                for (Map.Entry<String, List<Double>> entry : marksByAid.entrySet()) {
                    List<Double> mList = entry.getValue();
                    if (!mList.isEmpty()) {
                        double sum = 0.0;
                        for (Double d : mList) sum += d;
                        double avg = Math.round((sum / mList.size()) * 10.0) / 10.0;
                        calculatedAvgMap.put(entry.getKey(), avg);
                        submissionsCountMap.put(entry.getKey(), mList.size());
                    }
                }
            }
        }

        List<Map<String, Object>> courseMarksList = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            Course course = resolveCourse(enrollment);
            List<AssessmentResult> courseResults = filterResultsForCourse(allResults, enrollment, course);

            List<Map<String, Object>> enrichedAssessments = new ArrayList<>();
            for (AssessmentResult r : courseResults) {
                enrichedAssessments.add(enrichAssessmentResult(r, assessmentMap, calculatedAvgMap, submissionsCountMap));
            }

            Map<String, Object> courseData = new LinkedHashMap<>();
            courseData.put("courseId", course.getId());
            courseData.put("courseCode", course.getCode());
            courseData.put("courseName", course.getName());
            courseData.put("credits", course.getCredits());
            courseData.put("evaluationType", course.getEvaluationType());
            courseData.put("assessments", enrichedAssessments);

            courseMarksList.add(courseData);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("studentId", student.getId());
        response.put("courses", courseMarksList);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/attendance")
    public ResponseEntity<?> attendance(Authentication authentication) {
        Student student = resolveAuthenticatedStudent(authentication);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "A valid student login is required"));
        }

        ObjectId studentObjectId = student.getId() != null && ObjectId.isValid(student.getId()) ? new ObjectId(student.getId()) : null;
        ObjectId userObjectId = student.getUserId();

        List<Enrollment> enrollments = findStudentEnrollments(studentObjectId, userObjectId);
        List<AttendanceRecord> allAttendance = findStudentAttendance(studentObjectId, userObjectId);

        List<Map<String, Object>> courseAttendanceList = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            Course course = resolveCourse(enrollment);
            AttendanceRecord ar = filterAttendanceForCourse(allAttendance, enrollment, course);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("courseId", course.getId());
            data.put("courseCode", course.getCode());
            data.put("courseName", course.getName());
            data.put("credits", course.getCredits());
            data.put("classesConducted", ar != null ? ar.getClassesConducted() : null);
            data.put("classesAttended", ar != null ? ar.getClassesAttended() : null);
            data.put("classesMissed", ar != null ? ar.getClassesMissed() : null);
            data.put("attendancePercentage", ar != null ? ar.getAttendancePercentage() : null);

            courseAttendanceList.add(data);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("studentId", student.getId());
        response.put("courses", courseAttendanceList);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> recommendations(Authentication authentication) {
        Student student = resolveAuthenticatedStudent(authentication);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "A valid student login is required"));
        }

        if (mongoTemplate == null) {
            return ResponseEntity.ok(Map.of("recommendations", List.of()));
        }

        ObjectId studentObjectId = student.getId() != null && ObjectId.isValid(student.getId()) ? new ObjectId(student.getId()) : null;
        ObjectId userObjectId = student.getUserId();

        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query();
        if (studentObjectId != null && userObjectId != null) {
            query.addCriteria(new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                    org.springframework.data.mongodb.core.query.Criteria.where("studentId").is(studentObjectId),
                    org.springframework.data.mongodb.core.query.Criteria.where("userId").is(userObjectId)
            ));
        } else if (studentObjectId != null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("studentId").is(studentObjectId));
        } else if (userObjectId != null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("userId").is(userObjectId));
        }

        List<org.bson.Document> docs = mongoTemplate.find(query, org.bson.Document.class, "recommendations");
        List<Map<String, Object>> result = new ArrayList<>();
        for (org.bson.Document doc : docs) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", doc.getObjectId("_id") != null ? doc.getObjectId("_id").toHexString() : null);
            map.put("title", doc.getString("title"));
            map.put("description", doc.getString("description"));
            map.put("category", doc.getString("category"));
            map.put("priority", doc.getString("priority"));
            map.put("status", doc.getString("status"));
            result.add(map);
        }

        return ResponseEntity.ok(Map.of("recommendations", result));
    }

    private List<Enrollment> findStudentEnrollments(ObjectId studentId, ObjectId userId) {
        List<Enrollment> list = new ArrayList<>();
        if (studentId != null) {
            list.addAll(enrollmentRepository.findByStudentId(studentId));
        }
        if (list.isEmpty() && userId != null) {
            list.addAll(enrollmentRepository.findByUserId(userId));
        }
        return list;
    }

    private List<AssessmentResult> findStudentResults(ObjectId studentId, ObjectId userId) {
        List<AssessmentResult> list = new ArrayList<>();
        if (studentId != null) {
            list.addAll(resultRepository.findByStudentId(studentId));
        }
        if (list.isEmpty() && userId != null) {
            list.addAll(resultRepository.findByUserId(userId));
        }
        return list;
    }

    private List<AttendanceRecord> findStudentAttendance(ObjectId studentId, ObjectId userId) {
        List<AttendanceRecord> list = new ArrayList<>();
        if (studentId != null) {
            list.addAll(attendanceRepository.findByStudentId(studentId));
        }
        if (list.isEmpty() && userId != null) {
            list.addAll(attendanceRepository.findByUserId(userId));
        }
        return list;
    }

    private Course resolveCourse(Enrollment enrollment) {
        Course course = null;
        if (enrollment.getCourseId() != null) {
            course = courseRepository.findById(enrollment.getCourseId().toHexString()).orElse(null);
        }
        if (course == null && enrollment.getCourseCode() != null) {
            course = courseRepository.findByCode(enrollment.getCourseCode()).orElse(null);
        }
        if (course == null) {
            course = new Course();
            course.setCode(enrollment.getCourseCode() != null ? enrollment.getCourseCode() : "COURSE");
            course.setName(enrollment.getCourseCode() != null ? enrollment.getCourseCode() : "Enrolled Course");
        }
        return course;
    }

    private List<AssessmentResult> filterResultsForCourse(List<AssessmentResult> allResults, Enrollment enrollment, Course course) {
        List<AssessmentResult> list = new ArrayList<>();
        for (AssessmentResult r : allResults) {
            if (r == null) continue;
            boolean match = false;
            if (enrollment.getCourseId() != null && enrollment.getCourseId().equals(r.getCourseId())) {
                match = true;
            } else if (r.getCourseCode() != null) {
                if (enrollment.getCourseCode() != null && enrollment.getCourseCode().equalsIgnoreCase(r.getCourseCode())) {
                    match = true;
                } else if (course.getCode() != null && course.getCode().equalsIgnoreCase(r.getCourseCode())) {
                    match = true;
                }
            } else if (enrollment.getId() != null && r.getEnrollmentId() != null && ObjectId.isValid(enrollment.getId())) {
                if (new ObjectId(enrollment.getId()).equals(r.getEnrollmentId())) {
                    match = true;
                }
            }
            if (match) {
                list.add(r);
            }
        }
        return list;
    }

    private AttendanceRecord filterAttendanceForCourse(List<AttendanceRecord> allAttendance, Enrollment enrollment, Course course) {
        for (AttendanceRecord a : allAttendance) {
            if (a == null) continue;
            if (enrollment.getCourseId() != null && enrollment.getCourseId().equals(a.getCourseId())) {
                return a;
            }
            if (a.getCourseCode() != null) {
                if (enrollment.getCourseCode() != null && enrollment.getCourseCode().equalsIgnoreCase(a.getCourseCode())) {
                    return a;
                }
                if (course.getCode() != null && course.getCode().equalsIgnoreCase(a.getCourseCode())) {
                    return a;
                }
            }
            if (enrollment.getId() != null && a.getEnrollmentId() != null && ObjectId.isValid(enrollment.getId())) {
                if (new ObjectId(enrollment.getId()).equals(a.getEnrollmentId())) {
                    return a;
                }
            }
        }
        return null;
    }

    private Map<String, Object> enrichAssessmentResult(
            AssessmentResult res,
            Map<String, Assessment> assessmentMap,
            Map<String, Double> calculatedAvgMap,
            Map<String, Integer> submissionsCountMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", res.getId());
        map.put("assessmentId", res.getAssessmentId() != null ? res.getAssessmentId().toHexString() : null);
        map.put("courseId", res.getCourseId() != null ? res.getCourseId().toHexString() : null);
        map.put("courseCode", res.getCourseCode());
        map.put("enrollmentId", res.getEnrollmentId() != null ? res.getEnrollmentId().toHexString() : null);
        map.put("studentId", res.getStudentId() != null ? res.getStudentId().toHexString() : null);
        map.put("userId", res.getUserId() != null ? res.getUserId().toHexString() : null);
        map.put("assessmentName", res.getAssessmentName());
        map.put("assessmentType", res.getAssessmentType());
        map.put("status", res.getStatus());
        map.put("grade", res.getGrade());
        map.put("obtainedMarks", res.getObtainedMarks());
        map.put("marksObtained", res.getMarksObtained());
        map.put("maxMarks", res.getMaxMarks());

        String aKey = res.getAssessmentId() != null ? res.getAssessmentId().toHexString() : null;
        Assessment assess = aKey != null ? assessmentMap.get(aKey) : null;

        Double officialAvg = assess != null ? assess.getClassAverage() : null;
        Double dynamicAvg = aKey != null ? calculatedAvgMap.get(aKey) : null;
        Double classAverage = officialAvg != null ? officialAvg : dynamicAvg;
        Integer submissionsCount = aKey != null ? submissionsCountMap.get(aKey) : null;

        map.put("classAverage", classAverage);
        map.put("officialClassAverage", officialAvg);
        map.put("calculatedClassAverage", dynamicAvg);
        map.put("submissionsCount", submissionsCount);

        String name = (res.getAssessmentName() != null ? res.getAssessmentName() : "").toUpperCase(Locale.ROOT);
        String type = (res.getAssessmentType() != null ? res.getAssessmentType() : "").toUpperCase(Locale.ROOT);

        double rawMax = 100.0;
        double weight = 0.0;
        if (name.contains("CAT 1") || name.equals("CAT1") || (type.equals("CAT") && name.contains("1"))) {
            rawMax = 50.0;
            weight = 15.0;
        } else if (name.contains("CAT 2") || name.equals("CAT2") || (type.equals("CAT") && name.contains("2"))) {
            rawMax = 50.0;
            weight = 15.0;
        } else if (name.contains("QUIZ 1") || name.equals("QUIZ1") || (type.equals("QUIZ") && name.contains("1"))) {
            rawMax = 10.0;
            weight = 10.0;
        } else if (name.contains("QUIZ 2") || name.equals("QUIZ2") || (type.equals("QUIZ") && name.contains("2"))) {
            rawMax = 10.0;
            weight = 10.0;
        } else if (name.contains("QUIZ 3") || name.equals("QUIZ3") || (type.equals("QUIZ") && name.contains("3"))) {
            rawMax = 10.0;
            weight = 10.0;
        } else if (name.contains("FAT") || type.contains("FAT") || name.contains("FINAL")) {
            rawMax = 100.0;
            weight = 40.0;
        } else if (type.equals("LAB") || name.contains("LAB") || type.equals("OTHER")) {
            rawMax = 10.0;
            weight = 2.5;
        }

        map.put("rawMaxMarks", rawMax);
        map.put("weightage", weight);

        Double obtained = res.getObtainedMarks();
        Double storedMax = res.getMaxMarks();
        if (obtained != null && storedMax != null && storedMax > 0) {
            double normalized = (obtained / storedMax) * rawMax;
            double rounded = Math.round(normalized * 10.0) / 10.0;
            map.put("rawMarks", Math.min(rawMax, Math.max(0.0, rounded)));
        } else {
            map.put("rawMarks", obtained);
        }

        return map;
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
}