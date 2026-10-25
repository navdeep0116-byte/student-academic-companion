# Student Academic Companion — Demo Data Seeder

## What this does
- Adds synthetic demo records to the existing `student_academic_companion` database.
- Includes students, teachers, courses, semesters, classes, enrollments, assessments, assignments, attendance, results, predictions, recommendations, study sessions, submissions, and teaching assignments.
- Leaves future FAT marks pending/null so the example demonstrates prediction from partial CAT/quiz/assignment/lab data.
- Does not drop collections or delete non-demo records. Re-running removes only records marked `demoSeed: "sac-demo-v1"` from the collections it seeds.

## Important before running
I inspected the uploaded backend. It currently has `User` and `Student` models, and `/api/student/dashboard` only returns profile fields; it does not yet query courses, results, attendance, or predictions. The seed file will populate MongoDB, but additional backend API work is still required before these seeded academic records appear on the dashboard.
The schema for most academic collections is not defined by Java models in the uploaded backend, so the script uses a consistent synthetic document structure based on the existing collection names. Review it before using it for anything other than a demo.

## Run using mongosh (Windows PowerShell)
1. Download/install MongoDB Shell (`mongosh`) if not already installed.
2. Ensure your MongoDB connection string is available in the PowerShell session as `$env:MONGODB_URI`. Do not paste it into chat or commit it to Git.
3. Put `seed-demo-data.js` in a folder where you can open PowerShell.
4. Run:
   ```powershell
   mongosh "$env:MONGODB_URI" --file .\seed-demo-data.js
   ```
   The URI must connect to the Atlas cluster and have write access to `student_academic_companion`.
5. Read the printed counts and confirm the script reports successful inserts.
6. In Atlas, open `student_academic_companion` → Browse Collections to inspect the new documents.

## Demo credentials
The script assigns a common demo password `password` to seeded user records using a BCrypt hash. This is for local presentation only. Do not use these credentials in production.
The existing authentication code only permits student login through its current login endpoint. Teacher/admin account login and dashboards need separate backend authorization/controller work; inserting teacher/admin records alone does not make those flows work.

## Safety
- Do not run against production data.
- Back up the database first if it contains anything important.
- The script never drops a collection, but it does delete previous seed-tagged documents to make re-running repeatable.
- All records are fabricated examples and must be labelled as synthetic/demo data in presentations.
