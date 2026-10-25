package com.academiccompanion.backend.service;

import com.academiccompanion.backend.dto.CourseLevelAvailabilityDto;
import com.academiccompanion.backend.dto.CoursePredictionDto;
import com.academiccompanion.backend.dto.LevelEligibilityDto;
import com.academiccompanion.backend.model.AssessmentResult;
import com.academiccompanion.backend.model.AttendanceRecord;
import com.academiccompanion.backend.model.Course;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;

@Service
public class MlPredictionService {

    private static final Logger log = LoggerFactory.getLogger(MlPredictionService.class);
    private static final Set<String> SUPPORTED_COURSES = Set.of(
            "DBMS", "CN", "STAT", "OOP", "DSA", "OS", "JAVA", "SE"
    );

    private final RestClient restClient;
    private final String mlServiceUrl;

    public MlPredictionService(@Value("${ml.service.url:http://127.0.0.1:5000}") String mlServiceUrl) {
        this.mlServiceUrl = mlServiceUrl.endsWith("/") ? mlServiceUrl.substring(0, mlServiceUrl.length() - 1) : mlServiceUrl;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));

        this.restClient = RestClient.builder()
                .baseUrl(this.mlServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isHealthy() {
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(Map.class);
            return response != null && "UP".equalsIgnoreCase(String.valueOf(response.get("status")));
        } catch (Exception e) {
            log.warn("ML prediction service health check failed at {}: {}", mlServiceUrl, e.getMessage());
            return false;
        }
    }

    public Set<String> getAvailableModelLevels() {
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/models")
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.get("availableLevels") instanceof List<?> list) {
                Set<String> set = new LinkedHashSet<>();
                for (Object item : list) {
                    if (item != null) set.add(item.toString());
                }
                return set;
            }
        } catch (Exception e) {
            log.warn("Failed to query model availability from ML service at {}: {}", mlServiceUrl, e.getMessage());
        }
        return Collections.emptySet();
    }

    public LevelEligibilityDto checkLevelEligibility(String level, Map<String, Double> marks, Set<String> availableModels) {
        if (marks == null) {
            marks = Collections.emptyMap();
        }

        List<String> requiredInputs;
        List<String> missing = new ArrayList<>();

        if ("level1".equalsIgnoreCase(level)) {
            requiredInputs = List.of("cat1", "quiz1");
            if (!marks.containsKey("cat1")) missing.add("CAT 1");
            if (!marks.containsKey("quiz1")) missing.add("Quiz 1");
        } else if ("level2".equalsIgnoreCase(level)) {
            requiredInputs = List.of("cat1", "cat2", "quiz1", "quiz2");
            if (!marks.containsKey("cat1")) missing.add("CAT 1");
            if (!marks.containsKey("cat2")) missing.add("CAT 2");
            if (!marks.containsKey("quiz1")) missing.add("Quiz 1");
            if (!marks.containsKey("quiz2")) missing.add("Quiz 2");
        } else if ("level3".equalsIgnoreCase(level)) {
            List<String> allInputs = new ArrayList<>(List.of("cat1", "cat2", "quiz1", "quiz2", "quiz3"));
            for (int i = 1; i <= 10; i++) {
                allInputs.add("lab" + i);
            }
            requiredInputs = allInputs;

            if (!marks.containsKey("cat1")) missing.add("CAT 1");
            if (!marks.containsKey("cat2")) missing.add("CAT 2");
            if (!marks.containsKey("quiz1")) missing.add("Quiz 1");
            if (!marks.containsKey("quiz2")) missing.add("Quiz 2");
            if (!marks.containsKey("quiz3")) missing.add("Quiz 3");

            List<String> missingLabs = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                if (!marks.containsKey("lab" + i)) {
                    missingLabs.add("Lab " + i);
                }
            }
            if (missingLabs.size() == 10) {
                missing.add("10 Lab assessments");
            } else if (!missingLabs.isEmpty()) {
                missing.add(String.join(", ", missingLabs));
            }
        } else {
            return new LevelEligibilityDto(false, Collections.emptyList(), List.of("Unknown level"), "Unknown prediction level: " + level);
        }

        if (availableModels != null && !availableModels.isEmpty() && !availableModels.contains(level.toLowerCase(Locale.ROOT))) {
            return new LevelEligibilityDto(false, requiredInputs, missing, "Model file for " + level + " is not available in ML service.");
        }

        if (!missing.isEmpty()) {
            return new LevelEligibilityDto(false, requiredInputs, missing, "Missing completed results for: " + String.join(", ", missing));
        }

        return new LevelEligibilityDto(true, requiredInputs, Collections.emptyList(), null);
    }

    public CourseLevelAvailabilityDto getCourseLevelAvailability(Course course, List<AssessmentResult> results, Set<String> availableModels) {
        CourseLevelAvailabilityDto dto = new CourseLevelAvailabilityDto();
        dto.setCourseId(course.getId());
        dto.setCourseCode(course.getCode());
        dto.setCourseName(course.getName());
        dto.setModelCourseCode(mapCourseCode(course.getCode(), course.getName()));

        Map<String, Double> normalizedMarks = extractNormalizedMarks(results);
        dto.setCompletedAssessmentsCount(normalizedMarks.size());
        dto.setRecordedMarks(normalizedMarks);

        Map<String, LevelEligibilityDto> levels = new LinkedHashMap<>();
        List<String> eligibleLevels = new ArrayList<>();

        for (String lvl : List.of("level1", "level2", "level3")) {
            LevelEligibilityDto elig = checkLevelEligibility(lvl, normalizedMarks, availableModels);
            levels.put(lvl, elig);
            if (elig.isEligible()) {
                eligibleLevels.add(lvl);
            }
        }

        dto.setLevels(levels);
        dto.setEligibleLevels(eligibleLevels);
        return dto;
    }

    public CoursePredictionDto predictForCourseWithLevel(
            Course course,
            List<AssessmentResult> results,
            AttendanceRecord attendance,
            Double studentCgpa,
            String requestedLevel,
            Map<String, Double> whatIfMarks) {

        CoursePredictionDto dto = new CoursePredictionDto();
        dto.setCourseId(course.getId());
        dto.setCourseCode(course.getCode());
        dto.setCourseName(course.getName());
        dto.setWarning("Synthetic demo data model. Overall course score out of 100, NOT FAT marks.");

        if (requestedLevel == null || (!requestedLevel.equalsIgnoreCase("level1")
                && !requestedLevel.equalsIgnoreCase("level2")
                && !requestedLevel.equalsIgnoreCase("level3"))) {
            dto.setPredictionStatus("INSUFFICIENT_DATA");
            dto.setStatusMessage("Invalid prediction level requested. Must be level1, level2, or level3.");
            return dto;
        }

        String stage = requestedLevel.toLowerCase(Locale.ROOT);
        dto.setLevel(stage);
        switch (stage) {
            case "level1" -> dto.setStageDescription("Level 1 (Early prediction based on CAT 1 and Quiz 1)");
            case "level2" -> dto.setStageDescription("Level 2 (Mid-semester prediction based on CAT 1, CAT 2, Quiz 1, Quiz 2)");
            case "level3" -> dto.setStageDescription("Level 3 (Pre-FAT comprehensive prediction with CATs, Quizzes, and all 10 Labs)");
        }

        String modelCourseCode = mapCourseCode(course.getCode(), course.getName());
        if (modelCourseCode == null || !SUPPORTED_COURSES.contains(modelCourseCode)) {
            dto.setPredictionStatus("UNSUPPORTED_COURSE");
            dto.setStatusMessage("Course " + (course != null && course.getCode() != null ? course.getCode() : "unknown") + " is not supported by the ML prediction model.");
            return dto;
        }

        Map<String, Double> authoritativeMarks = extractNormalizedMarks(results);
        dto.setCompletedAssessmentsCount(authoritativeMarks.size());

        // Merge what-if marks if provided; authoritative completed marks strictly take precedence
        Map<String, Double> effectiveMarks = new LinkedHashMap<>(authoritativeMarks);
        if (whatIfMarks != null) {
            for (Map.Entry<String, Double> entry : whatIfMarks.entrySet()) {
                String key = entry.getKey() != null ? entry.getKey().toLowerCase(Locale.ROOT) : "";
                Double val = entry.getValue();
                if (!effectiveMarks.containsKey(key) && val != null) {
                    effectiveMarks.put(key, val);
                }
            }
        }

        dto.setNormalizedInputMarks(effectiveMarks);

        // Server-side validation of level eligibility
        Set<String> availableModels = getAvailableModelLevels();
        LevelEligibilityDto eligibility = checkLevelEligibility(stage, effectiveMarks, availableModels);
        if (!eligibility.isEligible()) {
            dto.setPredictionStatus("INSUFFICIENT_DATA");
            dto.setStatusMessage(eligibility.getReason() != null ? eligibility.getReason() : "Requirements for " + stage + " are not met.");
            return dto;
        }

        // Context parameters
        double attendancePct = 80.0;
        if (attendance != null && attendance.getAttendancePercentage() != null) {
            attendancePct = attendance.getAttendancePercentage();
        }
        double cgpa = (studentCgpa != null && studentCgpa > 0) ? studentCgpa : 7.0;
        double weeklyStudyHours = 8.0;

        // Build Flask payload
        Map<String, Object> payloadMarks = new LinkedHashMap<>();
        if ("level1".equals(stage)) {
            payloadMarks.put("cat1", effectiveMarks.get("cat1"));
            payloadMarks.put("quiz1", effectiveMarks.get("quiz1"));
        } else if ("level2".equals(stage)) {
            payloadMarks.put("cat1", effectiveMarks.get("cat1"));
            payloadMarks.put("cat2", effectiveMarks.get("cat2"));
            payloadMarks.put("quiz1", effectiveMarks.get("quiz1"));
            payloadMarks.put("quiz2", effectiveMarks.get("quiz2"));
        } else {
            payloadMarks.put("cat1", effectiveMarks.get("cat1"));
            payloadMarks.put("cat2", effectiveMarks.get("cat2"));
            payloadMarks.put("quiz1", effectiveMarks.get("quiz1"));
            payloadMarks.put("quiz2", effectiveMarks.get("quiz2"));
            payloadMarks.put("quiz3", effectiveMarks.get("quiz3"));
            for (int i = 1; i <= 10; i++) {
                payloadMarks.put("lab" + i, effectiveMarks.get("lab" + i));
            }
        }

        // Strict guard: ensure no forbidden FAT or final keys exist
        payloadMarks.remove("fat");
        payloadMarks.remove("fatMarks");
        payloadMarks.remove("overallScore");
        payloadMarks.remove("theoryScore");
        payloadMarks.remove("labScore");

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("level", stage);
        requestBody.put("courseCode", modelCourseCode);
        requestBody.put("attendancePercentage", round(attendancePct, 1));
        requestBody.put("previousCgpa", round(cgpa, 2));
        requestBody.put("weeklyStudyHours", round(weeklyStudyHours, 1));
        requestBody.put("marks", payloadMarks);

        try {
            Map<?, ?> response = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("predictedOverallScore")) {
                dto.setPredictionStatus("AVAILABLE");
                dto.setPredictedOverallScore(toDouble(response.get("predictedOverallScore")));
                dto.setLikelyGrade(String.valueOf(response.get("likelyGrade")));
                dto.setRiskLevel(String.valueOf(response.get("riskLevel")));
                dto.setStatusMessage("Prediction generated by Flask ML service.");

                Object rangeObj = response.get("illustrativeRange");
                if (rangeObj instanceof List<?> rangeList && rangeList.size() >= 2) {
                    dto.setIllustrativeRange(List.of(
                            toDouble(rangeList.get(0)),
                            toDouble(rangeList.get(1))
                    ));
                }
                return dto;
            } else {
                dto.setPredictionStatus("SERVICE_UNAVAILABLE");
                dto.setStatusMessage("ML service returned an unexpected response.");
                return dto;
            }
        } catch (Exception e) {
            log.warn("Failed to obtain prediction from ML service for course {} ({}): {}", course.getCode(), stage, e.getMessage());
            dto.setPredictionStatus("SERVICE_UNAVAILABLE");
            dto.setStatusMessage("ML prediction service is currently unavailable.");
            return dto;
        }
    }

    public CoursePredictionDto predictForCourse(
            Course course,
            List<AssessmentResult> results,
            AttendanceRecord attendance,
            Double studentCgpa) {

        Map<String, Double> normalizedMarks = extractNormalizedMarks(results);
        String stage = determineEligibleStage(normalizedMarks);
        if (stage == null) {
            CoursePredictionDto dto = new CoursePredictionDto();
            dto.setCourseId(course.getId());
            dto.setCourseCode(course.getCode());
            dto.setCourseName(course.getName());
            dto.setWarning("Synthetic demo data model. Overall course score out of 100, NOT FAT marks.");
            dto.setPredictionStatus("INSUFFICIENT_DATA");
            dto.setLevel(null);
            dto.setCompletedAssessmentsCount(normalizedMarks.size());
            dto.setNormalizedInputMarks(normalizedMarks);
            dto.setStageDescription("Level 1 requires completed CAT 1 and Quiz 1.");
            dto.setStatusMessage("Prediction pending: insufficient evaluations. At least CAT 1 and Quiz 1 must be completed.");
            return dto;
        }

        return predictForCourseWithLevel(course, results, attendance, studentCgpa, stage, null);
    }

    public String mapCourseCode(String code, String name) {
        if (code != null) {
            String upper = code.trim().toUpperCase(Locale.ROOT);
            if (SUPPORTED_COURSES.contains(upper)) {
                return upper;
            }
            if (upper.equals("BACSE202")) return "DBMS";
            if (upper.equals("BACSE203")) return "CN";
            if (upper.equals("BACSE324")) return "OS";
            if (upper.equals("BAMAT207")) return "STAT";
        }
        if (name != null) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.contains("database")) return "DBMS";
            if (lower.contains("network")) return "CN";
            if (lower.contains("embedded") || lower.contains("operating system") || lower.contains("architecture")) return "OS";
            if (lower.contains("probab") || lower.contains("statistic")) return "STAT";
            if (lower.contains("data structure")) return "DSA";
            if (lower.contains("object oriented") || lower.contains("oop")) return "OOP";
            if (lower.contains("java")) return "JAVA";
            if (lower.contains("software")) return "SE";
        }
        if (code != null) {
            String upper = code.trim().toUpperCase(Locale.ROOT);
            if (upper.contains("DBMS") || upper.contains("202")) return "DBMS";
            if (upper.contains("CN") || upper.contains("NET")) return "CN";
            if (upper.contains("STAT") || upper.contains("MAT")) return "STAT";
            if (upper.contains("DSA") || upper.contains("204")) return "DSA";
            if (upper.contains("OOP") || upper.contains("205")) return "OOP";
            if (upper.contains("OS") || upper.contains("324")) return "OS";
            if (upper.contains("JAVA")) return "JAVA";
            if (upper.contains("SE")) return "SE";
        }
        return null;
    }

    public Map<String, Double> extractNormalizedMarks(List<AssessmentResult> results) {
        Map<String, Double> map = new LinkedHashMap<>();
        if (results == null) return map;

        int labCounter = 1;

        for (AssessmentResult res : results) {
            if (res == null) continue;
            if (res.getStatus() == null || !"COMPLETED".equalsIgnoreCase(res.getStatus())) {
                continue; // Skip pending or uncompleted
            }
            Double obtained = res.getObtainedMarks();
            if (obtained == null) continue;

            String name = (res.getAssessmentName() != null ? res.getAssessmentName() : "").toUpperCase(Locale.ROOT);
            String type = (res.getAssessmentType() != null ? res.getAssessmentType() : "").toUpperCase(Locale.ROOT);

            // STRICT EXCLUSION: Never use FAT as an input!
            if (name.contains("FAT") || type.contains("FAT") || name.contains("FINAL") || type.contains("FINAL")) {
                continue;
            }

            Double max = res.getMaxMarks();

            if (name.contains("CAT 1") || name.equals("CAT1") || (type.equals("CAT") && name.contains("1"))) {
                map.put("cat1", normalize(obtained, max, 50.0));
            } else if (name.contains("CAT 2") || name.equals("CAT2") || (type.equals("CAT") && name.contains("2"))) {
                map.put("cat2", normalize(obtained, max, 50.0));
            } else if (name.contains("QUIZ 1") || name.equals("QUIZ1") || (type.equals("QUIZ") && name.contains("1"))) {
                map.put("quiz1", normalize(obtained, max, 10.0));
            } else if (name.contains("QUIZ 2") || name.equals("QUIZ2") || (type.equals("QUIZ") && name.contains("2"))) {
                map.put("quiz2", normalize(obtained, max, 10.0));
            } else if (name.contains("QUIZ 3") || name.equals("QUIZ3") || (type.equals("QUIZ") && name.contains("3"))) {
                map.put("quiz3", normalize(obtained, max, 10.0));
            } else if (type.equals("LAB") || name.contains("LAB")) {
                if (labCounter <= 10) {
                    map.put("lab" + labCounter, normalize(obtained, max, 10.0));
                    labCounter++;
                }
            }
        }

        return map;
    }

    public String determineEligibleStage(Map<String, Double> marks) {
        if (marks == null || !marks.containsKey("cat1") || !marks.containsKey("quiz1")) {
            return null; // Level 1 requires CAT 1 and Quiz 1
        }
        if (!marks.containsKey("cat2") || !marks.containsKey("quiz2")) {
            return "level1"; // Level 1 satisfied, Level 2 missing
        }
        if (!marks.containsKey("quiz3")) {
            return "level2"; // Level 2 satisfied, Level 3 missing quiz3
        }
        for (int i = 1; i <= 10; i++) {
            if (!marks.containsKey("lab" + i)) {
                return "level2"; // Level 3 requires all 10 labs
            }
        }
        return "level3"; // Level 3 satisfied
    }

    private double normalize(Double obtained, Double maxMarks, double targetMax) {
        if (obtained == null) return 0.0;
        if (maxMarks != null && maxMarks > 0) {
            double normalized = (obtained / maxMarks) * targetMax;
            return round(Math.min(targetMax, Math.max(0.0, normalized)), 2);
        }
        return round(Math.min(targetMax, Math.max(0.0, obtained)), 2);
    }

    private double round(double val, int decimals) {
        return BigDecimal.valueOf(val).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
    }

    private Double toDouble(Object obj) {
        if (obj instanceof Number num) {
            return num.doubleValue();
        }
        if (obj != null) {
            try {
                return Double.parseDouble(obj.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0.0;
    }
}
