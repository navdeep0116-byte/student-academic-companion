from pathlib import Path
import json
import joblib
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.ensemble import RandomForestRegressor

BASE = Path(__file__).resolve().parent
MODEL_DIR = BASE / "models"
MODEL_DIR.mkdir(exist_ok=True)

# Stage-specific feature sets. All scores are predicted overall course score /100.
COMMON = ["courseCode", "attendancePercentage", "previousCgpa", "weeklyStudyHours"]
LABS = [f"lab{i}" for i in range(1, 11)]
STAGES = {
    "level1": COMMON + ["cat1", "quiz1"],
    "level2": COMMON + ["cat1", "cat2", "quiz1", "quiz2"],
    "level3": COMMON + ["cat1", "cat2", "quiz1", "quiz2", "quiz3"] + LABS,
}

def main():
    path = BASE / "historical_training_data.csv"
    if not path.exists():
        raise SystemExit("Dataset not found. Run: python generate_dataset.py")
    df = pd.read_csv(path)
    reports = {}
    for stage, features in STAGES.items():
        X, y = df[features].copy(), df["overallScore"].copy()
        numeric = [c for c in features if c != "courseCode"]
        preprocess = ColumnTransformer([
            ("numeric", Pipeline([("imputer", SimpleImputer(strategy="median")), ("scale", StandardScaler())]), numeric),
            ("course", OneHotEncoder(handle_unknown="ignore"), ["courseCode"]),
        ])
        model = Pipeline([
            ("preprocess", preprocess),
            ("regressor", RandomForestRegressor(n_estimators=200, min_samples_leaf=3, max_features=0.9, random_state=42, n_jobs=-1)),
        ])
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        model.fit(X_train, y_train)
        pred = model.predict(X_test).clip(0, 100)
        report = {
            "stage": stage, "target": "overallScore /100", "features": features,
            "trainRows": int(len(X_train)), "testRows": int(len(X_test)),
            "mae": round(float(mean_absolute_error(y_test, pred)),3),
            "rmse": round(float(mean_squared_error(y_test, pred)**0.5),3),
            "r2": round(float(r2_score(y_test, pred)),4),
            "note": "Synthetic-data evaluation only; not real-world validated."
        }
        joblib.dump({"model": model, "features": features, "stage": stage, "target": "overallScore"}, MODEL_DIR / f"{stage}_overall_model.joblib")
        reports[stage] = report
        print(f"{stage}: overall-score MAE={report['mae']}, RMSE={report['rmse']}, R2={report['r2']}")
    (MODEL_DIR / "evaluation_report.json").write_text(json.dumps(reports, indent=2), encoding="utf-8")
    print(f"Saved corrected overall-score models and evaluation report in {MODEL_DIR}")

if __name__ == "__main__":
    main()
