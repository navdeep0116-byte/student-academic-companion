const configuredApiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL
const API_BASE_URL = configuredApiBaseUrl || (process.env.NODE_ENV === "production" ? "" : "http://localhost:8080")

if (process.env.NODE_ENV === "production" && !configuredApiBaseUrl) {
  console.warn("[v0] NEXT_PUBLIC_API_BASE_URL is required for production authentication and academic data.")
}

async function readErrorMessage(response: Response) {
  const text = await response.text()
  if (!text) return `Request failed with status ${response.status}`
  try {
    const body = JSON.parse(text) as { error?: string; message?: string }
    return body.error || body.message || text
  } catch {
    return text
  }
}

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = typeof window !== "undefined" ? sessionStorage.getItem("sac_token") : null
  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
    })
  } catch {
    throw new Error("Academic service is unavailable. Check the backend URL and try again.")
  }
  if (!response.ok) throw new Error(await readErrorMessage(response))
  return response.json()
}

export type AuthResponse = { token?: string; tokenType?: string; id?: string; userId?: string; fullName: string; email?: string; role?: string; status?: string }
export type ForgotPasswordResponse = { message: string; emailDelivery?: string; resetToken?: string }
export type ResetPasswordResponse = { message: string }
export const authApi = {
  login: (body: { email: string; password: string }) => apiRequest<AuthResponse>("/api/auth/login", { method: "POST", body: JSON.stringify(body) }),
  register: (body: { fullName: string; email: string; password: string }) => apiRequest<AuthResponse>("/api/auth/register", { method: "POST", body: JSON.stringify(body) }),
  me: () => apiRequest<AuthResponse>("/api/auth/me"),
  forgotPassword: (body: { email: string }) => apiRequest<ForgotPasswordResponse>("/api/auth/forgot-password", { method: "POST", body: JSON.stringify(body) }),
  resetPassword: (body: { email: string; token: string; newPassword: string }) => apiRequest<ResetPasswordResponse>("/api/auth/reset-password", { method: "POST", body: JSON.stringify(body) }),
}
export interface CoursePrediction {
  courseId?: string
  courseCode: string
  courseName: string
  predictionStatus: "AVAILABLE" | "INSUFFICIENT_DATA" | "SERVICE_UNAVAILABLE" | string
  level?: "level1" | "level2" | "level3" | string | null
  stageDescription?: string
  predictedOverallScore?: number | null
  likelyGrade?: string | null
  riskLevel?: string | null
  illustrativeRange?: number[] | null
  completedAssessmentsCount?: number
  normalizedInputMarks?: Record<string, number>
  statusMessage?: string
  warning?: string
}

export interface PredictionResponse {
  studentId: string
  studentNumber: string
  fullName: string
  mlServiceHealthy: boolean
  totalCourses: number
  predictableCourses: number
  predictions: CoursePrediction[]
  disclaimer: string
}

export interface LevelEligibility {
  eligible: boolean
  requiredInputs: string[]
  missingInputs: string[]
  reason: string | null
}

export interface CourseLevelAvailability {
  courseId: string
  courseCode: string
  courseName: string
  modelCourseCode: string
  completedAssessmentsCount: number
  recordedMarks: Record<string, number>
  levels: {
    level1: LevelEligibility
    level2: LevelEligibility
    level3: LevelEligibility
    [key: string]: LevelEligibility
  }
  eligibleLevels: string[]
}

export interface PredictionAvailabilityResponse {
  studentId: string
  studentNumber: string
  fullName: string
  mlServiceHealthy: boolean
  availableModels: string[]
  courses: CourseLevelAvailability[]
}

export interface PredictRequest {
  courseCode: string
  level: "level1" | "level2" | "level3" | string
  whatIfMarks?: Record<string, number>
}

export interface LiveAssessment {
  id: string
  assessmentId?: string | null
  assessmentName: string
  assessmentType: string
  obtainedMarks?: number | null
  marksObtained?: number | null
  maxMarks?: number | null
  rawMarks?: number | null
  rawMaxMarks?: number | null
  weightage?: number | null
  classAverage?: number | null
  status?: string
  grade?: string | null
}

export interface LiveCourseMarks {
  courseId: string
  courseCode: string
  courseName: string
  credits?: number | null
  evaluationType?: string | null
  assessments: LiveAssessment[]
}

export interface StudentMarksResponse {
  studentId: string
  courses: LiveCourseMarks[]
}

export interface LiveCourseAttendance {
  courseId: string
  courseCode: string
  courseName: string
  credits?: number | null
  classesConducted: number | null
  classesAttended: number | null
  classesMissed: number | null
  attendancePercentage: number | null
}

export interface StudentAttendanceResponse {
  studentId: string
  courses: LiveCourseAttendance[]
}

export interface StudentDashboardResponse {
  studentId: string
  studentNumber: string
  fullName: string
  program?: string | null
  department?: string | null
  yearOfStudy?: number | null
  section?: string | null
  totalCourses: number
  attendancePercentage?: number | null
  currentSemesterSgpa?: number | null
  previousCgpa?: number | null
  academicDataAvailable: boolean
  academicYear?: string | null
  semester?: string | null
}

export interface TeacherAssessment {
  id: string
  name: string
  type: string
  maxMarks: number
  rawMaxMarks: number
  classAverage: number | null
  status?: string
}

export interface TeacherClass {
  classId: string
  className: string
  courseCode: string
  courseName: string
  section?: string
  enrolledCount?: number
  assessments: TeacherAssessment[]
}

export interface TeacherClassesResponse {
  classes: TeacherClass[]
}

export interface TeacherStudentRoster {
  studentId: string
  studentNumber: string
  fullName: string
  email: string
  section: string
  attendancePercentage: number | null
  previousCgpa?: number | null
  enrollmentId?: string | null
}

export interface TeacherAssessmentDetail {
  id: string
  name: string
  type: string
  maxMarks: number
  rawMaxMarks: number
  officialClassAverage: number | null
  calculatedAverage: number | null
  submissionsCount: number
  enrolledCount?: number
  totalStudents?: number
  status?: string
}

export interface CumulativeCourseAverage {
  calculatedAverage: number | null
  gradedStudentsCount: number
  totalEnrolled: number
  formula: string
}

export interface ClassMarksResponse {
  classId: string
  cumulativeCourseAverage?: CumulativeCourseAverage | null
  assessments: TeacherAssessmentDetail[]
  students: Array<{
    studentId: string
    studentNumber: string
    fullName: string
    marks: Record<string, number>
    cumulativeScore?: number | null
    completedAssessmentsCount?: number
  }>
}

export interface AdminOverviewStats {
  totalStudents: number
  activeStudents: number
  pendingStudents: number
  totalTeachers: number
  activeTeachers: number
  pendingTeachers: number
  totalCourses: number
  totalClasses: number
  totalEnrollments: number
  recentActivity: Array<{ title: string; detail: string }>
}

export interface AdminUser {
  id: string
  name: string
  email: string
  role: "STUDENT" | "TEACHER" | "ADMIN" | string
  status: "ACTIVE" | "INACTIVE" | "PENDING" | string
  date: string
  studentId?: string
  studentNumber?: string
  employeeNumber?: string
  department?: string
  section?: string
  previousCgpa?: number | null
  previousCgpaUpdatedAt?: string | null
  previousCgpaUpdatedBy?: string | null
}

export interface AdminCourse {
  id: string
  code: string
  name: string
  credits: number
  department: string
  evaluationType: string
  description?: string
  classes: number
  enrolled: number
  teacher: string
  status: string
}

export interface AdminClass {
  classId: string
  name: string
  courseCode: string
  courseName: string
  section: string
  year: string
  semester: string
  department: string
  students: number
  advisor: string
  status: string
}

export interface AvailableTeacher {
  id: string
  name: string
  employeeNumber: string
  department: string
  assignedClassesCount: number
  isAvailable: boolean
}

export interface EligibleStudent {
  studentId: string
  studentNumber: string
  fullName: string
}

export interface StudentRecommendation {
  id: string
  title: string
  description: string
  category: string
  priority: string
  status: string
}

export const academicApi = {
  // Student
  studentDashboard: () => apiRequest<StudentDashboardResponse>("/api/student/dashboard"),
  prediction: (level?: string) =>
    apiRequest<PredictionResponse>(level ? `/api/student/prediction?level=${encodeURIComponent(level)}` : "/api/student/prediction"),
  predictionAvailability: () => apiRequest<PredictionAvailabilityResponse>("/api/student/prediction/availability"),
  predictLevel: (body: PredictRequest) =>
    apiRequest<CoursePrediction>("/api/student/prediction", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  marks: () => apiRequest<StudentMarksResponse>("/api/student/marks"),
  attendance: () => apiRequest<StudentAttendanceResponse>("/api/student/attendance"),
  recommendations: () => apiRequest<{ recommendations: StudentRecommendation[] }>("/api/student/recommendations"),

  // Teacher
  teacherDashboard: () => apiRequest<unknown>("/api/teacher/dashboard"),
  teacherClasses: () => apiRequest<TeacherClassesResponse>("/api/teacher/classes"),
  teacherClassStudents: (classId: string) =>
    apiRequest<{ students: TeacherStudentRoster[] }>(`/api/teacher/classes/${classId}/students`),
  teacherClassAssessments: (classId: string) =>
    apiRequest<{ assessments: TeacherAssessmentDetail[]; cumulativeCourseAverage?: CumulativeCourseAverage | null }>(`/api/teacher/classes/${classId}/assessments`),
  teacherClassMarks: (classId: string) =>
    apiRequest<ClassMarksResponse>(`/api/teacher/classes/${classId}/marks`),
  updateClassMarks: (
    classId: string,
    payload: {
      studentId?: string
      assessmentId?: string
      marks?: number | null
      entries?: Array<{ studentId: string; assessmentId: string; marks: number | null }>
    }
  ) =>
    apiRequest<{ success: boolean; updatedCount: number; message: string }>(
      `/api/teacher/classes/${classId}/marks`,
      { method: "PUT", body: JSON.stringify(payload) }
    ),
  updateClassAverage: (assessmentId: string, classAverage: number | null) =>
    apiRequest<{ success: boolean; assessmentId: string; classAverage: number | null; message: string }>(
      `/api/teacher/assessments/${assessmentId}/class-average`,
      {
        method: "PUT",
        body: JSON.stringify({ classAverage }),
      }
    ),

  // Admin
  adminDashboard: () => apiRequest<unknown>("/api/admin/dashboard"),
  adminOverview: () => apiRequest<AdminOverviewStats>("/api/admin/overview"),
  adminUsers: () => apiRequest<{ users: AdminUser[] }>("/api/admin/users"),
  createAdminUser: (body: {
    name: string
    email: string
    role: string
    password?: string
    studentNumber?: string
    employeeNumber?: string
    department?: string
    section?: string
  }) =>
    apiRequest<{ success: boolean; userId: string; email: string; role: string }>("/api/admin/users", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  updateUserStatus: (id: string, status?: string) =>
    apiRequest<{ success: boolean; id: string; status: string }>(`/api/admin/users/${id}/status`, {
      method: "PATCH",
      body: status ? JSON.stringify({ status }) : undefined,
    }),
  adminCourses: () => apiRequest<{ courses: AdminCourse[] }>("/api/admin/courses"),
  createCourse: (body: { code: string; name: string; department?: string; credits?: number; description?: string }) =>
    apiRequest<{ success: boolean; courseId: string; code: string; name: string }>("/api/admin/courses", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  availableTeachers: () => apiRequest<{ teachers: AvailableTeacher[] }>("/api/admin/teachers/available"),
  adminClasses: () => apiRequest<{ classes: AdminClass[] }>("/api/admin/classes"),
  createClass: (body: { courseCode: string; section: string; teacherId?: string }) =>
    apiRequest<{ success: boolean; classId: string; name: string }>("/api/admin/classes", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  eligibleStudents: () => apiRequest<{ students: EligibleStudent[] }>("/api/admin/students/eligible"),
  enrollStudent: (body: { studentId: string; classId: string }) =>
    apiRequest<{ success: boolean; enrollmentId: string; message: string }>("/api/admin/enrollments", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  updateStudentCgpa: (studentId: string, previousCgpa: number | null) =>
    apiRequest<{
      success: boolean
      studentId: string
      studentNumber: string
      previousCgpa: number | null
      previousCgpaUpdatedAt: string
      previousCgpaUpdatedBy: string
      message: string
    }>(`/api/admin/students/${studentId}/cgpa`, {
      method: "PUT",
      body: JSON.stringify({ previousCgpa }),
    }),
}


