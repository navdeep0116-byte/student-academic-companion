from pathlib import Path
import numpy as np
import pandas as pd

OUT = Path(__file__).resolve().parent
RNG = np.random.default_rng(20261019)
N_STUDENTS = 5000
COURSES = [
    ("DBMS", "Database Systems"), ("CN", "Computer Networks"),
    ("STAT", "Statistics for Engineers"), ("OOP", "Object Oriented Programming"),
    ("DSA", "Data Structures and Algorithms"), ("OS", "Operating Systems"),
    ("JAVA", "Java Programming"), ("SE", "Software Engineering"),
]

def clip(v, low, high):
    return np.clip(v, low, high)

def grade(score):
    if score >= 90: return "S"
    if score >= 80: return "A"
    if score >= 70: return "B"
    if score >= 60: return "C"
    if score >= 50: return "D"
    if score >= 40: return "E"
    return "F"

def main():
    rows, students = [], []
    for si in range(1, N_STUDENTS + 1):
        sid = f"SYN{si:05d}"
        ability = float(clip(RNG.normal(65, 14), 15, 98))
        attendance = float(clip(RNG.normal(80 + (ability - 65)*0.25, 12), 30, 100))
        cgpa = float(clip(ability / 10 + RNG.normal(0, .45), 2, 10))
        study_hours = float(clip(RNG.normal(8 + (ability - 60)*.12, 4), 0, 30))
        students.append({"studentId": sid, "synthetic": True, "previousCgpa": round(cgpa,2),
                         "attendancePercentage": round(attendance,1), "weeklyStudyHours": round(study_hours,1)})
        for code, name in COURSES:
            difficulty = float(RNG.normal(0, 6))
            latent = float(clip(ability - difficulty, 5, 100))
            # Raw marks respect the actual maxima: CAT /50, quizzes /10, labs /10, FAT /100.
            cat1 = float(clip(RNG.normal(latent*.50, 7), 0, 50))
            cat2 = float(clip(RNG.normal(latent*.50 + (cat1-latent*.50)*.30, 7), 0, 50))
            quiz1 = float(clip(RNG.normal(latent*.10, 1.7), 0, 10))
            quiz2 = float(clip(RNG.normal(latent*.10 + (quiz1-latent*.10)*.25, 1.6), 0, 10))
            quiz3 = float(clip(RNG.normal(latent*.10 + ((quiz1+quiz2)/2-latent*.10)*.20, 1.6), 0, 10))
            labs = [float(clip(RNG.normal(latent*.10, 1.5), 0, 10)) for _ in range(10)]
            # Generate a final FAT target from underlying ability and observable progress,
            # without using FAT as a feature in any stage model.
            theory_progress = (
                cat1 * .30 + cat2 * .30 + quiz1 + quiz2 + quiz3
            ) / 35.0
            lab_progress = float(np.mean(labs)) / 10.0
            fat = float(clip(0.45*latent + 35*theory_progress + 20*lab_progress + RNG.normal(0, 9), 0, 100))
            theory = cat1*.30 + cat2*.30 + quiz1 + quiz2 + quiz3 + fat*.40
            lab_score = sum(labs)  # out of 100
            overall = .75*theory + .25*lab_score
            row = {
                "studentId": sid, "courseCode": code, "courseName": name,
                "cat1": round(cat1,2), "cat2": round(cat2,2),
                "quiz1": round(quiz1,2), "quiz2": round(quiz2,2), "quiz3": round(quiz3,2),
                **{f"lab{i+1}": round(labs[i],2) for i in range(10)},
                "attendancePercentage": round(attendance,1), "previousCgpa": round(cgpa,2),
                "weeklyStudyHours": round(study_hours,1), "fatMarks": round(fat,2),
                "theoryScore": round(theory,2), "labScore": round(lab_score,2),
                "overallScore": round(overall,2), "finalGrade": grade(overall), "synthetic": True
            }
            rows.append(row)
    df = pd.DataFrame(rows)
    df.to_csv(OUT / "historical_training_data.csv", index=False)
    pd.DataFrame(students).to_csv(OUT / "synthetic_students.csv", index=False)

    # 400 current-semester demo student-course records, no FAT or derived final score.
    live = df.sample(n=400, random_state=42).copy()
    rng = np.random.default_rng(1234)
    live["predictionStage"] = rng.choice(["level1", "level2", "level3"], size=len(live), p=[.34,.33,.33])
    for idx, row in live.iterrows():
        stage = row["predictionStage"]
        if stage == "level1":
            for col in ["cat2","quiz2","quiz3"] + [f"lab{i}" for i in range(1,11)]:
                live.at[idx, col] = np.nan
        elif stage == "level2":
            for col in ["quiz3"] + [f"lab{i}" for i in range(1,11)]:
                live.at[idx, col] = np.nan
        else:
            # Level 3 represents all pre-FAT theory marks and all lab assessments available;
            # simulate some labs as not yet completed.
            labcols = [f"lab{i}" for i in range(1,11)]
            missing_count = int(rng.integers(0,4))
            for col in rng.choice(labcols, size=missing_count, replace=False):
                live.at[idx, col] = np.nan
    live = live.drop(columns=["fatMarks","theoryScore","labScore","overallScore","finalGrade"])
    live.to_csv(OUT / "current_semester_demo_students.csv", index=False)
    print(f"Generated {N_STUDENTS:,} synthetic students and {len(df):,} complete student-course records.")
    print("Historical records include all 10 lab marks and FAT target; current-semester records exclude FAT and final outcomes.")
    print("Grading: CAT1/CAT2 /50 -> 15 each; quizzes /10 -> 10 each; FAT /100 -> 40; labs 10x/10 -> lab /100; overall = 75% theory + 25% lab.")

if __name__ == "__main__":
    main()
