export type EvaluationType = "THEORY" | "LAB" | "EMBEDDED"
export type AssessmentStatus = "COMPLETED" | "PENDING"

export type AssessmentRecord = {
  id: string
  type: string
  label: string
  maxMarks: number
  obtainedMarks: number | null
  status: AssessmentStatus
  date?: string
}

export type TheoryEvaluationSettings = {
  cat1Weight: number
  cat2Weight: number
  quizzesWeight: number
  fatWeight: number
  provisional: boolean
}

export type CourseRecord = {
  code: string
  name: string
  evaluationType: EvaluationType
  mark: number | null
  attendance: number
  attended: number
  conducted: number
  grade: string | null
  points: number
  credits: number
  assessments: { cat1: number; cat2: number; assignments: number; quizzes: number; fat: number }
  assessmentRecords: AssessmentRecord[]
  theorySettings?: TheoryEvaluationSettings
  theoryScore?: number | null
  labScore?: number | null
  combinedScore?: number | null
}

const theorySettings: TheoryEvaluationSettings = { cat1Weight: 15, cat2Weight: 15, quizzesWeight: 15, fatWeight: 30, provisional: true }
const theoryAssessments = (code: string, values: [number, number, number, number, number]): AssessmentRecord[] => [
  { id: `${code}-cat1`, type: "CAT", label: "CAT 1", maxMarks: 50, obtainedMarks: values[0], status: "COMPLETED", date: "2025-09-12" },
  { id: `${code}-cat2`, type: "CAT", label: "CAT 2", maxMarks: 50, obtainedMarks: values[1], status: "COMPLETED", date: "2025-10-10" },
  { id: `${code}-q1`, type: "QUIZ", label: "Quiz 1", maxMarks: 10, obtainedMarks: values[2], status: "COMPLETED", date: "2025-09-20" },
  { id: `${code}-q2`, type: "QUIZ", label: "Quiz 2", maxMarks: 10, obtainedMarks: values[3], status: "COMPLETED", date: "2025-10-03" },
  { id: `${code}-q3`, type: "QUIZ", label: "Quiz 3", maxMarks: 10, obtainedMarks: null, status: "PENDING" },
  { id: `${code}-fat`, type: "FAT", label: "FAT", maxMarks: 100, obtainedMarks: values[4], status: "COMPLETED", date: "2025-11-22" },
]
const labAssessments = (code: string, values: Array<number | null>): AssessmentRecord[] => values.map((value, index) => ({ id: `${code}-lab-${index + 1}`, type: "LAB", label: `Lab ${index + 1}`, maxMarks: 10, obtainedMarks: value, status: value === null ? "PENDING" : "COMPLETED", date: value === null ? undefined : `2025-0${Math.min(index + 1, 9)}-18` }))

export const demoStudent = {
  name: "Aarav Mehta", email: "aarav.mehta@university.edu", cgpa: 8.9, attendance: 88.2, courses: 4, risk: "Low",
  performance: [
    { name: "CAT 1", value: 16.75 }, { name: "CAT 2", value: 15.75 }, { name: "Quiz 1", value: 8.25 }, { name: "Quiz 2", value: 7.75 }, { name: "FAT", value: 33.5 },
  ],
  coursesList: [
    { code: "CSE302", name: "Database Management Systems", evaluationType: "THEORY" as const, mark: null, attendance: 92, attended: 46, conducted: 50, grade: null, points: 9, credits: 4, assessments: { cat1: 18, cat2: 17, assignments: 9, quizzes: 8, fat: 34 }, assessmentRecords: theoryAssessments("CSE302", [36, 34, 8, 9, 84]), theorySettings, theoryScore: null, labScore: null, combinedScore: null },
    { code: "CSE304", name: "Computer Networks", evaluationType: "EMBEDDED" as const, mark: null, attendance: 86, attended: 43, conducted: 50, grade: null, points: 8, credits: 4, assessments: { cat1: 16, cat2: 15, assignments: 8, quizzes: 7, fat: 33 }, assessmentRecords: [...theoryAssessments("CSE304", [32, 30, 7, 8, 76]), ...labAssessments("CSE304", [8, 9, 7, 8, 8, 9, null, null, null, null])], theorySettings, theoryScore: null, labScore: null, combinedScore: null },
    { code: "MAT202", name: "Statistics for Engineers", evaluationType: "THEORY" as const, mark: null, attendance: 78, attended: 39, conducted: 50, grade: null, points: 7, credits: 3, assessments: { cat1: 14, cat2: 13, assignments: 8, quizzes: 7, fat: 32 }, assessmentRecords: theoryAssessments("MAT202", [28, 27, 7, 7, 72]), theorySettings, theoryScore: null, labScore: null, combinedScore: null },
    { code: "CSE306", name: "Object-Oriented Programming Lab", evaluationType: "LAB" as const, mark: null, attendance: 95, attended: 48, conducted: 50, grade: null, points: 10, credits: 4, assessments: { cat1: 0, cat2: 0, assignments: 10, quizzes: 9, fat: 35 }, assessmentRecords: labAssessments("CSE306", [9, 10, 9, 8, 10, 9, 9, 10, 8, 9]), theorySettings: undefined, theoryScore: null, labScore: null, combinedScore: null },
  ],
  gradeDistribution: [],
}
export const demoTeacher = { students: 124, average: "78.4%", attendance: "86.7%", atRisk: 12, courses: [{ name: "Database Management Systems", average: 81, attendance: 89 }, { name: "Computer Networks", average: 76, attendance: 86 }, { name: "Statistics for Engineers", average: 73, attendance: 82 }], studentRows: [{ name: "Aarav Mehta", score: 89, attendance: 92, status: "On track" }, { name: "Diya Shah", score: 84, attendance: 88, status: "On track" }, { name: "Kabir Rao", score: 61, attendance: 73, status: "Needs attention" }, { name: "Maya Iyer", score: 77, attendance: 86, status: "On track" }], weakTopics: ["Probability distributions", "Network protocols", "SQL normalization"] }
export const demoAdmin = { users: 842, pending: 18, courses: 36, activeSemesters: 2, approvals: [{ name: "Riya Kapoor", type: "Student registration", date: "Today" }, { name: "Dr. N. Banerjee", type: "Faculty verification", date: "Yesterday" }, { name: "CSE 2026 cohort", type: "Class creation", date: "Oct 14" }] }
