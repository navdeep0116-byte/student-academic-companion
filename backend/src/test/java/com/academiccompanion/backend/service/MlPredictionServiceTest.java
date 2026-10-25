package com.academiccompanion.backend.service;

import com.academiccompanion.backend.model.AssessmentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MlPredictionServiceTest {

    private MlPredictionService service;

    @BeforeEach
    void setUp() {
        service = new MlPredictionService("http://127.0.0.1:5000");
    }

    @Test
    void testDetermineEligibleStage_InsufficientData() {
        // No marks at all
        assertNull(service.determineEligibleStage(Map.of()));

        // Only CAT 1 (missing Quiz 1)
        assertNull(service.determineEligibleStage(Map.of("cat1", 35.0)));

        // Only Quiz 1 (missing CAT 1)
        assertNull(service.determineEligibleStage(Map.of("quiz1", 8.0)));
    }

    @Test
    void testDetermineEligibleStage_Level1() {
        Map<String, Double> marks = Map.of(
                "cat1", 35.0,
                "quiz1", 8.0
        );
        assertEquals("level1", service.determineEligibleStage(marks));
    }

    @Test
    void testDetermineEligibleStage_Level2() {
        Map<String, Double> marks = Map.of(
                "cat1", 35.0,
                "quiz1", 8.0,
                "cat2", 40.0,
                "quiz2", 9.0
        );
        assertEquals("level2", service.determineEligibleStage(marks));
    }

    @Test
    void testDetermineEligibleStage_Level3() {
        Map<String, Double> marksWithoutLabs = Map.of(
                "cat1", 35.0,
                "quiz1", 8.0,
                "cat2", 40.0,
                "quiz2", 9.0,
                "quiz3", 8.5
        );
        // Without 10 labs, highest eligible stage is level2
        assertEquals("level2", service.determineEligibleStage(marksWithoutLabs));

        // With all 10 labs, level3 is satisfied
        Map<String, Double> marksWithLabs = new java.util.HashMap<>(marksWithoutLabs);
        for (int i = 1; i <= 10; i++) {
            marksWithLabs.put("lab" + i, 8.0);
        }
        assertEquals("level3", service.determineEligibleStage(marksWithLabs));
    }

    @Test
    void testCheckLevelEligibility_ZeroMarksAreValid() {
        // Legitimate zero marks
        Map<String, Double> marks = Map.of(
                "cat1", 0.0,
                "quiz1", 0.0
        );
        var res = service.checkLevelEligibility("level1", marks, Set.of("level1", "level2", "level3"));
        assertTrue(res.isEligible(), "Legitimate 0 marks must be eligible");
        assertNull(res.getReason());
    }

    @Test
    void testCheckLevelEligibility_MissingInputs() {
        Map<String, Double> marks = Map.of("cat1", 35.0);
        var res = service.checkLevelEligibility("level1", marks, Set.of("level1", "level2", "level3"));
        assertFalse(res.isEligible());
        assertTrue(res.getMissingInputs().contains("Quiz 1"));
        assertNotNull(res.getReason());
    }

    @Test
    void testCheckLevelEligibility_ModelUnavailable() {
        Map<String, Double> marks = Map.of("cat1", 35.0, "quiz1", 8.0);
        var res = service.checkLevelEligibility("level1", marks, Set.of("level2", "level3")); // level1 missing
        assertFalse(res.isEligible());
        assertTrue(res.getReason().contains("Model file"));
    }

    @Test
    void testExtractNormalizedMarks_ExcludesFatAndNormalizesScales() {
        List<AssessmentResult> results = new ArrayList<>();

        // Completed CAT 1 (max 20, obtained 16 -> 16/20 * 50 = 40.0)
        AssessmentResult cat1 = new AssessmentResult();
        cat1.setAssessmentName("CAT 1");
        cat1.setAssessmentType("CAT");
        cat1.setObtainedMarks(16.0);
        cat1.setMaxMarks(20.0);
        cat1.setStatus("COMPLETED");
        results.add(cat1);

        // Completed Quiz 1 (max 10, obtained 9.0 -> 9.0)
        AssessmentResult quiz1 = new AssessmentResult();
        quiz1.setAssessmentName("Quiz 1");
        quiz1.setAssessmentType("QUIZ");
        quiz1.setObtainedMarks(9.0);
        quiz1.setMaxMarks(10.0);
        quiz1.setStatus("COMPLETED");
        results.add(quiz1);

        // Completed Lab Assessment 1 (max 20, obtained 18 -> 18/20 * 10 = 9.0)
        AssessmentResult lab1 = new AssessmentResult();
        lab1.setAssessmentName("Lab Assessment 1");
        lab1.setAssessmentType("LAB");
        lab1.setObtainedMarks(18.0);
        lab1.setMaxMarks(20.0);
        lab1.setStatus("COMPLETED");
        results.add(lab1);

        // FAT result - MUST BE EXCLUDED!
        AssessmentResult fat = new AssessmentResult();
        fat.setAssessmentName("FAT");
        fat.setAssessmentType("FAT");
        fat.setObtainedMarks(85.0);
        fat.setMaxMarks(100.0);
        fat.setStatus("COMPLETED");
        results.add(fat);

        // Pending CAT 2 - MUST BE EXCLUDED because status is PENDING!
        AssessmentResult cat2 = new AssessmentResult();
        cat2.setAssessmentName("CAT 2");
        cat2.setAssessmentType("CAT");
        cat2.setObtainedMarks(null);
        cat2.setMaxMarks(20.0);
        cat2.setStatus("PENDING");
        results.add(cat2);

        Map<String, Double> normalized = service.extractNormalizedMarks(results);

        // Assertions
        assertEquals(40.0, normalized.get("cat1"));
        assertEquals(9.0, normalized.get("quiz1"));
        assertEquals(9.0, normalized.get("lab1"));
        assertFalse(normalized.containsKey("cat2"), "Pending CAT 2 must not be included");
        assertFalse(normalized.containsKey("fat"), "FAT must strictly never be included");
        assertFalse(normalized.containsKey("fatMarks"), "FAT must strictly never be included");
    }

    @Test
    void testMapCourseCode() {
        assertEquals("DBMS", service.mapCourseCode("BCSE203", "Database Systems"));
        assertEquals("DSA", service.mapCourseCode("BCSE204", "Data Structures and Algorithms"));
        assertEquals("STAT", service.mapCourseCode("BAMAT207", "Statistics for Engineers"));
        assertEquals("OOP", service.mapCourseCode("BCSE205", "Object Oriented Programming"));
        assertEquals("CN", service.mapCourseCode("BCSE206", "Computer Networks"));
        assertEquals("OS", service.mapCourseCode("BCSE207", "Operating Systems"));
    }

    @Test
    void testExtractNormalizedMarks_RawScale50() {
        List<AssessmentResult> results = new ArrayList<>();

        // Completed CAT 1 with raw max 50, obtained 38.0 -> 38.0
        AssessmentResult cat1 = new AssessmentResult();
        cat1.setAssessmentName("CAT 1");
        cat1.setAssessmentType("CAT");
        cat1.setObtainedMarks(38.0);
        cat1.setMaxMarks(50.0);
        cat1.setStatus("COMPLETED");
        results.add(cat1);

        // Completed CAT 2 with raw max 50, obtained 42.0 -> 42.0
        AssessmentResult cat2 = new AssessmentResult();
        cat2.setAssessmentName("CAT 2");
        cat2.setAssessmentType("CAT");
        cat2.setObtainedMarks(42.0);
        cat2.setMaxMarks(50.0);
        cat2.setStatus("COMPLETED");
        results.add(cat2);

        Map<String, Double> normalized = service.extractNormalizedMarks(results);
        assertEquals(38.0, normalized.get("cat1"));
        assertEquals(42.0, normalized.get("cat2"));
    }
}
