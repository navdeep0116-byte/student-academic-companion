from contextlib import contextmanager
import gc
import json
import os
from pathlib import Path
import threading
import joblib
import pandas as pd
from flask import Flask, jsonify, request

BASE = Path(__file__).resolve().parent
MODEL_DIR = BASE / "models"
app = Flask(__name__)
COMMON = ["attendancePercentage", "previousCgpa", "weeklyStudyHours"]
LABS = [f"lab{i}" for i in range(1, 11)]
STAGES = {
    "level1": ["courseCode"] + COMMON + ["cat1", "quiz1"],
    "level2": ["courseCode"] + COMMON + ["cat1", "cat2", "quiz1", "quiz2"],
    "level3": ["courseCode"] + COMMON + ["cat1", "cat2", "quiz1", "quiz2", "quiz3"] + LABS,
}
REQUIRED = {
    "level1": ["cat1", "quiz1"],
    "level2": ["cat1", "cat2", "quiz1", "quiz2"],
    "level3": ["cat1", "cat2", "quiz1", "quiz2", "quiz3"] + LABS,
}
COURSES = {"DBMS", "CN", "STAT", "OOP", "DSA", "OS", "JAVA", "SE"}

# Single-model cache synchronization state
_cache_lock = threading.Lock()
_cache_condition = threading.Condition(_cache_lock)
_current_stage = None
_current_model = None
_active_readers = 0  # number of threads currently executing inference with _current_model

def get_loaded_models():
    with _cache_condition:
        return [_current_stage] if _current_stage else []

class _ModelsDictWrapper:
    """Compatibility wrapper exposing dictionary-like access to the single-model cache."""
    def keys(self):
        return get_loaded_models()
    def __contains__(self, key):
        return key in get_loaded_models()
    def __getitem__(self, key):
        with _cache_condition:
            if _current_stage == key and _current_model is not None:
                return _current_model
            raise KeyError(key)
    def __len__(self):
        return len(get_loaded_models())
    def clear(self):
        reset_cache_for_testing()

MODELS = _ModelsDictWrapper()

def reset_cache_for_testing():
    """Unloads any active model and forces garbage collection. Used for test isolation."""
    global _current_stage, _current_model, _active_readers
    with _cache_condition:
        while _active_readers > 0:
            _cache_condition.wait()
        _current_stage = None
        _current_model = None
        _active_readers = 0
        gc.collect()

def grade(score):
    if score >= 90: return "S"
    if score >= 80: return "A"
    if score >= 70: return "B"
    if score >= 60: return "C"
    if score >= 50: return "D"
    if score >= 40: return "E"
    return "F"

@contextmanager
def use_model(stage: str):
    """
    Thread-safe single-model cache context manager.
    Guarantees:
    1. At most ONE model is loaded in memory per worker process at any time.
    2. Active prediction requests protect the model from being evicted or unloaded
       while inference is actively running.
    3. If another thread requests a different model while active predictions are in-flight,
       it waits until active predictions complete, safely evicts the old model,
       runs gc.collect(), and loads the requested model.
    4. Concurrent requests for the same model execute simultaneously without reloading.
    """
    global _current_stage, _current_model, _active_readers

    if stage not in STAGES:
        yield None, "level must be level1, level2, or level3"
        return

    err = None
    model_to_use = None
    with _cache_condition:
        # Wait if another stage is loaded AND still actively in use by running predictions
        while _current_stage is not None and _current_stage != stage and _active_readers > 0:
            _cache_condition.wait()

        # If requested stage is not loaded, evict the old model and load the requested one
        if _current_stage != stage:
            if _current_model is not None:
                _current_model = None
                _current_stage = None
                gc.collect()

            path = MODEL_DIR / f"{stage}_overall_model.joblib"
            if not path.exists():
                err = f"Model file for '{stage}' is not available. Run train_model.py first."
            else:
                try:
                    loaded = joblib.load(path)
                    _current_model = loaded
                    _current_stage = stage
                except Exception as e:
                    err = f"Failed to load model file for '{stage}': {str(e)}"

        if not err:
            _active_readers += 1
            model_to_use = _current_model

    if err:
        yield None, err
        return

    try:
        yield model_to_use, None
    finally:
        with _cache_condition:
            _active_readers -= 1
            if _active_readers == 0:
                _cache_condition.notify_all()

@app.get("/health")
def health():
    return jsonify({
        "status": "UP",
        "modelsLoaded": get_loaded_models(),
        "maxCachedModels": 1,
        "target": "overall course score /100",
        "syntheticDemo": True,
    })

@app.get("/models")
@app.get("/model-availability")
def models_availability():
    """
    Reports model file availability on disk without eagerly loading model objects into RAM.
    """
    with _cache_condition:
        loaded_stage = _current_stage

    models_info = {}
    available_levels = []
    for stage in sorted(STAGES.keys()):
        path = MODEL_DIR / f"{stage}_overall_model.joblib"
        exists = path.exists()
        if exists:
            available_levels.append(stage)
        models_info[stage] = {
            "available": exists,
            "filename": f"{stage}_overall_model.joblib",
            "sizeBytes": path.stat().st_size if exists else 0,
            "isLoaded": (stage == loaded_stage),
            "requiredInputs": REQUIRED[stage],
        }
    return jsonify({
        "availableLevels": available_levels,
        "models": models_info,
        "currentlyLoaded": loaded_stage,
        "maxCachedModels": 1,
    })

@app.get("/model-info")
def model_info():
    path = MODEL_DIR / "evaluation_report.json"
    report = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    return jsonify({
        "stages": STAGES,
        "grading": {
            "cat1": "raw /50 -> 15 theory points",
            "cat2": "raw /50 -> 15 theory points",
            "quizzes": "each raw /10 -> 10 theory points",
            "fat": "raw /100 -> 40 theory points",
            "labs": "10 assessments /10 each -> lab score /100",
            "overall": "0.75 * theory score + 0.25 * lab score",
        },
        "evaluation": report,
        "metricsCaveat": "Synthetic-data metrics only.",
    })

@app.post("/predict")
def predict():
    payload = request.get_json(silent=True) or {}
    stage = payload.get("level")
    if stage not in STAGES:
        return jsonify({"error": "level must be level1, level2, or level3"}), 400

    marks = payload.get("marks") or {}
    if any(k.lower() in {"fat", "fatmarks", "overallscore", "theoryscore", "labscore"} for k in marks):
        return jsonify({"error": "Do not send FAT or derived final scores to a pre-FAT prediction."}), 400

    course = str(payload.get("courseCode", "DBMS")).upper()
    if course not in COURSES:
        return jsonify({"error": f"Unknown courseCode. Use one of {sorted(COURSES)}"}), 400

    values = {
        "courseCode": course,
        "attendancePercentage": payload.get("attendancePercentage", 80),
        "previousCgpa": payload.get("previousCgpa", 7.0),
        "weeklyStudyHours": payload.get("weeklyStudyHours", 8.0),
    }
    values.update({k: marks.get(k) for k in ["cat1", "cat2", "quiz1", "quiz2", "quiz3"] + LABS})

    missing = [k for k in REQUIRED[stage] if values[k] is None]
    if missing:
        return jsonify({"error": f"{stage} requires marks for: {', '.join(missing)}"}), 400

    limits = {
        "cat1": 50,
        "cat2": 50,
        "quiz1": 10,
        "quiz2": 10,
        "quiz3": 10,
        **{k: 10 for k in LABS},
        "attendancePercentage": 100,
        "previousCgpa": 10,
        "weeklyStudyHours": 80,
    }
    for key, val in values.items():
        if key == "courseCode" or val is None:
            continue
        try:
            values[key] = float(val)
        except (TypeError, ValueError):
            return jsonify({"error": f"{key} must be numeric"}), 400
        if values[key] < 0 or values[key] > limits[key]:
            return jsonify({"error": f"{key} must be between 0 and {limits[key]}"}), 400

    with use_model(stage) as (model_data, err):
        if err:
            status_code = 503 if "not available" in err.lower() or "not trained" in err.lower() else 400
            return jsonify({"error": err}), status_code

        X = pd.DataFrame([{k: values.get(k) for k in STAGES[stage]}])
        score = round(float(max(0, min(100, model_data["model"].predict(X)[0]))), 2)

    letter = grade(score)
    band = 15 if stage == "level1" else 11 if stage == "level2" else 8
    return jsonify({
        "level": stage,
        "courseCode": course,
        "predictedOverallScore": score,
        "likelyGrade": letter,
        "riskLevel": "High" if score < 50 else "Medium" if score < 65 else "Low",
        "illustrativeRange": [round(max(0, score - band), 2), round(min(100, score + band), 2)],
        "featuresUsed": STAGES[stage],
        "grading": {
            "theoryWeight": 0.75,
            "labWeight": 0.25,
            "cat1Max": 50,
            "cat2Max": 50,
            "quizMax": 10,
            "labAssessments": 10,
            "labAssessmentMax": 10,
        },
        "warning": "Synthetic-data demonstration only. Range is illustrative, not a calibrated confidence interval.",
    })

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port, debug=False)
