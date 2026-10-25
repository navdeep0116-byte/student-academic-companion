from pathlib import Path
import json
import joblib
import pandas as pd
from flask import Flask, jsonify, request

BASE = Path(__file__).resolve().parent
MODEL_DIR = BASE / "models"
app = Flask(__name__)
COMMON = ["attendancePercentage", "previousCgpa", "weeklyStudyHours"]
LABS = [f"lab{i}" for i in range(1,11)]
STAGES = {
    "level1": ["courseCode"] + COMMON + ["cat1","quiz1"],
    "level2": ["courseCode"] + COMMON + ["cat1","cat2","quiz1","quiz2"],
    "level3": ["courseCode"] + COMMON + ["cat1","cat2","quiz1","quiz2","quiz3"] + LABS,
}
REQUIRED = {
    "level1": ["cat1","quiz1"],
    "level2": ["cat1","cat2","quiz1","quiz2"],
    "level3": ["cat1","cat2","quiz1","quiz2","quiz3"],
}
COURSES = {"DBMS","CN","STAT","OOP","DSA","OS","JAVA","SE"}
MODELS = {}

def grade(score):
    if score >= 90: return "S"
    if score >= 80: return "A"
    if score >= 70: return "B"
    if score >= 60: return "C"
    if score >= 50: return "D"
    if score >= 40: return "E"
    return "F"

def load_models():
    for stage in STAGES:
        path = MODEL_DIR / f"{stage}_overall_model.joblib"
        if path.exists():
            MODELS[stage] = joblib.load(path)

@app.get("/health")
def health():
    return jsonify({"status":"UP","modelsLoaded":sorted(MODELS.keys()),"target":"overall course score /100","syntheticDemo":True})

@app.get("/model-info")
def model_info():
    path = MODEL_DIR / "evaluation_report.json"
    report = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    return jsonify({"stages":STAGES,"grading":{"cat1":"raw /50 -> 15 theory points","cat2":"raw /50 -> 15 theory points","quizzes":"each raw /10 -> 10 theory points","fat":"raw /100 -> 40 theory points","labs":"10 assessments /10 each -> lab score /100","overall":"0.75 * theory score + 0.25 * lab score"},"evaluation":report,"metricsCaveat":"Synthetic-data metrics only."})

@app.post("/predict")
def predict():
    payload = request.get_json(silent=True) or {}
    stage = payload.get("level")
    if stage not in STAGES:
        return jsonify({"error":"level must be level1, level2, or level3"}), 400
    if stage not in MODELS:
        return jsonify({"error":f"Model {stage} is not trained. Run python train_model.py first."}), 503
    marks = payload.get("marks") or {}
    if any(k.lower() in {"fat","fatmarks","overallscore","theoryscore","labscore"} for k in marks):
        return jsonify({"error":"Do not send FAT or derived final scores to a pre-FAT prediction."}), 400
    course = str(payload.get("courseCode","DBMS")).upper()
    if course not in COURSES:
        return jsonify({"error":f"Unknown courseCode. Use one of {sorted(COURSES)}"}), 400
    values = {"courseCode":course,
              "attendancePercentage":payload.get("attendancePercentage",80),
              "previousCgpa":payload.get("previousCgpa",7.0),
              "weeklyStudyHours":payload.get("weeklyStudyHours",8.0)}
    values.update({k:marks.get(k) for k in ["cat1","cat2","quiz1","quiz2","quiz3"] + LABS})
    missing = [k for k in REQUIRED[stage] if values[k] is None]
    if missing:
        return jsonify({"error":f"{stage} requires marks for: {', '.join(missing)}"}), 400
    limits = {"cat1":50,"cat2":50,"quiz1":10,"quiz2":10,"quiz3":10,**{k:10 for k in LABS},
              "attendancePercentage":100,"previousCgpa":10,"weeklyStudyHours":80}
    for key, val in values.items():
        if key == "courseCode" or val is None: continue
        try: values[key] = float(val)
        except (TypeError,ValueError): return jsonify({"error":f"{key} must be numeric"}),400
        if values[key] < 0 or values[key] > limits[key]:
            return jsonify({"error":f"{key} must be between 0 and {limits[key]}"}),400
    X = pd.DataFrame([{k:values.get(k) for k in STAGES[stage]}])
    score = round(float(max(0,min(100,MODELS[stage]["model"].predict(X)[0]))),2)
    # Derive an estimated letter grade from predicted overall course score.
    letter = grade(score)
    band = 15 if stage == "level1" else 11 if stage == "level2" else 8
    return jsonify({"level":stage,"courseCode":course,"predictedOverallScore":score,
                    "likelyGrade":letter,"riskLevel":"High" if score < 50 else "Medium" if score < 65 else "Low",
                    "illustrativeRange":[round(max(0,score-band),2),round(min(100,score+band),2)],
                    "featuresUsed":STAGES[stage],
                    "grading":{"theoryWeight":0.75,"labWeight":0.25,"cat1Max":50,"cat2Max":50,"quizMax":10,"labAssessments":10,"labAssessmentMax":10},
                    "warning":"Synthetic-data demonstration only. Range is illustrative, not a calibrated confidence interval."})

load_models()
if __name__ == "__main__":
    if not MODELS: print("No corrected models found. Run generate_dataset.py then train_model.py.")
    app.run(host="127.0.0.1",port=5000,debug=False)
