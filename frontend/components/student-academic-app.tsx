'use client'

import { useEffect, useState } from "react"

import { Area, AreaChart, Bar, BarChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts"

import { AlertTriangle, BarChart3, BookOpen, CalendarDays, Check, CheckCircle2, ClipboardList, Eye, EyeOff, GraduationCap, Info, Layers, LayoutDashboard, Lock, LogOut, Menu, Settings, ShieldCheck, Sparkles, TrendingUp, UserRound, Users, X } from "lucide-react"

import { academicApi, authApi, type LiveAssessment, type LiveCourseMarks, type StudentMarksResponse, type LiveCourseAttendance, type StudentAttendanceResponse, type PredictionResponse, type CoursePrediction, type StudentDashboardResponse, type PredictionAvailabilityResponse, type CourseLevelAvailability, type LevelEligibility } from "@/lib/api-client"

import { AdminDashboard } from "@/components/admin-dashboard"

import { TeacherDashboard } from "@/components/teacher-dashboard"

import { MIN_ATTENDANCE_PERCENTAGE, attendancePercentage, attendanceStatus, belowThresholdCourseCount } from "@/lib/attendance"

import { calculateLabScore, calculateTheoryScore, getPrediction } from "@/lib/predictions"
import { calculateCourseRecordedScore, findAssessment, getRawMark, type CourseScoreCalculation } from "@/lib/grading-scheme"

type Role = "student" | "teacher" | "admin"

const VALID_ROLES: readonly Role[] = ["student", "teacher", "admin"]

type Page = "Overview" | "Marks & Results" | "Attendance" | "Performance" | "Predictions" | "Profile" | "Settings" | "My Courses" | "Students" | "Assessments" | "User Management" | "Courses & Classes"

const card = "rounded-2xl border bg-card p-5 shadow-sm"

function StatCard({ icon: Icon, label, value, note }: { icon: typeof TrendingUp; label: string; value: string | number; note: string }) { return <div className={card}><div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary"><Icon /></div><p className="mt-4 text-sm text-muted-foreground">{label}</p><p className="mt-1 text-2xl font-semibold">{value}</p><p className="mt-1 text-xs text-muted-foreground">{note}</p></div> }

function SelectBox({ label, value, onChange, children }: { label: string; value: string; onChange: (value: string) => void; children: React.ReactNode }) { return <label className="flex flex-col gap-2 text-sm font-medium">{label}<select value={value} onChange={e => onChange(e.target.value)} className="h-10 rounded-xl border bg-background px-3 font-normal outline-none focus:ring-2 focus:ring-ring">{children}</select></label> }

function Login({ onLogin }: { onLogin: (name: string, role: Role) => void }) {
  const [register, setRegister] = useState(false)
  const [forgot, setForgot] = useState(false)
  const [resetStep, setResetStep] = useState<"request" | "verify">("request")
  const [resetToken, setResetToken] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState("")
  const [error, setError] = useState("")
  const [form, setForm] = useState({ fullName: "", email: "", password: "", confirm: "" })

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setMessage("");

    if (forgot) {
      if (resetStep === "request") {
        if (!form.email.trim() || !form.email.includes("@")) {
          setError("Enter a valid university email.");
          return;
        }
        setLoading(true);
        try {
          const res = await authApi.forgotPassword({ email: form.email.trim() });
          setMessage(res.message);
          if (res.resetToken) {
            setResetToken(res.resetToken);
          }
          setResetStep("verify");
        } catch (err) {
          setError(err instanceof Error ? err.message : "Unable to process password reset.");
        } finally {
          setLoading(false);
        }
        return;
      }

      if (!resetToken.trim()) {
        setError("Enter the reset token.");
        return;
      }
      if (newPassword.length < 8) {
        setError("New password must be at least 8 characters.");
        return;
      }
      if (newPassword !== confirmPassword) {
        setError("Passwords do not match.");
        return;
      }
      setLoading(true);
      try {
        const res = await authApi.resetPassword({
          email: form.email.trim(),
          token: resetToken.trim(),
          newPassword: newPassword,
        });
        setMessage(res.message);
        setForgot(false);
        setResetStep("request");
        setResetToken("");
        setNewPassword("");
        setConfirmPassword("");
        setForm(f => ({ ...f, password: "" }));
      } catch (err) {
        setError(err instanceof Error ? err.message : "Password reset failed.");
      } finally {
        setLoading(false);
      }
      return;
    }

    if (register && form.password !== form.confirm) { setError("Passwords do not match."); return }
    if (register && !form.fullName.trim()) { setError("Enter your full name."); return }
    if (!form.email.trim() || !form.email.includes("@")) { setError("Enter a valid university email."); return }
    if (form.password.length < 8) { setError("Password must be at least 8 characters."); return }

    setLoading(true);
    try {
      const result = register ? await authApi.register({ fullName: form.fullName, email: form.email, password: form.password }) : await authApi.login({ email: form.email, password: form.password });
      const normalizedRole = (result.role || (register ? "student" : "")).toLowerCase();
      if (!VALID_ROLES.includes(normalizedRole as Role)) throw new Error("Your account role is unavailable.");
      if (!result.token) throw new Error("The server did not return an authentication token. Please try again.");
      sessionStorage.setItem("sac_token", result.token);
      sessionStorage.setItem("sac_email", result.email || form.email.trim());
      onLogin(result.fullName, normalizedRole as Role);
    } catch (error) {
      setError(error instanceof Error ? error.message : "Unable to sign in. The backend authentication service is unavailable.");
    } finally {
      setLoading(false);
    }
  }

  return <main className="flex min-h-screen bg-background"><section className="hidden w-[46%] flex-col justify-between bg-[#0b1f3a] p-12 text-white lg:flex"><div><div className="flex items-center gap-3"><div className="flex size-10 items-center justify-center rounded-xl bg-blue-500"><GraduationCap /></div><b>Student Academic Companion</b></div><div className="mt-24 max-w-lg"><p className="text-sm font-medium uppercase tracking-[0.2em] text-blue-300">Your academic command center</p><h1 className="mt-5 text-5xl font-semibold leading-tight">Make every semester count.</h1><p className="mt-6 text-lg leading-8 text-slate-300">A clearer view of your progress, attendance, and opportunities to improve.</p></div></div><p className="text-sm text-slate-400">© 2026 Student Academic Companion</p></section><section className="flex flex-1 items-center justify-center p-6"><div className="w-full max-w-md"><div className="mb-8 flex items-center gap-3 lg:hidden"><div className="flex size-10 items-center justify-center rounded-xl bg-blue-600 text-white"><GraduationCap /></div><b>Student Academic Companion</b></div><div className="rounded-3xl border bg-card p-8 shadow-sm"><h2 className="mt-2 text-3xl font-semibold">{forgot ? (resetStep === "verify" ? "Set new password" : "Reset your password") : register ? "Create your account" : "Sign in to your portal"}</h2><p className="mt-2 text-sm text-muted-foreground">{forgot ? (resetStep === "verify" ? "Enter the reset token and your new password." : "Enter your university email to receive recovery instructions.") : register ? "Start tracking your academic journey." : "Use your university credentials to continue."}</p><form onSubmit={submit} className="mt-8 flex flex-col gap-4">{register && <label className="text-sm font-medium">Full name<input required value={form.fullName} onChange={e => setForm({ ...form, fullName: e.target.value })} className="mt-2 h-11 w-full rounded-xl border bg-background px-3 outline-none focus:border-primary" /></label>}<label className="text-sm font-medium">University email<input required type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} className="mt-2 h-11 w-full rounded-xl border bg-background px-3 outline-none focus:border-primary" /></label>{forgot && resetStep === "verify" && (<><label className="text-sm font-medium">Reset token<input required value={resetToken} onChange={e => setResetToken(e.target.value)} placeholder="Security reset token" className="mt-2 h-11 w-full font-mono text-xs rounded-xl border bg-background px-3 outline-none focus:border-primary" /></label><label className="text-sm font-medium">New password<input required type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} placeholder="Minimum 8 characters" className="mt-2 h-11 w-full rounded-xl border bg-background px-3 outline-none focus:border-primary" /></label><label className="text-sm font-medium">Confirm new password<input required type="password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} className="mt-2 h-11 w-full rounded-xl border bg-background px-3 outline-none focus:border-primary" /></label></>)}{!forgot && <><label className="text-sm font-medium">Password<input required type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} className="mt-2 h-11 w-full rounded-xl border bg-background px-3 outline-none focus:border-primary" /></label>{register && <label className="text-sm font-medium">Confirm password<input required type="password" value={form.confirm} onChange={e => setForm({ ...form, confirm: e.target.value })} className="mt-2 h-11 w-full rounded-xl border bg-background px-3 outline-none focus:border-primary" /></label>}</>}{error && <p className="rounded-lg bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}{message && <p className="rounded-lg bg-emerald-500/10 p-3 text-sm text-emerald-700 dark:text-emerald-300">{message}</p>}<button disabled={loading} className="h-11 rounded-xl bg-primary font-medium text-primary-foreground disabled:opacity-60">{loading ? "Working…" : forgot ? (resetStep === "verify" ? "Save new password" : "Request reset") : register ? "Create account" : "Sign in"}</button></form><div className="mt-5 flex justify-between text-sm text-muted-foreground"><button onClick={() => { setForgot(!forgot); setResetStep("request"); setRegister(false); setError(""); setMessage(""); }} className="hover:text-primary">{forgot ? "Back to sign in" : "Forgot password?"}</button>{forgot && resetStep === "request" && <button onClick={() => { setResetStep("verify"); setError(""); }} className="font-medium text-primary">I have a token</button>}{!forgot && <button onClick={() => setRegister(!register)} className="font-medium text-primary">{register ? "I have an account" : "Create account"}</button>}</div></div></div></section></main>
}

function getLiveAssessment(course: LiveCourseMarks, testName: string, testType?: string): LiveAssessment | undefined {
  const norm = (s?: string) => (s || "").toUpperCase().replace(/\s+/g, "")
  const target = norm(testName)
  return course.assessments.find(a => {
    const aName = norm(a.assessmentName)
    const aType = norm(a.assessmentType)
    if (aName === target || aName.includes(target)) return true
    if (testType && aType === norm(testType) && aName.includes(target)) return true
    return false
  })
}

function getLiveAssessmentValue(course: LiveCourseMarks, testName: string, testType?: string): string {
  const a = getLiveAssessment(course, testName, testType)
  if (!a) return "Not yet"
  const marks = a.obtainedMarks ?? a.marksObtained
  if (a.status === "COMPLETED" && typeof marks === "number") {
    return `${marks}/${a.maxMarks ?? 0}`
  }
  return "Not yet"
}

function getLiveQuizValue(course: LiveCourseMarks): string {
  const quizzes = course.assessments.filter(a => {
    const t = (a.assessmentType || "").toUpperCase()
    const n = (a.assessmentName || "").toUpperCase()
    return t === "QUIZ" || n.includes("QUIZ")
  })
  const completed = quizzes.filter(a => a.status === "COMPLETED" && typeof (a.obtainedMarks ?? a.marksObtained) === "number")
  if (!completed.length) return "Not yet"
  const obtainedSum = completed.reduce((sum, a) => sum + ((a.obtainedMarks ?? a.marksObtained) || 0), 0)
  const maxSum = completed.reduce((sum, a) => sum + (a.maxMarks || 0), 0)
  return `${Math.round(obtainedSum * 10) / 10}/${maxSum}`
}

function getLiveLabValue(course: LiveCourseMarks): string {
  const labs = course.assessments.filter(a => {
    const t = (a.assessmentType || "").toUpperCase()
    const n = (a.assessmentName || "").toUpperCase()
    return t === "LAB" || t === "OTHER" || n.includes("LAB")
  })
  const completed = labs.filter(a => a.status === "COMPLETED" && typeof (a.obtainedMarks ?? a.marksObtained) === "number")
  if (!completed.length) return "Not yet"
  const obtainedSum = completed.reduce((sum, a) => sum + ((a.obtainedMarks ?? a.marksObtained) || 0), 0)
  const maxSum = completed.reduce((sum, a) => sum + (a.maxMarks || 0), 0)
  return `${Math.round(obtainedSum * 10) / 10}/${maxSum}`
}

function getLiveRecordedScore(course: LiveCourseMarks) {
  const completed = course.assessments.filter(a => a.status === "COMPLETED" && typeof (a.obtainedMarks ?? a.marksObtained) === "number")
  if (!completed.length) return { text: "Not enough data", sub: "0 completed evaluations", pct: null, earned: 0, total: 0 }
  const earned = completed.reduce((sum, a) => sum + ((a.obtainedMarks ?? a.marksObtained) || 0), 0)
  const total = completed.reduce((sum, a) => sum + (a.maxMarks || 0), 0)
  const pct = total > 0 ? (earned / total) * 100 : 0
  return {
    text: `${pct.toFixed(1)}%`,
    sub: `${earned.toFixed(1)} / ${total} marks earned`,
    pct,
    earned,
    total
  }
}

function findCoursePrediction(course: LiveCourseMarks, predData: PredictionResponse | null): CoursePrediction | null {
  if (!predData?.predictions || !predData.predictions.length) return null
  const cCode = (course.courseCode || "").trim().toUpperCase()
  const cId = (course.courseId || "").trim()

  return predData.predictions.find(p => {
    const pCode = (p.courseCode || "").trim().toUpperCase()
    const pId = (p.courseId || "").trim()
    if (pCode && cCode && pCode === cCode) return true
    if (pId && cId && pId === cId) return true
    return false
  }) || null
}

const nav = { student: [["Overview", LayoutDashboard], ["Marks & Results", ClipboardList], ["Attendance", CheckCircle2], ["Performance", BarChart3], ["Predictions", Sparkles], ["Profile", UserRound]], teacher: [["Overview", LayoutDashboard], ["My Courses", BookOpen], ["Students", Users], ["Assessments", ClipboardList]], admin: [["Overview", LayoutDashboard], ["User Management", Users], ["Courses & Classes", BookOpen]] } as const

function Sidebar({ role, active, setActive, logout, open, close }: { role: Role; active: Page; setActive: (x: Page) => void; logout: () => void; open: boolean; close: () => void }) { return <aside className={`fixed inset-y-0 left-0 z-20 flex w-72 flex-col border-r bg-sidebar p-5 transition-transform lg:static lg:translate-x-0 ${open ? "translate-x-0" : "-translate-x-full"}`}><div className="flex items-center justify-between"><div className="flex items-center gap-3"><div className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground"><GraduationCap /></div><b>SAC</b></div><button onClick={close} className="lg:hidden"><X /></button></div><div className="mt-10 flex flex-col gap-1">{nav[role].map(([label, Icon]) => <button key={label} onClick={() => { setActive(label as Page); close() }} className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium ${active === label ? "bg-sidebar-accent text-sidebar-accent-foreground" : "text-sidebar-foreground/70 hover:bg-sidebar-accent"}`}><Icon />{label}</button>)}</div><div className="mt-auto flex flex-col gap-1"><button onClick={() => { setActive("Settings"); close() }} className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-sidebar-foreground/70 hover:bg-sidebar-accent"><Settings />Settings</button><button onClick={logout} className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-sidebar-foreground/70 hover:bg-sidebar-accent"><LogOut />Sign out</button></div></aside> }

interface PerformanceChartPayload {
  name: string
  courseName: string
  value: number | null
  scoreDisplay: string
  evaluationsCount: number
  totalEvaluations: number
  statusText: string
  hasData: boolean
}

function CustomPerformanceTooltip({ active, payload }: { active?: boolean; payload?: Array<{ payload: PerformanceChartPayload }> }) {
  if (!active || !payload || !payload.length) return null
  const data = payload[0].payload
  return (
    <div className="rounded-xl border bg-popover/95 p-3 shadow-md backdrop-blur-sm text-xs">
      <p className="font-semibold text-foreground text-sm">{data.name}</p>
      <p className="text-muted-foreground mb-2">{data.courseName}</p>
      <div className="flex items-center justify-between gap-3 border-t pt-1.5">
        <span className="text-muted-foreground">Current score:</span>
        <span className="font-bold text-primary font-mono text-xs">{data.scoreDisplay}</span>
      </div>
      <div className="flex items-center justify-between gap-3 pt-1">
        <span className="text-muted-foreground">Graded evaluations:</span>
        <span className="font-medium text-foreground">{data.evaluationsCount} / {data.totalEvaluations} completed</span>
      </div>
      <div className="flex items-center justify-between gap-3 pt-1">
        <span className="text-muted-foreground">Status:</span>
        <span className={`font-semibold ${data.hasData ? "text-emerald-600 dark:text-emerald-400" : "text-amber-600 dark:text-amber-400"}`}>
          {data.statusText}
        </span>
      </div>
    </div>
  )
}

function Overview({ go, name }: { go: (p: Page) => void; name: string }) {
  const [dashboard, setDashboard] = useState<StudentDashboardResponse | null>(null)
  const [attendanceData, setAttendanceData] = useState<StudentAttendanceResponse | null>(null)
  const [marksData, setMarksData] = useState<StudentMarksResponse | null>(null)
  const [recs, setRecs] = useState<Array<{ id: string; title: string; description: string; priority: string }>>([])

  useEffect(() => {
    let cancelled = false
    Promise.all([
      academicApi.studentDashboard().catch(() => null),
      academicApi.attendance().catch(() => null),
      academicApi.marks().catch(() => null),
      academicApi.recommendations().catch(() => null),
    ]).then(([d, a, m, r]) => {
      if (!cancelled) {
        if (d) setDashboard(d)
        if (a) setAttendanceData(a)
        if (m) setMarksData(m)
        if (r?.recommendations) setRecs(r.recommendations)
      }
    })
    return () => { cancelled = true }
  }, [])

  const coursesCount = dashboard?.totalCourses != null ? String(dashboard.totalCourses) : "0"
  const hasAttendanceRecords = Boolean(attendanceData?.courses && attendanceData.courses.length > 0)
  const attendanceVal = dashboard?.attendancePercentage != null
    ? `${Number(dashboard.attendancePercentage).toFixed(1)}%`
    : "No attendance data"

  const attendanceChartData = hasAttendanceRecords
    ? attendanceData!.courses.map(c => ({
        code: c.courseCode,
        attendance: c.attendancePercentage ?? 0
      }))
    : []

  const hasMarksRecords = Boolean(marksData?.courses && marksData.courses.length > 0)
  const courseCalculations = (marksData?.courses || []).map(c => {
    const calc = calculateCourseRecordedScore(c)
    return {
      courseCode: c.courseCode,
      courseName: c.courseName,
      calc,
      score: calc.scoreValue,
      hasCompletedEvaluations: calc.scoreValue !== null,
    }
  })

  const hasAnyEvaluations = courseCalculations.some(c => c.hasCompletedEvaluations)

  const performanceChartData = courseCalculations.map(c => ({
    name: c.courseCode,
    courseName: c.courseName,
    value: c.score != null ? c.score : null,
    scoreDisplay: c.score != null ? `${c.score.toFixed(1)}%` : "No data yet",
    evaluationsCount: c.calc.completedAssessmentsCount,
    totalEvaluations: c.calc.totalAssessmentsCount,
    statusText: c.calc.isComplete ? "Final" : c.score != null ? "In progress" : "No evaluations",
    hasData: c.score != null,
  }))

  const hasEnrollments = (dashboard?.totalCourses ?? 0) > 0
  const riskStatus = !hasEnrollments
    ? "Not assessed"
    : recs.some(r => r.priority === "HIGH")
    ? "Elevated"
    : "Low"
  const riskNote = !hasEnrollments
    ? "Awaiting enrollment records"
    : "Predictions updated"

  const statCards: Array<{
    icon: typeof TrendingUp
    label: string
    value: string
    note: string
    targetPage: Page
  }> = [
    {
      icon: GraduationCap,
      label: "Previous CGPA",
      value: dashboard?.previousCgpa != null ? `${Number(dashboard.previousCgpa).toFixed(2)} / 10` : "Historical record unavailable",
      note: dashboard?.previousCgpa != null ? "Historical cumulative GPA" : "Historical record unavailable",
      targetPage: "Profile",
    },
    {
      icon: TrendingUp,
      label: "Current-semester SGPA",
      value: dashboard?.currentSemesterSgpa != null ? `${Number(dashboard.currentSemesterSgpa).toFixed(2)} / 10` : "Not available",
      note: dashboard?.currentSemesterSgpa != null ? "Calculated from completed assessments" : "Semester assessments pending completion",
      targetPage: "Performance",
    },
    {
      icon: BookOpen,
      label: "Total courses",
      value: coursesCount,
      note: hasEnrollments ? "Enrolled this semester" : "No active enrollments",
      targetPage: "Marks & Results",
    },
    {
      icon: CheckCircle2,
      label: "Attendance",
      value: attendanceVal,
      note: dashboard?.attendancePercentage != null ? `Threshold: ${MIN_ATTENDANCE_PERCENTAGE}%` : "No sessions recorded",
      targetPage: "Attendance",
    },
    {
      icon: AlertTriangle,
      label: "Academic risk",
      value: riskStatus,
      note: riskNote,
      targetPage: "Predictions",
    },
  ]

  return (
    <div className="flex flex-col gap-7">
      <LiveStudentStatus />
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <h2 className="mt-3 text-3xl font-semibold">Good morning, {name.split(" ")[0]}.</h2>
          <p className="mt-2 text-muted-foreground">Here&apos;s how your semester is shaping up.</p>
        </div>
        <button onClick={() => go("Performance")} className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground">View analytics</button>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {statCards.map(({ icon: Icon, label, value, note, targetPage }) => (
          <button className="text-left" onClick={() => go(targetPage)} key={label}>
            <StatCard icon={Icon} label={label} value={value} note={note} />
          </button>
        ))}
      </div>
      <div className="grid gap-5 xl:grid-cols-[1.35fr_1fr]">
        <div className={card}>
          <div className="flex items-start justify-between">
            <div>
              <h3 className="font-semibold">Current-semester snapshot</h3>
              <p className="mt-1 text-sm text-muted-foreground">Assessment performance across enrolled courses (0–100%)</p>
            </div>
            {hasAnyEvaluations && (
              <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
                Weighted Scale
              </span>
            )}
          </div>
          <div className="mt-5 h-64">
            {hasMarksRecords && hasAnyEvaluations ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={performanceChartData} margin={{ top: 10, right: 10, left: -15, bottom: 0 }}>
                  <defs>
                    <linearGradient id="scoreGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#2563eb" stopOpacity={0.35} />
                      <stop offset="95%" stopColor="#2563eb" stopOpacity={0.02} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                  <YAxis domain={[0, 100]} ticks={[0, 25, 50, 75, 100]} tickFormatter={(v) => `${v}%`} tick={{ fontSize: 11 }} />
                  <Tooltip content={<CustomPerformanceTooltip />} />
                  <Area
                    type="monotone"
                    dataKey="value"
                    stroke="#2563eb"
                    fill="url(#scoreGradient)"
                    strokeWidth={2.5}
                    dot={{ r: 4, stroke: "#2563eb", strokeWidth: 2, fill: "#ffffff" }}
                    activeDot={{ r: 6, stroke: "#1d4ed8", strokeWidth: 2, fill: "#2563eb" }}
                    connectNulls={false}
                  />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-full flex-col items-center justify-center rounded-xl border border-dashed p-6 text-center text-muted-foreground">
                <TrendingUp className="mb-2 size-8 opacity-40" />
                <p className="text-sm font-medium">No performance records</p>
                <p className="mt-1 text-xs">Evaluations will appear here once faculty records course marks.</p>
              </div>
            )}
          </div>
          {hasMarksRecords && (
            <div className="mt-3 flex flex-wrap gap-2 border-t pt-3">
              {performanceChartData.map(item => (
                <div key={item.name} className="flex items-center gap-1.5 rounded-lg border bg-muted/30 px-2.5 py-1 text-xs">
                  <span className="font-semibold text-foreground">{item.name}:</span>
                  {item.hasData ? (
                    <span className="font-mono font-bold text-primary">{item.scoreDisplay}</span>
                  ) : (
                    <span className="text-muted-foreground italic">No data yet</span>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
        <div className={card}>
          <h3 className="font-semibold">Attendance by course</h3>
          <p className="mt-1 text-sm text-muted-foreground">{MIN_ATTENDANCE_PERCENTAGE}% minimum reference</p>
          <div className="mt-5 h-64">
            {hasAttendanceRecords && attendanceChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={attendanceChartData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="code" />
                  <YAxis domain={[0, 100]} />
                  <ReferenceLine y={MIN_ATTENDANCE_PERCENTAGE} stroke="#dc2626" strokeDasharray="5 5" />
                  <Tooltip />
                  <Bar dataKey="attendance" fill="#2563eb" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-full flex-col items-center justify-center rounded-xl border border-dashed p-6 text-center text-muted-foreground">
                <CheckCircle2 className="mb-2 size-8 opacity-40" />
                <p className="text-sm font-medium">No attendance records</p>
                <p className="mt-1 text-xs">Class sessions will appear here once attendance is recorded.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {recs.length > 0 && (
        <div className={card}>
          <div className="flex items-center justify-between mb-3">
            <div>
              <h3 className="font-semibold">Academic Recommendations</h3>
              <p className="text-sm text-muted-foreground">Personalized study guidance generated from your evaluations.</p>
            </div>
            <span className="rounded-full bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">
              {recs.length} Active Plan{recs.length === 1 ? "" : "s"}
            </span>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            {recs.map((rec) => (
              <div key={rec.id} className="rounded-xl border bg-muted/20 p-3.5">
                <div className="flex items-center justify-between">
                  <h4 className="font-medium text-sm">{rec.title}</h4>
                  <span className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase ${
                    rec.priority === "HIGH" ? "bg-rose-500/10 text-rose-700 dark:text-rose-300" : "bg-primary/10 text-primary"
                  }`}>
                    {rec.priority}
                  </span>
                </div>
                <p className="mt-1 text-xs text-muted-foreground leading-relaxed">{rec.description}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className={`${card} flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between`}>
        <div>
          <h3 className="font-semibold">Course Evaluation</h3>
          <p className="mt-1 text-sm text-muted-foreground">Detailed marks and ML forecasts are available under Marks & Results.</p>
        </div>
        <button onClick={() => go("Marks & Results")} className="rounded-xl border px-4 py-2 text-sm font-medium">Review marks</button>
      </div>
    </div>
  )
}

function MarksPage() {
  const [courseFilter, setCourseFilter] = useState("All")
  const [selected, setSelected] = useState<LiveCourseMarks | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [marksData, setMarksData] = useState<StudentMarksResponse | null>(null)
  const [predData, setPredData] = useState<PredictionResponse | null>(null)

  function loadData() {
    setLoading(true)
    setError(null)
    Promise.all([
      academicApi.marks(),
      academicApi.prediction().catch(() => null)
    ])
      .then(([mRes, pRes]) => {
        setMarksData(mRes)
        setPredData(pRes)
        setLoading(false)
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Failed to load marks and results")
        setLoading(false)
      })
  }

  useEffect(() => {
    let cancelled = false
    Promise.all([
      academicApi.marks(),
      academicApi.prediction().catch(() => null)
    ])
      .then(([mRes, pRes]) => {
        if (!cancelled) {
          setMarksData(mRes)
          setPredData(pRes)
          setLoading(false)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Failed to load marks and results")
          setLoading(false)
        }
      })
    return () => { cancelled = true }
  }, [])

  const courses = marksData?.courses || []
  const rows = courseFilter === "All" ? courses : courses.filter(c => c.courseCode === courseFilter)

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="mt-3 text-3xl font-semibold">Marks & Results</h2>
        <p className="mt-2 text-muted-foreground">Course assessments from verified university records and machine learning predicted scores.</p>
      </div>

      {loading && (
        <div className={`${card} flex items-center justify-center p-12 text-sm text-muted-foreground`}>
          <div className="flex items-center gap-3">
            <span className="size-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            <span>Loading verified marks and forecasts…</span>
          </div>
        </div>
      )}

      {error && !loading && (
        <div className={`${card} border-destructive/40 bg-destructive/5 flex items-center justify-between p-5`}>
          <div>
            <h3 className="font-semibold text-destructive">Unable to load marks</h3>
            <p className="text-sm text-muted-foreground">{error}</p>
          </div>
          <button onClick={loadData} className="rounded-xl border px-3 py-1.5 text-xs font-medium hover:bg-muted">Retry</button>
        </div>
      )}

      {!loading && !error && (
        <>
          <div className={`${card} grid gap-4`}>
            <SelectBox label="Current semester course" value={courseFilter} onChange={setCourseFilter}>
              <option value="All">All Enrolled Courses</option>
              {courses.map(c => <option key={c.courseCode} value={c.courseCode}>{c.courseCode} · {c.courseName}</option>)}
            </SelectBox>
          </div>

          <div className={card}>
            <div className="flex items-center justify-between mb-2">
              <h3 className="font-semibold">Course-wise marks</h3>
              <span className="text-xs text-muted-foreground">{rows.length} course{rows.length === 1 ? "" : "s"} enrolled</span>
            </div>
            <div className="mt-4 overflow-x-auto">
              <table className="w-full min-w-[900px] text-left text-sm">
                <thead className="border-b text-xs uppercase text-muted-foreground">
                  <tr>
                    {["Course", "CAT 1", "CAT 2", "Quizzes", "Lab assessments", "FAT", "Current recorded score", "Predicted score", "Status"].map(h => (
                      <th className="p-3" key={h}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {rows.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="p-6 text-center text-sm text-muted-foreground">No enrolled course records found.</td>
                    </tr>
                  ) : (
                    rows.map(c => {
                      const pred = findCoursePrediction(c, predData)
                      const calc = calculateCourseRecordedScore(c)

                      return (
                        <tr onClick={() => setSelected(c)} className="cursor-pointer border-b last:border-0 hover:bg-muted/50" key={c.courseCode}>
                          <td className="p-3 font-medium">
                            {c.courseName}
                            <span className="block text-xs text-muted-foreground">
                              {c.courseCode} · {c.evaluationType || "THEORY"}{c.credits ? ` · ${c.credits} credits` : ""}
                            </span>
                            <span className="block text-xs text-muted-foreground">
                              {calc.completedAssessmentsCount} of {calc.totalAssessmentsCount} assessment{calc.totalAssessmentsCount === 1 ? "" : "s"} completed
                            </span>
                          </td>
                          <td className="p-3 font-mono text-xs">{calc.cat1Raw !== null ? `${calc.cat1Raw}/50` : "Not yet"}</td>
                          <td className="p-3 font-mono text-xs">{calc.cat2Raw !== null ? `${calc.cat2Raw}/50` : "Not yet"}</td>
                          <td className="p-3">
                            <div className="flex flex-col gap-0.5 text-xs font-mono">
                              <span>Q1: <b>{calc.quiz1Raw !== null ? `${calc.quiz1Raw}/10` : "—"}</b></span>
                              <span>Q2: <b>{calc.quiz2Raw !== null ? `${calc.quiz2Raw}/10` : "—"}</b></span>
                              <span>Q3: <b>{calc.quiz3Raw !== null ? `${calc.quiz3Raw}/10` : "—"}</b></span>
                            </div>
                          </td>
                          <td className="p-3">
                            <div className="text-xs">
                              <span className="font-medium font-mono">{calc.completedLabsCount}/10 completed</span>
                              {calc.labScore !== null && (
                                <span className="block text-[11px] text-muted-foreground">{calc.labScore.toFixed(1)}% score</span>
                              )}
                            </div>
                          </td>
                          <td className="p-3 font-mono text-xs">{calc.fatRaw !== null ? `${calc.fatRaw}/100` : "Not yet"}</td>
                          <td className="p-3 font-semibold">
                            <div>
                              <span className={calc.isComplete ? "text-emerald-700 dark:text-emerald-400" : ""}>{calc.scoreText}</span>
                              <span className="block text-[11px] font-normal text-muted-foreground">{calc.scoreSub}</span>
                            </div>
                          </td>
                          <td className="p-3">
                            {pred && pred.predictionStatus === "AVAILABLE" && typeof pred.predictedOverallScore === "number" ? (
                              <div className="flex flex-col">
                                <div className="flex items-center gap-1.5">
                                  <span className="font-semibold text-primary">{pred.predictedOverallScore.toFixed(1)}%</span>
                                  {pred.level && (
                                    <span className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-bold uppercase text-primary">
                                      {pred.level}
                                    </span>
                                  )}
                                </div>
                                <span className="text-[11px] text-muted-foreground">ML estimate · Not actual final result</span>
                              </div>
                            ) : (
                              <span className="text-xs text-muted-foreground">Not available</span>
                            )}
                          </td>
                          <td className="p-3">
                            {calc.isComplete ? (
                              <span className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-700 dark:text-emerald-400">Complete</span>
                            ) : (
                              <span className="text-xs text-muted-foreground">In progress</span>
                            )}
                          </td>
                        </tr>
                      )
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {selected && (
        <div role="dialog" className="fixed inset-0 z-30 flex items-center justify-center bg-black/40 p-5" onClick={() => setSelected(null)}>
          <div className={`${card} w-full max-w-xl max-h-[90vh] overflow-y-auto`} onClick={e => e.stopPropagation()}>
            <div className="flex items-start justify-between">
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Detailed Assessment Breakdown</p>
                <h3 className="mt-1 text-xl font-semibold">{selected.courseName}</h3>
                <p className="text-xs text-muted-foreground">{selected.courseCode} · {selected.evaluationType || "THEORY"}{selected.credits ? ` · ${selected.credits} credits` : ""}</p>
              </div>
              <button onClick={() => setSelected(null)} className="rounded-lg p-1 hover:bg-muted"><X className="size-5" /></button>
            </div>

            <div className="mt-4">
              <p className="text-xs font-semibold uppercase text-muted-foreground mb-2">Evaluations & Class Comparison</p>
              <div className="grid gap-2 sm:grid-cols-2">
                {selected.assessments.map(a => {
                  const rawVal = typeof a.rawMarks === "number" ? a.rawMarks : typeof (a.obtainedMarks ?? a.marksObtained) === "number" ? a.obtainedMarks ?? a.marksObtained : null
                  const rawMax = a.rawMaxMarks ?? a.maxMarks ?? 10
                  const hasMarks = a.status === "COMPLETED" && typeof rawVal === "number"

                  return (
                    <div className="rounded-xl border bg-muted/40 p-3 text-xs" key={a.id || a.assessmentName}>
                      <div className="flex items-center justify-between">
                        <span className="font-medium text-foreground">{a.assessmentName}</span>
                        <span className={`px-1.5 py-0.5 rounded text-[10px] font-semibold ${hasMarks ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300" : "bg-muted text-muted-foreground"}`}>
                          {hasMarks ? "COMPLETED" : "PENDING"}
                        </span>
                      </div>
                      <div className="mt-2 flex items-baseline justify-between">
                        <span className="text-muted-foreground">Marks scored:</span>
                        <b className="text-sm font-semibold">{hasMarks ? `${rawVal} / ${rawMax}` : "Not recorded"}</b>
                      </div>
                      <div className="mt-1 flex items-baseline justify-between text-[11px] text-muted-foreground">
                        <span>Class average:</span>
                        <b>{a.classAverage != null ? `${a.classAverage} / ${rawMax}` : "Not entered"}</b>
                      </div>
                      {a.weightage != null && (
                        <span className="text-[10px] text-muted-foreground block mt-1">Weightage: {a.weightage}%</span>
                      )}
                    </div>
                  )
                })}
              </div>
            </div>

            {(() => {
              const selectedCalc = calculateCourseRecordedScore(selected)
              const p = findCoursePrediction(selected, predData)
              return (
                <div className="mt-5 grid grid-cols-2 gap-3 text-sm border-t pt-4">
                  <div className="rounded-xl bg-muted p-3">
                    <span className="text-xs text-muted-foreground block">Verified Recorded Score</span>
                    <b className="mt-1 block text-base">{selectedCalc.scoreText}</b>
                    <span className="text-[11px] text-muted-foreground block">{selectedCalc.scoreSub}</span>
                  </div>
                  <div className="rounded-xl bg-primary/10 p-3">
                    <span className="text-xs text-muted-foreground block">ML Predicted Score</span>
                    {p && p.predictionStatus === "AVAILABLE" && typeof p.predictedOverallScore === "number" ? (
                      <>
                        <b className="mt-1 block text-base text-primary">{p.predictedOverallScore.toFixed(1)}%</b>
                        <span className="text-[11px] text-muted-foreground block capitalize">{p.level} stage estimate · Out of 100</span>
                      </>
                    ) : (
                      <b className="mt-1 block text-base text-muted-foreground">Not available</b>
                    )}
                  </div>
                </div>
              )
            })()}

            <p className="mt-4 text-[11px] text-muted-foreground">
              <b>Distinction:</b> Recorded marks represent verified evaluations stored in MongoDB Atlas. Predictions are machine learning estimates of final overall performance (strictly excluding FAT before FAT completion), not actual final results. Class averages are entered directly by the assigned faculty.
            </p>
          </div>
        </div>
      )}
    </div>
  )
}

function AttendancePage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [data, setData] = useState<StudentAttendanceResponse | null>(null)
  const [selectedCourseCode, setSelectedCourseCode] = useState("all")
  const [conductedInput, setConductedInput] = useState("")
  const [attendedInput, setAttendedInput] = useState("")
  const [plannedInput, setPlannedInput] = useState("60")

  function loadAttendance() {
    setLoading(true)
    setError(null)
    academicApi.attendance()
      .then((res) => {
        setData(res)
        setLoading(false)
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Failed to load attendance records")
        setLoading(false)
      })
  }

  useEffect(() => {
    let cancelled = false
    academicApi.attendance()
      .then((res) => {
        if (!cancelled) {
          setData(res)
          setLoading(false)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Failed to load attendance records")
          setLoading(false)
        }
      })
    return () => { cancelled = true }
  }, [])

  const courses = data?.courses || []
  const totalConducted = courses.reduce((sum, c) => sum + (c.classesConducted ?? 0), 0)
  const totalAttended = courses.reduce((sum, c) => sum + (c.classesAttended ?? 0), 0)
  const overallPercentage = totalConducted > 0 ? (totalAttended / totalConducted) * 100 : 0
  const belowThresholdCourses = courses.filter(c => (c.attendancePercentage ?? 0) < MIN_ATTENDANCE_PERCENTAGE)

  useEffect(() => {
    if (selectedCourseCode === "all") {
      setConductedInput(totalConducted > 0 ? String(totalConducted) : "40")
      setAttendedInput(totalAttended > 0 ? String(totalAttended) : "35")
    } else {
      const target = courses.find(c => c.courseCode === selectedCourseCode)
      if (target) {
        setConductedInput(String(target.classesConducted ?? 40))
        setAttendedInput(String(target.classesAttended ?? 35))
      }
    }
  }, [selectedCourseCode, totalConducted, totalAttended, courses])

  const integer = (value: string) => value !== "" && /^\d+$/.test(value)
  const c = integer(conductedInput) ? Number(conductedInput) : 0
  const a = integer(attendedInput) ? Number(attendedInput) : 0
  const p = integer(plannedInput) ? Number(plannedInput) : 0
  const valid = integer(conductedInput) && integer(attendedInput) && integer(plannedInput) && c >= 0 && a >= 0 && a <= c && p >= c
  const semesterLimit = valid ? p : c
  const remainingClasses = Math.max(0, semesterLimit - c)
  const currentValue = valid ? attendancePercentage(a, c) : null
  const current = currentValue ?? 0
  const need = valid && attendanceStatus(a, c) === "Below 75%" && c > 0 ? Math.ceil((MIN_ATTENDANCE_PERCENTAGE / 100 * c - a) / (1 - MIN_ATTENDANCE_PERCENTAGE / 100)) : 0
  const canMiss = valid && attendanceStatus(a, c) === "On track" && c > 0 ? Math.min(remainingClasses, Math.max(0, Math.floor((a - MIN_ATTENDANCE_PERCENTAGE / 100 * c) / (MIN_ATTENDANCE_PERCENTAGE / 100)))) : 0
  const validationMessage = !integer(conductedInput) ? "Classes conducted must be a non-negative integer." : !integer(attendedInput) ? "Classes attended must be a non-negative integer." : !integer(plannedInput) ? "Planned semester classes must be an integer." : a > c ? "Classes attended cannot exceed classes conducted." : p < c ? "Planned semester classes must be greater than or equal to classes conducted." : ""

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="mt-3 text-3xl font-semibold">Attendance</h2>
        <p className="mt-2 text-muted-foreground">Track presence by course from verified records and plan recovery before you fall below 75%.</p>
      </div>

      {loading && (
        <div className={`${card} flex items-center justify-center p-12 text-sm text-muted-foreground`}>
          <div className="flex items-center gap-3">
            <span className="size-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            <span>Loading verified attendance records…</span>
          </div>
        </div>
      )}

      {error && !loading && (
        <div className={`${card} border-destructive/40 bg-destructive/5 flex items-center justify-between p-5`}>
          <div>
            <h3 className="font-semibold text-destructive">Unable to load attendance</h3>
            <p className="text-sm text-muted-foreground">{error}</p>
          </div>
          <button onClick={loadAttendance} className="rounded-xl border px-3 py-1.5 text-xs font-medium hover:bg-muted">Retry</button>
        </div>
      )}

      {!loading && !error && (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            <StatCard icon={CheckCircle2} label="Overall attendance" value={`${overallPercentage.toFixed(1)}%`} note={`${totalAttended} of ${totalConducted} classes conducted`} />
            <StatCard icon={BookOpen} label="Classes attended" value={totalAttended} note={`Across ${courses.length} enrolled courses`} />
            <StatCard icon={AlertTriangle} label="Below threshold" value={`${belowThresholdCourses.length} course${belowThresholdCourses.length === 1 ? "" : "s"}`} note={`Below ${MIN_ATTENDANCE_PERCENTAGE}% requirement`} />
          </div>

          <div className="grid gap-5 xl:grid-cols-[1fr_1.2fr]">
            <div className={card}>
              <h3 className="font-semibold">Attendance by course</h3>
              <p className="mt-1 text-sm text-muted-foreground">{MIN_ATTENDANCE_PERCENTAGE}% minimum reference</p>
              <div className="mt-5 h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={courses.map(x => ({ code: x.courseCode, attendance: x.attendancePercentage ?? 0 }))}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="code" />
                    <YAxis domain={[50, 100]} />
                    <ReferenceLine y={MIN_ATTENDANCE_PERCENTAGE} stroke="#dc2626" strokeDasharray="5 5" label={`${MIN_ATTENDANCE_PERCENTAGE}%`} />
                    <Tooltip />
                    <Bar dataKey="attendance" fill="#16a34a" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className={card}>
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-semibold">Attendance calculator</h3>
                  <p className="mt-1 text-sm text-muted-foreground">Pre-fill with course figures or calculate scenarios.</p>
                </div>
                {courses.length > 0 && (
                  <select value={selectedCourseCode} onChange={e => setSelectedCourseCode(e.target.value)} className="h-9 rounded-lg border bg-background px-2 text-xs">
                    <option value="all">Overall (All Courses)</option>
                    {courses.map(x => <option key={x.courseCode} value={x.courseCode}>{x.courseCode}</option>)}
                  </select>
                )}
              </div>
              {validationMessage && <p role="alert" className="mt-2 rounded-lg border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">{validationMessage}</p>}
              <div className="mt-4 grid gap-4 sm:grid-cols-3">
                <label className="text-xs font-medium">Classes conducted<input type="number" min="1" value={conductedInput} onChange={e => setConductedInput(e.target.value)} className="mt-1.5 h-9 w-full rounded-xl border bg-background px-3 text-sm" /></label>
                <label className="text-xs font-medium">Classes attended<input type="number" min="0" value={attendedInput} onChange={e => setAttendedInput(e.target.value)} className="mt-1.5 h-9 w-full rounded-xl border bg-background px-3 text-sm" /></label>
                <label className="text-xs font-medium">Planned semester classes<input type="number" min={c || 1} value={plannedInput} onChange={e => setPlannedInput(e.target.value)} className="mt-1.5 h-9 w-full rounded-xl border bg-background px-3 text-sm" /></label>
              </div>
              <div className="mt-4 rounded-xl bg-muted p-4 text-sm">
                Current rate: <b>{current.toFixed(1)}%</b>
                <p className="mt-1.5 text-xs text-muted-foreground">{need ? `Attend the next ${need} consecutive class${need === 1 ? "" : "es"} to reach 75%.` : `You can miss approximately ${canMiss} class${canMiss === 1 ? "" : "es"} and remain at or above 75%.`}</p>
              </div>
            </div>
          </div>

          <div className={card}>
            <h3 className="font-semibold">Course attendance detail</h3>
            <div className="mt-4 overflow-x-auto">
              <table className="w-full min-w-[650px] text-left text-sm">
                <thead className="border-b text-xs uppercase text-muted-foreground">
                  <tr><th className="p-3">Course</th><th className="p-3">Attended</th><th className="p-3">Missed</th><th className="p-3">Total</th><th className="p-3">Rate</th><th className="p-3">Status</th></tr>
                </thead>
                <tbody>
                  {courses.map(x => {
                    const isBelow = (x.attendancePercentage ?? 0) < MIN_ATTENDANCE_PERCENTAGE
                    return (
                      <tr className="border-b last:border-0" key={x.courseCode}>
                        <td className="p-3 font-medium">
                          {x.courseName}
                          <span className="block text-xs text-muted-foreground">{x.courseCode}{x.credits ? ` · ${x.credits} credits` : ""}</span>
                        </td>
                        <td className="p-3">{x.classesAttended ?? 0}</td>
                        <td className="p-3">{x.classesMissed ?? 0}</td>
                        <td className="p-3">{x.classesConducted ?? 0}</td>
                        <td className="p-3 font-semibold">{x.attendancePercentage != null ? `${x.attendancePercentage}%` : "—"}</td>
                        <td className={`p-3 font-medium ${isBelow ? "text-destructive" : "text-emerald-700 dark:text-emerald-300"}`}>
                          {isBelow ? `Below ${MIN_ATTENDANCE_PERCENTAGE}%` : "On track"}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

function PerformancePage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [marksData, setMarksData] = useState<StudentMarksResponse | null>(null)

  function loadPerformance() {
    setLoading(true)
    setError(null)
    academicApi.marks()
      .then((res) => {
        setMarksData(res)
        setLoading(false)
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Failed to load performance analytics")
        setLoading(false)
      })
  }

  useEffect(() => {
    let cancelled = false
    academicApi.marks()
      .then((res) => {
        if (!cancelled) {
          setMarksData(res)
          setLoading(false)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Failed to load performance analytics")
          setLoading(false)
        }
      })
    return () => { cancelled = true }
  }, [])

  const courses = marksData?.courses || []

  const chartData = courses.map(c => {
    const calc = calculateCourseRecordedScore(c)
    return {
      name: c.courseCode,
      CAT1: calc.cat1Raw,
      CAT2: calc.cat2Raw,
      Quiz1: calc.quiz1Raw,
      Quiz2: calc.quiz2Raw,
      Quiz3: calc.quiz3Raw,
      FAT: calc.fatRaw,
      LabAverage: calc.labScore !== null ? Math.round((calc.labScore / 10) * 10) / 10 : null
    }
  })

  const completedEvidence = courses.flatMap(c =>
    c.assessments
      .filter(a => a.status === "COMPLETED" && (typeof a.rawMarks === "number" || typeof (a.obtainedMarks ?? a.marksObtained) === "number"))
      .map(a => {
        const raw = typeof a.rawMarks === "number" ? Math.round(a.rawMarks * 10) / 10 : (a.obtainedMarks ?? a.marksObtained)
        const rawMax = a.rawMaxMarks ?? a.maxMarks ?? 10
        return {
          id: a.id || `${c.courseCode}-${a.assessmentName}`,
          courseCode: c.courseCode,
          courseName: c.courseName,
          label: a.assessmentName,
          marks: raw,
          maxMarks: rawMax,
          weightage: a.weightage,
          classAverage: a.classAverage
        }
      })
  )

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="mt-3 text-3xl font-semibold">Performance analytics</h2>
        <p className="mt-2 text-muted-foreground">Current-semester assessment marks and evidence from verified university records.</p>
      </div>

      {loading && (
        <div className={`${card} flex items-center justify-center p-12 text-sm text-muted-foreground`}>
          <div className="flex items-center gap-3">
            <span className="size-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            <span>Loading verified performance analytics…</span>
          </div>
        </div>
      )}

      {error && !loading && (
        <div className={`${card} border-destructive/40 bg-destructive/5 flex items-center justify-between p-5`}>
          <div>
            <h3 className="font-semibold text-destructive">Unable to load performance</h3>
            <p className="text-sm text-muted-foreground">{error}</p>
          </div>
          <button onClick={loadPerformance} className="rounded-xl border px-3 py-1.5 text-xs font-medium hover:bg-muted">Retry</button>
        </div>
      )}

      {!loading && !error && (
        <div className="grid gap-5 xl:grid-cols-2">
          <div className={card}>
            <h3 className="font-semibold">Current-semester marks</h3>
            <p className="mt-1 text-sm text-muted-foreground">Scales: CATs /50, Quizzes /10, Labs /10, FAT /100. Pending assessments are omitted.</p>
            <div className="mt-4 h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" />
                  <YAxis domain={[0, 100]} />
                  <Tooltip />
                  <Bar dataKey="CAT1" fill="#2563eb" name="CAT 1 (/50)" />
                  <Bar dataKey="CAT2" fill="#7c3aed" name="CAT 2 (/50)" />
                  <Bar dataKey="Quiz1" fill="#059669" name="Quiz 1 (/10)" />
                  <Bar dataKey="Quiz2" fill="#d97706" name="Quiz 2 (/10)" />
                  <Bar dataKey="Quiz3" fill="#14b8a6" name="Quiz 3 (/10)" />
                  <Bar dataKey="FAT" fill="#dc2626" name="FAT (/100)" />
                  <Bar dataKey="LabAverage" fill="#0891b2" name="Lab avg (/10)" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className={card}>
            <div className="flex items-center justify-between">
              <h3 className="font-semibold">Assessment evidence</h3>
              <span className="text-xs text-muted-foreground">{completedEvidence.length} completed evaluation{completedEvidence.length === 1 ? "" : "s"}</span>
            </div>
            <p className="mt-1 text-xs text-muted-foreground">Verified evaluation submissions recorded for your account.</p>
            <div className="mt-4 flex flex-col gap-2.5 max-h-80 overflow-y-auto pr-1">
              {completedEvidence.length > 0 ? (
                completedEvidence.map(r => (
                  <div className="flex items-center justify-between rounded-xl bg-muted/40 p-3 text-sm" key={r.id}>
                    <div>
                      <span className="font-medium">{r.courseCode} · {r.label}</span>
                      <span className="block text-[11px] text-muted-foreground">{r.courseName}</span>
                    </div>
                    <div className="text-right">
                      <span className="font-semibold">{r.marks} / {r.maxMarks}</span>
                      <span className="block text-[11px] text-muted-foreground">
                        Class avg: <b>{r.classAverage != null ? `${r.classAverage} / ${r.maxMarks}` : "Not entered"}</b>
                      </span>
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-xs text-muted-foreground p-4">No completed assessments recorded yet.</p>
              )}
            </div>
            <div className="mt-4 rounded-xl border border-dashed bg-muted/20 p-3 text-xs text-muted-foreground">
              <b>Class comparison status:</b> Class averages are entered and updated directly by your assigned course faculty.
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function PredictionsPage() {
  const [loading, setLoading] = useState(true);
  const [switchingLevel, setSwitchingLevel] = useState(false);
  const [data, setData] = useState<PredictionResponse | null>(null);
  const [availability, setAvailability] = useState<PredictionAvailabilityResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Default to Level 1 when page opens, or respect persisted student preference if present
  const [globalLevel, setGlobalLevel] = useState<string>(() => {
    if (typeof window !== "undefined") {
      const persisted = sessionStorage.getItem("sac_prediction_level");
      if (persisted && ["level1", "level2", "level3"].includes(persisted)) {
        return persisted;
      }
    }
    return "level1";
  });

  const LEVEL_CONFIGS = [
    {
      id: "level1",
      label: "Level 1",
      title: "Early Forecast",
      badge: "CAT 1 + Quiz 1",
      summary: "Baseline theory forecast after early semester evaluations.",
      inputs: ["cat1", "quiz1"],
    },
    {
      id: "level2",
      label: "Level 2",
      title: "Mid-Semester",
      badge: "CAT 1 & 2 + Quiz 1 & 2",
      summary: "Refined forecast incorporating mid-term continuous assessments.",
      inputs: ["cat1", "cat2", "quiz1", "quiz2"],
    },
    {
      id: "level3",
      label: "Level 3",
      title: "Pre-FAT Comprehensive",
      badge: "Quizzes 1–3 + 10 Labs",
      summary: "Full internal continuous assessment forecast before final FAT examinations.",
      inputs: ["cat1", "cat2", "quiz1", "quiz2", "quiz3", ...Array.from({ length: 10 }, (_, i) => `lab${i + 1}`)],
    },
  ];

  function loadPredictionsForLevel(levelToLoad: string, isInitial = false) {
    if (isInitial) {
      setLoading(true);
    } else {
      setSwitchingLevel(true);
    }
    setError(null);

    Promise.all([
      academicApi.prediction(levelToLoad),
      academicApi.predictionAvailability().catch(() => null),
    ])
      .then(([predRes, availRes]) => {
        setData(predRes);
        setAvailability(availRes);
        setLoading(false);
        setSwitchingLevel(false);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Failed to load predictions");
        setLoading(false);
        setSwitchingLevel(false);
      });
  }

  useEffect(() => {
    loadPredictionsForLevel(globalLevel, true);
  }, []);

  function handleSelectGlobalLevel(newLevel: string) {
    if (newLevel === globalLevel && data) return;
    setGlobalLevel(newLevel);
    if (typeof window !== "undefined") {
      sessionStorage.setItem("sac_prediction_level", newLevel);
    }
    loadPredictionsForLevel(newLevel, false);
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="mt-3 text-3xl font-semibold">Academic predictions</h2>
        <p className="mt-2 text-muted-foreground">
          Machine learning forecasts generated from verified course assessment marks. Select an eligible prediction level above to evaluate your trajectory across all enrolled courses. Overall scores predict performance out of 100 (FAT marks strictly excluded).
        </p>
      </div>

      {loading && (
        <div className={`${card} flex items-center justify-center p-12 text-sm text-muted-foreground`}>
          <div className="flex items-center gap-3">
            <span className="size-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            <span>Fetching live ML course predictions for {globalLevel.toUpperCase()}…</span>
          </div>
        </div>
      )}

      {error && !loading && (
        <div className={`${card} border-destructive/40 bg-destructive/5`}>
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-destructive/10 text-destructive">
                <AlertTriangle className="size-5" />
              </div>
              <div>
                <h3 className="font-semibold text-destructive">Unable to load predictions</h3>
                <p className="mt-1 text-sm text-muted-foreground">{error}</p>
                <p className="mt-2 text-xs text-muted-foreground">
                  Ensure you are signed in with a valid student account and the backend/ML services are running.
                </p>
              </div>
            </div>
            <button
              onClick={() => loadPredictionsForLevel(globalLevel, true)}
              className="rounded-xl border bg-background px-4 py-2 text-sm font-medium hover:bg-muted"
            >
              Retry
            </button>
          </div>
        </div>
      )}

      {!loading && !error && data && (
        <>
          <div className={`${card} ${data.mlServiceHealthy ? "border-emerald-500/40 bg-emerald-500/5" : "border-amber-500/40 bg-amber-500/5"}`}>
            <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
              <div className="flex items-center gap-3">
                <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${data.mlServiceHealthy ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400" : "bg-amber-500/10 text-amber-600 dark:text-amber-400"}`}>
                  <Sparkles className="size-5" />
                </div>
                <div>
                  <h3 className="font-semibold">
                    {data.mlServiceHealthy ? "Flask ML Prediction Service Connected (Lazy Single-Model Cache)" : "Prediction Service Offline"}
                  </h3>
                  <p className="text-xs text-muted-foreground">
                    {data.predictableCourses} of {data.totalCourses} enrolled courses evaluated at {globalLevel.toUpperCase()} · Student: {data.fullName} ({data.studentNumber})
                  </p>
                </div>
              </div>
              <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${data.mlServiceHealthy ? "bg-emerald-500/20 text-emerald-700 dark:text-emerald-300" : "bg-amber-500/20 text-amber-700 dark:text-amber-300"}`}>
                <span className={`size-1.5 rounded-full ${data.mlServiceHealthy ? "bg-emerald-600" : "bg-amber-600"}`} />
                {data.mlServiceHealthy ? "Active Service" : "Service Offline"}
              </span>
            </div>
          </div>

          {/* Global Prediction Level Selector at Top of Page */}
          <section className={`${card} border-primary/20 bg-card`}>
            <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
              <div>
                <div className="flex items-center gap-2">
                  <span className="flex size-7 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <Layers className="size-4" />
                  </span>
                  <h3 className="font-semibold text-lg">Global Prediction Level Selection</h3>
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  Select a single prediction level applied uniformly across all enrolled courses to prevent redundant ML model reloading.
                </p>
              </div>

              {switchingLevel && (
                <div className="flex items-center gap-2 rounded-full border border-primary/30 bg-primary/5 px-3 py-1 text-xs font-medium text-primary shrink-0">
                  <span className="size-3 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                  <span>Switching model to {globalLevel.toUpperCase()}…</span>
                </div>
              )}
            </div>

            <div className="mt-4 grid gap-3 md:grid-cols-3">
              {LEVEL_CONFIGS.map((cfg) => {
                const isSelected = globalLevel === cfg.id;
                // Count how many courses are eligible for this level
                const eligibleCoursesCount = availability?.courses?.filter(
                  (c) => c.levels?.[cfg.id]?.eligible
                ).length ?? 0;
                const totalCoursesCount = availability?.courses?.length ?? (data?.totalCourses ?? 0);

                return (
                  <button
                    key={cfg.id}
                    type="button"
                    onClick={() => handleSelectGlobalLevel(cfg.id)}
                    disabled={switchingLevel}
                    className={`relative flex flex-col justify-between rounded-xl p-4 text-left transition-all cursor-pointer ${
                      isSelected
                        ? "border-2 border-primary bg-primary/10 text-foreground shadow-sm ring-1 ring-primary/20"
                        : "border bg-card/70 hover:bg-muted/60 hover:border-primary/40 text-muted-foreground"
                    }`}
                  >
                    <div>
                      <div className="flex items-center justify-between gap-2">
                        <span className={`rounded-md px-2 py-0.5 text-xs font-bold ${
                          isSelected ? "bg-primary text-primary-foreground" : "bg-muted text-foreground"
                        }`}>
                          {cfg.label}
                        </span>
                        <span className={`text-[11px] font-mono font-medium ${
                          isSelected ? "text-primary font-semibold" : "text-muted-foreground"
                        }`}>
                          {cfg.badge}
                        </span>
                      </div>
                      <h4 className={`mt-2 font-semibold text-sm ${isSelected ? "text-foreground" : "text-foreground/90"}`}>
                        {cfg.title}
                      </h4>
                      <p className="mt-1 text-xs text-muted-foreground leading-relaxed">
                        {cfg.summary}
                      </p>
                    </div>

                    <div className="mt-4 pt-3 border-t border-border/60 flex items-center justify-between text-[11px]">
                      <span className={isSelected ? "font-medium text-primary" : "text-muted-foreground"}>
                        {totalCoursesCount > 0
                          ? `${eligibleCoursesCount} of ${totalCoursesCount} courses eligible`
                          : "Evaluating requirements…"}
                      </span>
                      {isSelected ? (
                        <span className="flex items-center gap-1 font-semibold text-primary">
                          <Check className="size-3.5 stroke-[3]" />
                          <span>Active</span>
                        </span>
                      ) : (
                        <span className="text-muted-foreground hover:text-foreground">Click to select</span>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          </section>

          {/* Enrolled Courses Predictions */}
          <section className={card}>
            <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
              <div>
                <h3 className="font-semibold text-base">Enrolled Course Predictions (Evaluating at {globalLevel.toUpperCase()})</h3>
                <p className="mt-1 text-xs text-muted-foreground">
                  All courses evaluated using the active {globalLevel.toUpperCase()} model. If a course lacks required assessments, it is flagged as unavailable.
                </p>
              </div>
            </div>

            <div className="mt-5 grid gap-5 lg:grid-cols-2">
              {data.predictions.map((p) => {
                const isAvailable = p.predictionStatus === "AVAILABLE";
                const isInsufficient = p.predictionStatus === "INSUFFICIENT_DATA";
                const isUnsupported = p.predictionStatus === "UNSUPPORTED_COURSE";
                const courseAvail = availability?.courses?.find((c) => c.courseCode === p.courseCode);
                const activeLevelEligibility = courseAvail?.levels?.[globalLevel];
                const activeConfig = LEVEL_CONFIGS.find((cfg) => cfg.id === globalLevel) || LEVEL_CONFIGS[0];

                const riskColor =
                  p.riskLevel === "Low" ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border-emerald-500/30" :
                  p.riskLevel === "Medium" ? "bg-amber-500/10 text-amber-700 dark:text-amber-300 border-amber-500/30" :
                  "bg-rose-500/10 text-rose-700 dark:text-rose-300 border-rose-500/30";

                return (
                  <article className="flex flex-col justify-between rounded-xl border p-5 shadow-xs bg-card/60 transition-all hover:border-primary/40" key={p.courseCode}>
                    <div>
                      {/* Course Header */}
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-mono text-xs font-bold text-muted-foreground">{p.courseCode}</span>
                            <span className="rounded-md bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary capitalize">
                              {activeConfig.label} ({activeConfig.badge})
                            </span>
                          </div>
                          <h4 className="mt-1 font-semibold text-base">{p.courseName}</h4>
                          <p className="mt-1 text-xs text-muted-foreground">
                            {p.stageDescription || (isInsufficient ? "Assessment marks pending" : isUnsupported ? "Course not modeled" : "Prediction active")}
                          </p>
                        </div>

                        {/* Top Score Display */}
                        {isAvailable && p.predictedOverallScore != null && !switchingLevel ? (
                          <div className="text-right shrink-0">
                            <span className="block text-2xl font-bold text-primary">
                              {p.predictedOverallScore.toFixed(1)}%
                            </span>
                            <span className="text-[11px] text-muted-foreground">Predicted Overall</span>
                          </div>
                        ) : switchingLevel ? (
                          <div className="flex items-center gap-2 text-xs text-muted-foreground shrink-0">
                            <span className="size-3.5 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                            <span>Updating…</span>
                          </div>
                        ) : (
                          <span className={`rounded-lg px-2.5 py-1 text-xs font-medium shrink-0 ${
                            isInsufficient ? "bg-amber-500/10 text-amber-700 dark:text-amber-400 border border-amber-500/20" : "bg-muted text-muted-foreground"
                          }`}>
                            {isInsufficient ? "Pending Marks" : isUnsupported ? "Unsupported" : "Unavailable"}
                          </span>
                        )}
                      </div>

                      {/* Ineligible Explanation Banner if course lacks required assessments for this global level */}
                      {!isAvailable && (
                        <div className="mt-4 rounded-lg border border-dashed border-amber-500/40 bg-amber-500/5 p-3 text-xs text-amber-800 dark:text-amber-300 flex items-start gap-2.5">
                          <AlertTriangle className="size-4 shrink-0 mt-0.5 text-amber-600 dark:text-amber-400" />
                          <div>
                            <b>{activeConfig.label} unavailable for {p.courseCode}:</b>{" "}
                            {p.statusMessage || activeLevelEligibility?.reason || "This course does not meet the assessment requirements for the selected prediction level."}
                            <p className="mt-1 text-[11px] text-muted-foreground">
                              Evaluated marks are loaded from authoritative records. Missing assessments are never invented or defaulted to zero.
                            </p>
                          </div>
                        </div>
                      )}

                      {/* Required Assessment Inputs Breakdown for the Selected Level */}
                      <div className="mt-4 rounded-xl border bg-muted/20 p-3">
                        <div className="flex items-center justify-between text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">
                          <span>Inputs for {activeConfig.label}:</span>
                          <span className="text-[10px] font-normal normal-case text-muted-foreground">
                            Authoritative evaluated records
                          </span>
                        </div>

                        <div className="mt-2 flex flex-wrap gap-1.5">
                          {activeConfig.inputs.map((key) => {
                            const normKey = key.toLowerCase();
                            const recordedVal = courseAvail?.recordedMarks?.[normKey] ?? p.normalizedInputMarks?.[normKey];
                            const hasRecord = recordedVal !== undefined && recordedVal !== null;
                            const isLab = normKey.startsWith("lab");
                            const label = normKey.startsWith("cat")
                              ? normKey.toUpperCase() + " (/50)"
                              : isLab
                              ? normKey.toUpperCase() + " (/10)"
                              : normKey.toUpperCase() + " (/10)";

                            return (
                              <div
                                key={key}
                                className={`flex items-center gap-1.5 rounded-lg border px-2 py-1 text-xs font-mono transition-colors ${
                                  hasRecord
                                    ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-800 dark:text-emerald-300"
                                    : "border-dashed border-muted-foreground/30 bg-muted/40 text-muted-foreground"
                                }`}
                              >
                                {hasRecord ? (
                                  <Check className="size-3 text-emerald-600 dark:text-emerald-400 stroke-[3]" />
                                ) : (
                                  <span className="size-1.5 rounded-full bg-muted-foreground" />
                                )}
                                <span>{label}:</span>
                                <b className="font-sans font-semibold">
                                  {hasRecord ? recordedVal.toFixed(1) : "Pending"}
                                </b>
                              </div>
                            );
                          })}
                        </div>
                        <p className="mt-2 text-[10px] text-muted-foreground">
                          Evaluated marks are loaded automatically from authoritative records. Missing assessments are never treated as zero.
                        </p>
                      </div>

                      {/* Prediction Results Banner */}
                      {isAvailable && (
                        <div className="mt-4 flex flex-wrap items-center gap-2">
                          {p.likelyGrade && (
                            <span className="rounded-lg border bg-muted/50 px-2.5 py-1 text-xs font-semibold">
                              Likely Grade: <b className="text-foreground">{p.likelyGrade}</b>
                            </span>
                          )}
                          {p.riskLevel && (
                            <span className={`rounded-lg border px-2.5 py-1 text-xs font-semibold ${riskColor}`}>
                              Risk: {p.riskLevel}
                            </span>
                          )}
                          {p.illustrativeRange && p.illustrativeRange.length >= 2 && (
                            <span className="rounded-lg border bg-muted/30 px-2.5 py-1 text-xs text-muted-foreground">
                              Illustrative Range: {p.illustrativeRange[0].toFixed(1)}% – {p.illustrativeRange[1].toFixed(1)}%
                            </span>
                          )}
                        </div>
                      )}
                    </div>

                    <div className="mt-5 pt-3 border-t text-[11px] text-muted-foreground flex items-center justify-between">
                      <span>{p.completedAssessmentsCount} assessment(s) evaluated</span>
                      <span className="text-emerald-700 dark:text-emerald-400 font-medium">FAT Excluded (Pre-FAT)</span>
                    </div>
                  </article>
                );
              })}
            </div>
          </section>

          <p className="rounded-xl border bg-muted/30 p-4 text-xs text-muted-foreground">
            <b>Disclaimer:</b> {data.disclaimer}
          </p>
        </>
      )}
    </div>
  );
}

function SettingsPage({ theme, setTheme, user }: { theme: string; setTheme: (x: string) => void; user: { name: string; role: Role; email?: string } }) { const [show, setShow] = useState(false), [saved, setSaved] = useState(""), [passwords, setPasswords] = useState({ current: "", next: "", confirm: "" }); function savePassword(e: React.FormEvent) { e.preventDefault(); setSaved( passwords.next.length < 8 ? "New password must be at least 8 characters." : passwords.next !== passwords.confirm ? "Passwords do not match." : "Not connected: password endpoint is not available.") } return <div className="flex flex-col gap-6"><div><h2 className="text-3xl font-semibold">Settings</h2><p className="mt-2 text-muted-foreground">Manage appearance, security, and profile preferences.</p></div><section className={card}><h3 className="font-semibold">Appearance</h3><p className="mt-1 text-sm text-muted-foreground">Theme preference persists across refreshes.</p><div className="mt-4 flex flex-wrap gap-2">{["light", "dark"].map(x => <button key={x} onClick={() => setTheme(x)} className={`rounded-xl border px-4 py-2 text-sm capitalize ${theme === x ? "border-primary bg-primary/10 text-primary" : ""}`}>{x}</button>)}</div></section><section className={card}><h3 className="font-semibold">Change password</h3><p className="mt-1 text-sm text-muted-foreground">Backend status: not connected. No password is changed by this demo form.</p><form onSubmit={savePassword} className="mt-4 grid gap-4 md:grid-cols-3"><label className="text-sm font-medium">Current password<div className="relative mt-2"><input required type={show ? "text" : "password"} value={passwords.current} onChange={e => setPasswords({ ...passwords, current: e.target.value })} className="h-10 w-full rounded-xl border bg-background px-3 pr-10" /><button type="button" onClick={() => setShow(!show)} className="absolute right-3 top-2.5 text-muted-foreground">{show ? <EyeOff /> : <Eye />}</button></div></label><label className="text-sm font-medium">New password<input required type={show ? "text" : "password"} value={passwords.next} onChange={e => setPasswords({ ...passwords, next: e.target.value })} className="mt-2 h-10 w-full rounded-xl border bg-background px-3" /></label><label className="text-sm font-medium">Confirm password<input required type={show ? "text" : "password"} value={passwords.confirm} onChange={e => setPasswords({ ...passwords, confirm: e.target.value })} className="mt-2 h-10 w-full rounded-xl border bg-background px-3" /></label><div className="md:col-span-3 flex items-center gap-4"><button className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground">Save password</button>{saved && <span className="text-sm text-muted-foreground">{saved}</span>}</div></form></section><section className={card}><h3 className="font-semibold">Profile & preferences</h3><div className="mt-4 grid gap-4 sm:grid-cols-3"><div><p className="text-xs text-muted-foreground">Account name</p><p className="mt-1 font-medium">{user.name}</p></div><div><p className="text-xs text-muted-foreground">University email</p><p className="mt-1 font-medium">{user.email || "Email unavailable"}</p></div><div><p className="text-xs text-muted-foreground">Role</p><p className="mt-1 font-medium">{user.role}</p></div></div><p className="mt-5 rounded-xl bg-muted p-3 text-sm text-muted-foreground">Demo preference storage is limited to the theme selector. Profile editing is unavailable until the account endpoint is connected.</p></section></div> }

function ProfilePage({
  user,
  academicYear,
  program,
  studentNumber,
  previousCgpa,
  currentSemesterSgpa,
}: {
  user: { name: string; role: Role; email?: string }
  academicYear?: string
  program?: string
  studentNumber?: string | null
  previousCgpa?: number | null
  currentSemesterSgpa?: number | null
}) {
  const initials = user.name
    .trim()
    .split(/\s+/)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase()

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="mt-3 text-3xl font-semibold">Profile</h2>
        <p className="mt-2 text-muted-foreground">
          Your account information
        </p>
      </div>

      <div className={card}>
        <div className="flex items-center gap-4">
          <div className="flex size-16 items-center justify-center rounded-full bg-primary/10 text-xl font-semibold text-primary">
            {initials}
          </div>
          <div>
            <h3 className="text-xl font-semibold">{user.name}</h3>
            <p className="text-muted-foreground">
              {user.email || "Email unavailable"}
            </p>
          </div>
        </div>

        <div className="mt-6 grid gap-4 border-t pt-5 sm:grid-cols-3">
          <div>
            <p className="text-xs text-muted-foreground">Role</p>
            <p className="mt-1 font-medium capitalize">{user.role}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Program</p>
            <p className="mt-1 font-medium">{program || "Computer Science and Engineering"}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Academic year</p>
            <p className="mt-1 font-medium">{academicYear ?? "2025–26"}</p>
          </div>

          {user.role === "student" && (
            <>
              <div>
                <p className="text-xs text-muted-foreground">Registration Number</p>
                <p className="mt-1 font-mono font-medium text-primary">{studentNumber || "—"}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Previous CGPA</p>
                <p className="mt-1 font-medium">
                  {previousCgpa != null ? `${Number(previousCgpa).toFixed(2)} / 10` : "Historical record unavailable"}
                </p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Current-semester SGPA</p>
                <p className="mt-1 font-medium">
                  {currentSemesterSgpa != null ? `${Number(currentSemesterSgpa).toFixed(2)} / 10` : "Not available"}
                </p>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

function LiveRoleStatus({ role }: { role: "teacher" | "admin" }) { const [state, setState] = useState<"loading" | "connected" | "unavailable">("loading"); useEffect(() => { const request = role === "teacher" ? academicApi.teacherDashboard() : academicApi.adminDashboard(); request.then(() => setState("connected")).catch(() => setState("unavailable")) }, [role]); if (state === "loading") return <p className="rounded-xl border bg-muted/30 p-4 text-sm text-muted-foreground">Checking {role} API access…</p>; if (state === "unavailable") return <p role="status" className="rounded-xl border border-amber-500/40 bg-amber-500/10 p-4 text-sm text-amber-800 dark:text-amber-200">{role === "teacher" ? "Teacher dashboard API is unavailable. Sample records below are demo data only; no teacher action is being claimed as persisted." : "Admin dashboard API is unavailable. Sample records below are demo data only; no admin action is being claimed as persisted."}</p>; return <p role="status" className="rounded-xl border border-emerald-500/40 bg-emerald-500/10 p-4 text-sm text-emerald-800 dark:text-emerald-200">Connected to the {role} dashboard API. Displayed sample records remain labelled demo data until live records are loaded.</p> }

function LiveStudentStatus() { const [state, setState] = useState<"loading" | "connected" | "empty" | "unavailable">("loading"); useEffect(() => { let cancelled = false; academicApi.studentDashboard().then((raw) => { if (cancelled) return; const data = raw as { academicDataAvailable?: boolean } | null; setState(data?.academicDataAvailable ? "connected" : "empty") }).catch(() => { if (!cancelled) setState("unavailable") }); return () => { cancelled = true } }, []); if (state === "loading") return <p role="status" className="rounded-xl border bg-muted/30 p-3 text-sm text-muted-foreground">Checking student dashboard API…</p>; if (state === "unavailable") return <p role="status" className="rounded-xl border border-amber-500/40 bg-amber-500/10 p-3 text-sm text-amber-800 dark:text-amber-200">Student profile API is unavailable.</p>; if (state === "empty") return <p role="status" className="rounded-xl border border-blue-500/30 bg-blue-500/5 p-3 text-sm text-muted-foreground">Student profile API connected. No course enrollments or academic evaluations recorded yet for this student account.</p>; return <p role="status" className="rounded-xl border border-emerald-500/40 bg-emerald-500/10 p-3 text-sm text-emerald-800 dark:text-emerald-200">Student dashboard API connected. All figures reflect verified records from your university account.</p> }

function TeacherPage({ page, theme, setTheme, logout, go }: { page: Page; theme: string; setTheme: (theme: string) => void; logout: () => void; go: (page: Page) => void }) { return <TeacherDashboard page={page as "Overview" | "My Courses" | "Students" | "Assessments" | "Settings"} setPage={go as (page: "Overview" | "My Courses" | "Students" | "Assessments" | "Settings") => void} theme={theme} setTheme={setTheme} logout={logout} /> }

function AdminPage({ page, theme, setTheme, logout, go }: { page: Page; theme: string; setTheme: (theme: string) => void; logout: () => void; go: (page: Page) => void }) { return <AdminDashboard page={page as "Overview" | "User Management" | "Courses & Classes" | "Settings"} theme={theme} setTheme={setTheme} logout={logout} go={go as (page: "Overview" | "User Management" | "Courses & Classes" | "Settings") => void} /> }

type AuthState = { status: "checking" } | { status: "authenticated"; user: { name: string; role: Role } } | { status: "signed-out"; message?: string }

export default function StudentAcademicApp() { const [auth, setAuth] = useState<AuthState>({ status: "checking" }), [role, setRole] = useState<Role>("student"), [active, setActive] = useState<Page>("Overview"), [open, setOpen] = useState(false), [theme, setTheme] = useState("light"), [academicYear, setAcademicYear] = useState<string>("2025–26"), [program, setProgram] = useState<string>("Computer Science and Engineering"), [studentInfo, setStudentInfo] = useState<{ previousCgpa?: number | null; currentSemesterSgpa?: number | null; studentNumber?: string | null }>({}); useEffect(() => { let cancelled = false; const stored = sessionStorage.getItem("sac_theme"); if (stored === "light" || stored === "dark") setTheme(stored); const token = sessionStorage.getItem("sac_token"); if (!token) { setAuth({ status: "signed-out" }); return; } authApi.me().then(result => { if (cancelled) return; if (result.email) sessionStorage.setItem("sac_email", result.email); const normalizedRole = (result.role || "").toLowerCase(); if (VALID_ROLES.includes(normalizedRole as Role)) { const restored = { name: result.fullName, role: normalizedRole as Role }; setRole(restored.role); setAuth({ status: "authenticated", user: restored }) } else { sessionStorage.removeItem("sac_token"); setAuth({ status: "signed-out", message: "Your account role is unavailable." }) } }).catch(() => { if (cancelled) return; sessionStorage.removeItem("sac_token"); setAuth({ status: "signed-out", message: "Your session expired. Please sign in again." }) }); return () => { cancelled = true } }, []); useEffect(() => { if (auth.status === "authenticated") { academicApi.studentDashboard().then(res => { if (res?.academicYear) setAcademicYear(res.academicYear); if (res?.program) setProgram(res.program); setStudentInfo({ previousCgpa: res?.previousCgpa, currentSemesterSgpa: res?.currentSemesterSgpa, studentNumber: res?.studentNumber }); }).catch(() => {}); } }, [auth.status]); useEffect(() => { sessionStorage.setItem("sac_theme", theme); const dashboardTheme = auth.status === "authenticated"; document.documentElement.classList.toggle("dark", dashboardTheme && theme === "dark"); document.documentElement.classList.toggle("light", !dashboardTheme || theme === "light") }, [theme, auth.status]); if (auth.status === "checking") return <main className="flex min-h-screen items-center justify-center bg-background text-sm text-muted-foreground">Checking your session…</main>; if (auth.status === "signed-out") return <Login onLogin={(name, r) => { const next = { name, role: r }; setRole(r); setActive("Overview"); setAuth({ status: "authenticated", user: next }) }} />; const user = auth.user; function logout() { sessionStorage.removeItem("sac_token"); sessionStorage.removeItem("sac_email"); setAuth({ status: "signed-out" }); setRole("student"); setActive("Overview"); window.history.replaceState(null, "", "/") } function studentPage() { if (role === "teacher") return <TeacherPage page={active} theme={theme} setTheme={setTheme} logout={logout} go={setActive} />; if (role === "admin") return <AdminPage page={active} theme={theme} setTheme={setTheme} logout={logout} go={setActive} />; if (active === "Overview") return <Overview go={setActive} name={user.name} />; if (active === "Marks & Results") return <MarksPage />; if (active === "Attendance") return <AttendancePage />; if (active === "Performance") return <PerformancePage />; if (active === "Predictions") return <PredictionsPage />; if (active === "Settings") return <SettingsPage theme={theme} setTheme={setTheme} user={{ name: user.name, role: user.role, email: sessionStorage.getItem("sac_email") ?? undefined }} />;
return <ProfilePage user={{ name: user.name, role: user.role, email: sessionStorage.getItem("sac_email") ?? undefined }} academicYear={academicYear} program={program} studentNumber={studentInfo.studentNumber} previousCgpa={studentInfo.previousCgpa} currentSemesterSgpa={studentInfo.currentSemesterSgpa} /> } return <div className="flex min-h-screen bg-background"><Sidebar role={role} active={active} setActive={setActive} logout={logout} open={open} close={() => setOpen(false)} /><div className="min-w-0 flex-1"><header className="flex h-20 items-center justify-between border-b bg-card px-5 sm:px-8"><div className="flex items-center gap-3"><button onClick={() => setOpen(true)} className="lg:hidden"><Menu /></button><div><p className="text-xs text-muted-foreground">Academic year {academicYear}</p><h1 className="text-lg font-semibold">{active}</h1></div></div><div className="flex items-center gap-3"><div className="hidden text-right sm:block"><p className="text-sm font-medium">{user.name}</p><p className="text-xs capitalize text-muted-foreground">{role} account</p></div><button onClick={() => setActive("Profile")} className="flex size-9 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">{user.name.split(" ").map(x => x[0]).join("").slice(0, 2)}</button></div></header><main className="mx-auto max-w-[1400px] p-5 sm:p-8">{active === "Profile" ? <ProfilePage user={{ name: user.name, role: user.role, email: sessionStorage.getItem("sac_email") ?? undefined }} academicYear={academicYear} program={program} studentNumber={studentInfo.studentNumber} previousCgpa={studentInfo.previousCgpa} currentSemesterSgpa={studentInfo.currentSemesterSgpa} /> : role === "student" ? studentPage() : role === "teacher" ? <TeacherPage page={active} theme={theme} setTheme={setTheme} logout={logout} go={setActive} /> : <AdminPage page={active} theme={theme} setTheme={setTheme} logout={logout} go={setActive} />}</main></div></div> }
