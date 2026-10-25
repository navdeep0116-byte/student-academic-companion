/*
 Student Academic Companion — synthetic demo data seeder
 Run with mongosh against database student_academic_companion:
   mongosh "$env:MONGODB_URI" --file .\seed-demo-data.js   (PowerShell syntax for URI may vary)
 SAFETY: only deletes documents carrying demoSeed: true in collections this script seeds.
 All records are synthetic. Does not drop collections or modify existing non-demo records.
*/
const dbName = "student_academic_companion";
const d = db.getSiblingDB(dbName);
const now = new Date();
const seedTag = "sac-demo-v1";

// Remove only prior records created by this seed script; preserve all other data.
[
  "enrollments","courses","semesters","assessments","assignments","attendance",
  "results","predictions","recommendations","study_sessions","submissions",
  "teaching_assignments","classes","teachers","students","users"
].forEach(c => d.getCollection(c).deleteMany({ demoSeed: seedTag }));

const oid = () => new ObjectId();
const firstNames = ["Aarav","Ananya","Vihaan","Diya","Aditya","Isha","Kabir","Meera","Arjun","Saanvi","Reyansh","Aditi","Krishna","Myra","Ishaan","Sara","Rohan","Avni","Dev","Kiara","Kunal","Tara","Nikhil","Riya","Manav","Anika","Yash","Pihu","Dhruv","Naina"];
const lastNames = ["Sharma","Verma","Gupta","Singh","Patel","Reddy","Iyer","Mehta","Kapoor","Nair"];
const teachersInfo = [
  ["Dr. Priya Sharma","priya.sharma.demo@example.com","Computer Science"],
  ["Prof. Arjun Rao","arjun.rao.demo@example.com","Mathematics"],
  ["Dr. Neha Iyer","neha.iyer.demo@example.com","Computer Science"],
  ["Prof. Karan Mehta","karan.mehta.demo@example.com","Electronics"],
  ["Dr. Sara Khan","sara.khan.demo@example.com","Statistics"]
];

// Existing user's data is not altered. Seeded demo accounts use a known BCrypt hash
// for the password "password"; change it before any real deployment.
// This hash is commonly compatible with Spring BCryptPasswordEncoder.
const demoPasswordHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
const users = [];
const students = [];
const teachers = [];
const studentUserIds = [];

teachersInfo.forEach((t, i) => {
  const id = oid();
  users.push({_id:id, fullName:t[0], email:t[1], passwordHash:demoPasswordHash, role:"TEACHER", status:"ACTIVE", createdAt:now, demoSeed:seedTag});
  teachers.push({_id:oid(), userId:id, employeeNumber:`TCH${String(i+1).padStart(3,"0")}`, fullName:t[0], department:t[2], designation:i%2?"Assistant Professor":"Associate Professor", email:t[1], createdAt:now, demoSeed:seedTag});
});

const existingDemoUser = d.users.findOne({email:"demo.student2@example.com"});
let currentStudentId = null;
if (existingDemoUser && existingDemoUser._id) {
  const existingStudent = d.students.findOne({userId: existingDemoUser._id});
  if (existingStudent) currentStudentId = existingStudent._id;
}
for (let i=0; i<30; i++) {
  let userId, email, fullName;
  if (i===0 && existingDemoUser) {
    userId = existingDemoUser._id;
    email = existingDemoUser.email;
    fullName = existingDemoUser.fullName || "Demo Student 2";
  } else {
    userId = oid();
    fullName = `${firstNames[i]} ${lastNames[(i*3+2)%lastNames.length]}`;
    email = `student${String(i+1).padStart(2,"0")}.demo@example.com`;
    users.push({_id:userId, fullName, email, passwordHash:demoPasswordHash, role:"STUDENT", status:"ACTIVE", createdAt:now, demoSeed:seedTag});
  }
  studentUserIds.push(userId);
  const existingProfile = d.students.findOne({userId});
  const studentId = existingProfile ? existingProfile._id : oid();
  if (!existingProfile) {
    students.push({_id:studentId, userId, studentNumber:`SACDEMO${String(i+1).padStart(3,"0")}`, fullName, program:"B.Tech", department:"Computer Science and Engineering", yearOfStudy:2, section:["A","B","C"][i%3], email, createdAt:now, demoSeed:seedTag});
  }
}

if (users.length) d.users.insertMany(users, {ordered:false});
if (students.length) d.students.insertMany(students, {ordered:false});
if (teachers.length) d.teachers.insertMany(teachers, {ordered:false});

const teacherDocs = d.teachers.find({demoSeed:seedTag}).toArray();
const courseInfo = [
 ["BCSE203","Database Systems",4,"THEORY"],
 ["BCSE204","Data Structures and Algorithms",4,"THEORY"],
 ["BAMAT207","Statistics for Engineers",3,"THEORY"],
 ["BCSE205","Object Oriented Programming",3,"EMBEDDED"],
 ["BCSE206","Computer Networks",3,"THEORY"],
 ["BCSE207","Operating Systems",4,"THEORY"],
 ["BCSE208","Database Systems Lab",2,"LAB"],
 ["BCSE209","Web Technologies Lab",2,"LAB"]
];
const semesterId = oid();
d.semesters.insertOne({_id:semesterId, name:"Fall Semester 2026", code:"FALL-2026", academicYear:"2025–26", term:"FALL", startDate:new Date("2026-07-01"), endDate:new Date("2026-12-15"), status:"ACTIVE", demoSeed:seedTag});
const courseDocs = courseInfo.map((c,i)=>({_id:oid(), code:c[0], name:c[1], title:c[1], credits:c[2], evaluationType:c[3], semesterId, semester:"Fall Semester 2026", department:"Computer Science and Engineering", teacherId:teacherDocs[i%teacherDocs.length]?teacherDocs[i%teacherDocs.length]._id:null, status:"ACTIVE", createdAt:now, demoSeed:seedTag}));
d.courses.insertMany(courseDocs);
const classes = courseDocs.map((c,i)=>({_id:oid(), courseId:c._id, courseCode:c.code, courseName:c.name, semesterId, section:["A","B","C"][i%3], capacity:60, teacherId:c.teacherId, demoSeed:seedTag}));
d.classes.insertMany(classes);
d.teaching_assignments.insertMany(courseDocs.map((c,i)=>({_id:oid(), teacherId:c.teacherId, courseId:c._id, classId:classes[i]._id, semesterId, assignedAt:now, demoSeed:seedTag})));

const studentDocs = d.students.find({$or:[{demoSeed:seedTag},{userId:{$in:studentUserIds}}]}).toArray();
const enrollments = [], assessments = [], assignments = [], attendance = [], results = [], predictions = [], recommendations = [], studySessions = [], submissions = [];
const assessmentTypes = [
 ["CAT 1","CAT",20],["CAT 2","CAT",20],["Quiz 1","QUIZ",10],["Assignment 1","ASSIGNMENT",10],["Lab Assessment 1","LAB",20],["FAT","FAT",100]
];
studentDocs.forEach((s,si)=>{
  const profileIndex = si % 30;
  courseDocs.forEach((c,ci)=>{
    const enrollmentId=oid();
    enrollments.push({_id:enrollmentId, studentId:s._id, userId:s.userId, courseId:c._id, courseCode:c.code, semesterId, status:"ENROLLED", enrolledAt:now, demoSeed:seedTag});
    const availableStage = profileIndex < 10 ? 2 : profileIndex < 20 ? 4 : 5;
    assessmentTypes.forEach((a,ai)=>{
      const available = ai===0 || ai===1 || (ai===2 && availableStage>=3) || (ai===3 && availableStage>=4) || (ai===4 && availableStage>=5);
      const assessId = oid();
      if (ai < 5) assessments.push({_id:assessId, courseId:c._id, courseCode:c.code, semesterId, title:a[0], name:a[0], type:a[1], maxMarks:a[2], dueDate:new Date(Date.UTC(2026, 7+ai, 10)), status:available?"COMPLETED":"PENDING", demoSeed:seedTag});
      const obtained = Math.max(0, Math.min(a[2], Math.round((42 + ((profileIndex*17+ci*11+ai*7)%55)) / 100 * a[2])));
      const status = ai===5 ? "PENDING" : available ? "COMPLETED" : "PENDING";
      results.push({_id:oid(), studentId:s._id, userId:s.userId, courseId:c._id, courseCode:c.code, assessmentId:assessId, assessmentName:a[0], assessmentType:a[1], obtainedMarks:status==="COMPLETED"?obtained:null, maxMarks:a[2], status, grade:status==="COMPLETED"?(obtained/a[2]>=0.9?"S":obtained/a[2]>=0.8?"A":obtained/a[2]>=0.7?"B":obtained/a[2]>=0.6?"C":"D"):null, semesterId, demoSeed:seedTag});
      if (a[1]==="ASSIGNMENT") assignments.push({_id:oid(), courseId:c._id, studentId:s._id, title:"Assignment 1", maxMarks:a[2], obtainedMarks:status==="COMPLETED"?obtained:null, status, dueDate:new Date("2026-10-30"), demoSeed:seedTag});
      if (a[1]==="LAB") submissions.push({_id:oid(), assessmentId:assessId, studentId:s._id, courseId:c._id, submitted:status==="COMPLETED", marks:status==="COMPLETED"?obtained:null, status, demoSeed:seedTag});
    });
    const conducted=36+(ci%5)*3;
    const percent=Math.max(55,Math.min(100,58+((profileIndex*13+ci*9)%43)));
    const attended=Math.round(conducted*percent/100);
    attendance.push({_id:oid(), studentId:s._id, userId:s.userId, courseId:c._id, courseCode:c.code, semesterId, classesConducted:conducted, classesAttended:attended, classesMissed:conducted-attended, attendancePercentage:Math.round(attended/conducted*1000)/10, updatedAt:now, demoSeed:seedTag});
  });
  const overall = 48 + ((profileIndex*19)%49);
  const risk = overall<60?"HIGH":overall<75?"MEDIUM":"LOW";
  predictions.push({_id:oid(), studentId:s._id, userId:s.userId, semesterId, predictedFinalPercentage:overall, predictedGrade:overall>=90?"S":overall>=80?"A":overall>=70?"B":overall>=60?"C":"D", riskLevel:risk, modelType:"DEMO_RULE_BASED_NOT_ML", inputStage:profileIndex<10?"CAT_1_AND_CAT_2":profileIndex<20?"MID_SEMESTER":"PARTIAL_LABS", actualFatMarks:null, predictionDate:now, demoSeed:seedTag});
  recommendations.push({_id:oid(), studentId:s._id, semesterId, riskLevel:risk, title:risk==="HIGH"?"Academic recovery plan":risk==="MEDIUM"?"Improve consistency":"Maintain current progress", message:risk==="HIGH"?"Prioritise low-scoring courses, attend upcoming classes, and meet the instructor.":risk==="MEDIUM"?"Complete pending assignments and practise weak topics before FAT.":"Keep a regular study schedule and revise before assessments.", suggestedActions:risk==="HIGH"?["Meet course instructor","Complete pending work","Attend all upcoming classes"]:risk==="MEDIUM"?["Schedule weekly revision","Submit assignments on time"]:["Maintain study routine","Attempt practice questions"], demoSeed:seedTag});
  studySessions.push({_id:oid(), studentId:s._id, courseId:courseDocs[profileIndex%courseDocs.length]._id, date:new Date(Date.UTC(2026,9,1+profileIndex%15)), durationMinutes:30+(profileIndex%5)*20, topic:courseInfo[profileIndex%courseInfo.length][1], notes:"Synthetic demo study session", demoSeed:seedTag});
});
d.enrollments.insertMany(enrollments);
d.assessments.insertMany(assessments);
d.assignments.insertMany(assignments);
d.attendance.insertMany(attendance);
d.results.insertMany(results);
d.predictions.insertMany(predictions);
d.recommendations.insertMany(recommendations);
d.study_sessions.insertMany(studySessions);
d.submissions.insertMany(submissions);

print("Synthetic demo data seeded into " + dbName);
["users","students","teachers","courses","semesters","classes","enrollments","assessments","assignments","attendance","results","predictions","recommendations","study_sessions","submissions","teaching_assignments"].forEach(c=>print(c + ": " + d.getCollection(c).countDocuments({demoSeed:seedTag})));
print("Demo account password for seeded user records: password");
print("Reminder: existing Spring Boot APIs currently expose only student profile; this seed alone does not connect all records to dashboard endpoints.");
