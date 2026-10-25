import unittest
import threading
import json
from pathlib import Path
from app import app, MODELS

class TestMlService(unittest.TestCase):
    def setUp(self):
        self.client = app.test_client()

    def test_01_startup_and_health_loads_no_models(self):
        # Health check must not trigger any eager loading
        res = self.client.get("/health")
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertEqual(data["status"], "UP")
        # Initially, modelsLoaded should be empty (or whatever is in MODELS)
        self.assertIsInstance(data["modelsLoaded"], list)

    def test_02_model_availability_does_not_load_models(self):
        initial_loaded = list(MODELS.keys())
        res = self.client.get("/models")
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertIn("availableLevels", data)
        self.assertIn("level1", data["availableLevels"])
        self.assertIn("level2", data["availableLevels"])
        self.assertIn("level3", data["availableLevels"])
        self.assertEqual(list(MODELS.keys()), initial_loaded, "Calling /models must not load models into RAM")

    def test_03_invalid_level_rejected(self):
        res = self.client.post("/predict", json={"level": "level99", "courseCode": "DBMS", "marks": {}})
        self.assertEqual(res.status_code, 400)
        self.assertIn("error", res.get_json())

    def test_04_invalid_course_rejected(self):
        res = self.client.post("/predict", json={"level": "level1", "courseCode": "INVALID", "marks": {"cat1": 35, "quiz1": 8}})
        self.assertEqual(res.status_code, 400)
        self.assertIn("Unknown courseCode", res.get_json()["error"])

    def test_05_level1_missing_marks_rejected(self):
        # Missing quiz1
        res = self.client.post("/predict", json={"level": "level1", "courseCode": "DBMS", "marks": {"cat1": 35}})
        self.assertEqual(res.status_code, 400)
        self.assertIn("requires marks for: quiz1", res.get_json()["error"])

    def test_06_level1_predict_lazy_loads_and_succeeds(self):
        res = self.client.post("/predict", json={
            "level": "level1",
            "courseCode": "DBMS",
            "marks": {"cat1": 44, "quiz1": 7},
            "attendancePercentage": 85,
            "previousCgpa": 8.2,
            "weeklyStudyHours": 10
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertEqual(data["level"], "level1")
        self.assertEqual(data["courseCode"], "DBMS")
        self.assertGreaterEqual(data["predictedOverallScore"], 0)
        self.assertLessEqual(data["predictedOverallScore"], 100)
        self.assertIn(data["likelyGrade"], ["S", "A", "B", "C", "D", "E", "F"])
        self.assertIn(data["riskLevel"], ["Low", "Medium", "High"])
        self.assertEqual(len(data["illustrativeRange"]), 2)
        # Verify level1 is now loaded in MODELS
        self.assertIn("level1", MODELS)

    def test_07_zero_marks_are_valid(self):
        # Legitimate 0 mark for cat1
        res = self.client.post("/predict", json={
            "level": "level1",
            "courseCode": "DBMS",
            "marks": {"cat1": 0, "quiz1": 0},
            "attendancePercentage": 50,
            "previousCgpa": 5.0,
            "weeklyStudyHours": 2
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertGreaterEqual(data["predictedOverallScore"], 0)

    def test_08_out_of_range_marks_rejected(self):
        # cat1 > 50
        res = self.client.post("/predict", json={
            "level": "level1",
            "courseCode": "DBMS",
            "marks": {"cat1": 55, "quiz1": 8}
        })
        self.assertEqual(res.status_code, 400)
        self.assertIn("cat1 must be between 0 and 50", res.get_json()["error"])

        # negative marks
        res = self.client.post("/predict", json={
            "level": "level1",
            "courseCode": "DBMS",
            "marks": {"cat1": -5, "quiz1": 8}
        })
        self.assertEqual(res.status_code, 400)
        self.assertIn("cat1 must be between 0 and 50", res.get_json()["error"])

    def test_09_fat_marks_strictly_rejected(self):
        res = self.client.post("/predict", json={
            "level": "level1",
            "courseCode": "DBMS",
            "marks": {"cat1": 35, "quiz1": 8, "fat": 80}
        })
        self.assertEqual(res.status_code, 400)
        self.assertIn("Do not send FAT", res.get_json()["error"])

    def test_10_level2_lazy_loads_and_succeeds(self):
        res = self.client.post("/predict", json={
            "level": "level2",
            "courseCode": "CN",
            "marks": {"cat1": 40, "cat2": 38, "quiz1": 9, "quiz2": 8},
            "attendancePercentage": 90,
            "previousCgpa": 8.5
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertEqual(data["level"], "level2")
        self.assertIn("level2", MODELS)

    def test_11_level3_missing_labs_rejected(self):
        # Level 3 with only cat1, cat2, quiz1, quiz2, quiz3 (missing 10 labs)
        res = self.client.post("/predict", json={
            "level": "level3",
            "courseCode": "OS",
            "marks": {"cat1": 42, "cat2": 45, "quiz1": 9, "quiz2": 9, "quiz3": 8}
        })
        self.assertEqual(res.status_code, 400)
        self.assertIn("requires marks for", res.get_json()["error"])
        self.assertIn("lab1", res.get_json()["error"])

    def test_12_level3_with_all_inputs_succeeds(self):
        marks = {"cat1": 42, "cat2": 45, "quiz1": 9, "quiz2": 9, "quiz3": 8}
        for i in range(1, 11):
            marks[f"lab{i}"] = 9.0
        res = self.client.post("/predict", json={
            "level": "level3",
            "courseCode": "OS",
            "marks": marks
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertEqual(data["level"], "level3")
        self.assertIn("level3", MODELS)

    def test_13_thread_safe_concurrent_requests(self):
        errors = []
        def worker(level, marks):
            try:
                c = app.test_client()
                r = c.post("/predict", json={"level": level, "courseCode": "DBMS", "marks": marks})
                if r.status_code != 200:
                    errors.append(f"Status {r.status_code}: {r.get_data(as_text=True)}")
            except Exception as e:
                errors.append(str(e))

        threads = []
        for _ in range(10):
            t1 = threading.Thread(target=worker, args=("level1", {"cat1": 40, "quiz1": 8}))
            t2 = threading.Thread(target=worker, args=("level2", {"cat1": 40, "cat2": 42, "quiz1": 8, "quiz2": 9}))
            threads.extend([t1, t2])

        for t in threads:
            t.start()
        for t in threads:
            t.join()

        self.assertEqual(len(errors), 0, f"Concurrent requests had errors: {errors}")

    def test_14_single_model_eviction_lifecycle(self):
        from app import reset_cache_for_testing
        reset_cache_for_testing()
        self.assertEqual(len(MODELS), 0)

        # 1. Level 1
        r1 = self.client.post("/predict", json={"level": "level1", "courseCode": "DBMS", "marks": {"cat1": 35, "quiz1": 7}})
        self.assertEqual(r1.status_code, 200)
        self.assertEqual(list(MODELS.keys()), ["level1"])

        # 2. Level 2 (evicts Level 1)
        r2 = self.client.post("/predict", json={"level": "level2", "courseCode": "DBMS", "marks": {"cat1": 35, "cat2": 38, "quiz1": 7, "quiz2": 8}})
        self.assertEqual(r2.status_code, 200)
        self.assertEqual(list(MODELS.keys()), ["level2"])
        self.assertNotIn("level1", MODELS)

        # 3. Level 3 (evicts Level 2)
        l3_marks = {"cat1": 35, "cat2": 38, "quiz1": 7, "quiz2": 8, "quiz3": 9}
        for i in range(1, 11): l3_marks[f"lab{i}"] = 8
        r3 = self.client.post("/predict", json={"level": "level3", "courseCode": "DBMS", "marks": l3_marks})
        self.assertEqual(r3.status_code, 200)
        self.assertEqual(list(MODELS.keys()), ["level3"])
        self.assertNotIn("level2", MODELS)

        # 4. Level 1 again (evicts Level 3)
        r4 = self.client.post("/predict", json={"level": "level1", "courseCode": "DBMS", "marks": {"cat1": 40, "quiz1": 8}})
        self.assertEqual(r4.status_code, 200)
        self.assertEqual(list(MODELS.keys()), ["level1"])
        self.assertNotIn("level3", MODELS)

    def test_15_concurrent_reader_protection_prevents_unloading(self):
        import time
        from app import use_model, reset_cache_for_testing
        reset_cache_for_testing()

        events = []
        errors = []

        def reader_level1():
            try:
                with use_model("level1") as (model, err):
                    self.assertIsNone(err)
                    events.append("level1_started")
                    # Simulate in-progress inference work while holding the active reader guard
                    time.sleep(0.4)
                    events.append("level1_working")
                    # Ensure model is still level1 and valid
                    self.assertIn("level1", MODELS)
                    events.append("level1_finished")
            except Exception as e:
                errors.append(f"reader1: {e}")

        def reader_level2():
            try:
                # Give reader_level1 time to start and acquire the reader guard
                time.sleep(0.1)
                events.append("level2_requesting")
                with use_model("level2") as (model, err):
                    self.assertIsNone(err)
                    events.append("level2_acquired")
                    self.assertIn("level2", MODELS)
                    self.assertNotIn("level1", MODELS)
                    events.append("level2_finished")
            except Exception as e:
                errors.append(f"reader2: {e}")

        t1 = threading.Thread(target=reader_level1)
        t2 = threading.Thread(target=reader_level2)

        t1.start()
        t2.start()

        t1.join()
        t2.join()

        self.assertEqual(errors, [], f"Active-reader synchronization errors: {errors}")
        # Verify execution order: level2_acquired MUST occur AFTER level1_finished
        self.assertIn("level1_started", events)
        self.assertIn("level1_finished", events)
        self.assertIn("level2_acquired", events)
        idx_l1_finished = events.index("level1_finished")
        idx_l2_acquired = events.index("level2_acquired")
        self.assertGreater(idx_l2_acquired, idx_l1_finished, "Level 2 acquired model before Level 1 finished inference!")

if __name__ == "__main__":
    unittest.main()

