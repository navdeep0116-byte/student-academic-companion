# Student Academic Companion — Corrected ML Service

This version models the course grading rules supplied by the project owner.

## Grading
- CAT 1: raw score /50, contributes 15 points to theory score /100.
- CAT 2: raw score /50, contributes 15 points to theory score /100.
- Quiz 1, Quiz 2, Quiz 3: raw score /10 each, each contributes 10 points to theory score /100.
- FAT: raw score /100, contributes 40 points to theory score /100.
- Theory score = CAT1*(15/50) + CAT2*(15/50) + Q1 + Q2 + Q3 + FAT*(40/100), maximum 100.
- Lab: 10 assessments, each /10. Lab score /100 = sum(lab1..lab10), and lab contribution to overall = lab score * 0.25.
- Overall score /100 = theory score * 0.75 + lab score * 0.25.

## Three prediction stages
- level1: CAT1 + Quiz1 only, plus optional known lab marks and non-assessment context.
- level2: CAT1 + CAT2 + Quiz1 + Quiz2, plus optional known lab marks and non-assessment context.
- level3: CAT1 + CAT2 + all three quizzes + all available completed lab assessments. FAT is never an input.

For fairness, the API requires the stage-specific theory marks. Missing lab assessments are allowed and treated as unavailable; the model imputes them. Current-semester data should omit FAT entirely.

## Setup (Windows PowerShell)
From this folder:
```powershell
py -3.13 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python generate_dataset.py
python train_model.py
python app.py
```
If `.venv` already exists, activate it instead of recreating it.

## Test health
```powershell
Invoke-RestMethod http://127.0.0.1:5000/health
```

## Test Level 1
```powershell
$body = @{
  level = "level1"
  courseCode = "DBMS"
  marks = @{ cat1 = 36; quiz1 = 7; lab1 = 8; lab2 = 7 }
} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:5000/predict" -ContentType "application/json" -Body $body
```

## Test Level 2
```powershell
$body = @{
  level = "level2"
  courseCode = "DBMS"
  marks = @{ cat1 = 36; cat2 = 39; quiz1 = 7; quiz2 = 8; lab1 = 8; lab2 = 7 }
} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:5000/predict" -ContentType "application/json" -Body $body
```

## Test Level 3
```powershell
$body = @{
  level = "level3"
  courseCode = "DBMS"
  marks = @{ cat1 = 36; cat2 = 39; quiz1 = 7; quiz2 = 8; quiz3 = 9; lab1 = 8; lab2 = 7; lab3 = 8; lab4 = 9; lab5 = 8; lab6 = 7; lab7 = 8; lab8 = 9; lab9 = 8; lab10 = 7 }
} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:5000/predict" -ContentType "application/json" -Body $body
```

All generated records and metrics are synthetic. They demonstrate the workflow and are not evidence of real-world predictive accuracy. `historical_training_data.csv` includes the FAT target for training only; do not use it as current student input.
