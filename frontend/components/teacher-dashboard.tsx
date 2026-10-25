"use client"

import { useEffect, useMemo, useState } from "react"
import {
  AlertTriangle,
  BookOpen,
  CheckCircle2,
  ClipboardList,
  Edit3,
  GraduationCap,
  RefreshCw,
  Save,
  Search,
  Settings,
  ShieldCheck,
  UserCheck,
  Users,
  X,
} from "lucide-react"
import {
  academicApi,
  type ClassMarksResponse,
  type CumulativeCourseAverage,
  type TeacherAssessmentDetail,
  type TeacherClass,
  type TeacherStudentRoster,
} from "@/lib/api-client"

const card = "rounded-2xl border bg-card p-5 shadow-sm"
const input = "h-10 rounded-xl border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"

type Page = "Overview" | "My Courses" | "Students" | "Assessments" | "Settings"

function Button({
  children,
  onClick,
  variant = "primary",
  type = "button",
  disabled = false,
  className = "",
}: {
  children: React.ReactNode
  onClick?: () => void
  variant?: "primary" | "outline" | "danger"
  type?: "button" | "submit"
  disabled?: boolean
  className?: string
}) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`inline-flex items-center justify-center gap-2 rounded-xl px-3.5 py-2 text-sm font-medium transition-colors disabled:opacity-50 ${
        variant === "primary"
          ? "bg-primary text-primary-foreground hover:bg-primary/90"
          : variant === "danger"
          ? "border border-destructive/40 text-destructive hover:bg-destructive/10"
          : "border bg-background hover:bg-muted"
      } ${className}`}
    >
      {children}
    </button>
  )
}

export function TeacherDashboard({
  page,
  setPage,
  theme,
  setTheme,
  logout,
}: {
  page: Page
  setPage: (page: Page) => void
  theme: string
  setTheme: (theme: string) => void
  logout: () => void
}) {
  const [classes, setClasses] = useState<TeacherClass[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedClassId, setSelectedClassId] = useState<string>("")

  const loadClasses = () => {
    setLoading(true)
    setError(null)
    academicApi
      .teacherClasses()
      .then((res) => {
        const cls = res.classes || []
        setClasses(cls)
        if (cls.length > 0 && (!selectedClassId || !cls.some((c) => c.classId === selectedClassId))) {
          setSelectedClassId(cls[0].classId)
        }
        setLoading(false)
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Failed to load assigned classes")
        setLoading(false)
      })
  }

  useEffect(() => {
    loadClasses()
  }, [])

  const selectedClass = classes.find((c) => c.classId === selectedClassId) ?? classes[0]

  return (
    <div className="flex flex-col gap-7">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <span className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
            <ShieldCheck size={14} /> FACULTY PORTAL · ASSIGNED CLASSES
          </span>
          <h2 className="mt-3 text-3xl font-semibold tracking-tight">
            {page === "Overview" ? "Faculty dashboard" : page}
          </h2>
          <p className="mt-2 text-muted-foreground">
            Manage student evaluations, official course averages, and assessment marks for your assigned classes.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={loadClasses}>
            <RefreshCw size={15} /> Refresh
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20 text-muted-foreground">
          <RefreshCw className="mr-2 animate-spin" size={20} /> Loading faculty assignments...
        </div>
      ) : error ? (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/10 p-6 text-destructive">
          <p className="font-semibold">Unable to access faculty portal</p>
          <p className="mt-1 text-sm">{error}</p>
          <Button variant="outline" onClick={loadClasses} className="mt-4">
            Retry Connection
          </Button>
        </div>
      ) : (
        <>
          {page === "Overview" && (
            <Overview classes={classes} setPage={setPage} setSelectedClassId={setSelectedClassId} />
          )}
          {page === "My Courses" && (
            <MyCourses
              classes={classes}
              setPage={setPage}
              selectedClassId={selectedClassId}
              setSelectedClassId={setSelectedClassId}
            />
          )}
          {page === "Students" && (
            <StudentsView
              classes={classes}
              selectedClassId={selectedClassId}
              setSelectedClassId={setSelectedClassId}
            />
          )}
          {page === "Assessments" && (
            <AssessmentsView
              classes={classes}
              selectedClassId={selectedClassId}
              setSelectedClassId={setSelectedClassId}
            />
          )}
          {page === "Settings" && (
            <TeacherSettings theme={theme} setTheme={setTheme} logout={logout} />
          )}
        </>
      )}
    </div>
  )
}

function Overview({
  classes,
  setPage,
  setSelectedClassId,
}: {
  classes: TeacherClass[]
  setPage: (p: Page) => void
  setSelectedClassId: (id: string) => void
}) {
  const totalEnrolled = classes.reduce((sum, c) => sum + (c.enrolledCount || 0), 0)
  const totalAssessments = classes.reduce((sum, c) => sum + (c.assessments?.length || 0), 0)
  const uniqueCourses = new Set(classes.map((c) => c.courseCode))

  const statItems: Array<[React.ElementType, string, string | number, string]> = [
    [BookOpen, "Assigned courses", uniqueCourses.size, [...uniqueCourses].join(", ") || "None"],
    [Users, "Class sections", classes.length, classes.map((c) => `Sec ${c.section}`).join(", ") || "None"],
    [GraduationCap, "Enrolled students", totalEnrolled, "Across your assigned sections"],
    [ClipboardList, "Standard assessments", totalAssessments, "CATs, Quizzes, Labs & FAT"],
  ]

  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {statItems.map(([Icon, label, value, note]) => (
          <div className={card} key={label as string}>
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Icon size={20} />
            </div>
            <p className="mt-4 text-sm text-muted-foreground">{label}</p>
            <p className="mt-1 text-2xl font-semibold">{value}</p>
            <p className="mt-1 text-xs text-muted-foreground">{note}</p>
          </div>
        ))}
      </div>

      <section className={card}>
        <div className="flex items-center justify-between">
          <div>
            <h3 className="font-semibold">My Assigned Class Offerings</h3>
            <p className="mt-1 text-sm text-muted-foreground">
              Select a class section to view enrolled students or enter evaluation marks.
            </p>
          </div>
          <Button variant="outline" onClick={() => setPage("My Courses")}>
            View All Courses
          </Button>
        </div>

        <div className="mt-5 grid gap-4 md:grid-cols-2">
          {classes.map((c) => (
            <div
              key={c.classId}
              className="rounded-xl border bg-muted/20 p-4 transition-all hover:bg-muted/40"
            >
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-xs font-semibold uppercase text-primary">
                    {c.courseCode}
                  </span>
                  <h4 className="mt-1 text-lg font-semibold">{c.courseName}</h4>
                  <p className="text-sm text-muted-foreground">Section {c.section}</p>
                </div>
                <span className="rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-700 dark:text-emerald-300">
                  {c.enrolledCount ?? 0} Students
                </span>
              </div>

              <div className="mt-5 flex flex-wrap gap-2">
                <Button
                  variant="outline"
                  onClick={() => {
                    setSelectedClassId(c.classId)
                    setPage("Students")
                  }}
                >
                  <Users size={15} /> Student Roster
                </Button>
                <Button
                  onClick={() => {
                    setSelectedClassId(c.classId)
                    setPage("Assessments")
                  }}
                >
                  <Edit3 size={15} /> Enter Marks
                </Button>
              </div>
            </div>
          ))}

          {classes.length === 0 && (
            <p className="py-8 text-center text-sm text-muted-foreground md:col-span-2">
              No classes are currently assigned to your faculty account.
            </p>
          )}
        </div>
      </section>
    </div>
  )
}

function MyCourses({
  classes,
  setPage,
  selectedClassId,
  setSelectedClassId,
}: {
  classes: TeacherClass[]
  setPage: (p: Page) => void
  selectedClassId: string
  setSelectedClassId: (id: string) => void
}) {
  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-4 md:grid-cols-2">
        {classes.map((c) => (
          <article className={card} key={c.classId}>
            <div className="flex items-start justify-between">
              <div>
                <span className="text-xs font-semibold uppercase text-primary">
                  {c.courseCode}
                </span>
                <h3 className="mt-1 text-xl font-semibold">{c.courseName}</h3>
                <p className="mt-1 text-sm text-muted-foreground">
                  Section {c.section} · Theory & Laboratory
                </p>
              </div>
              <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
                Assigned
              </span>
            </div>

            <div className="mt-6 grid grid-cols-2 gap-4 border-y py-4 text-sm">
              <div>
                <span className="block text-xs text-muted-foreground">Enrolled Students</span>
                <span className="text-lg font-semibold">{c.enrolledCount ?? 0}</span>
              </div>
              <div>
                <span className="block text-xs text-muted-foreground">Assessments Configured</span>
                <span className="text-lg font-semibold">{c.assessments?.length ?? 16}</span>
              </div>
            </div>

            <div className="mt-5 flex flex-wrap gap-2">
              <Button
                variant="outline"
                onClick={() => {
                  setSelectedClassId(c.classId)
                  setPage("Students")
                }}
              >
                <Users size={15} /> View Students
              </Button>
              <Button
                onClick={() => {
                  setSelectedClassId(c.classId)
                  setPage("Assessments")
                }}
              >
                <Edit3 size={15} /> Evaluate Marks & Averages
              </Button>
            </div>
          </article>
        ))}
      </div>
    </div>
  )
}

function StudentsView({
  classes,
  selectedClassId,
  setSelectedClassId,
}: {
  classes: TeacherClass[]
  selectedClassId: string
  setSelectedClassId: (id: string) => void
}) {
  const [students, setStudents] = useState<TeacherStudentRoster[]>([])
  const [loading, setLoading] = useState(false)
  const [search, setSearch] = useState("")

  const loadStudents = (classId: string) => {
    if (!classId) return
    setLoading(true)
    academicApi
      .teacherClassStudents(classId)
      .then((res) => {
        setStudents(res.students || [])
        setLoading(false)
      })
      .catch((err) => {
        alert(err instanceof Error ? err.message : "Failed to load student roster")
        setLoading(false)
      })
  }

  useEffect(() => {
    if (selectedClassId) {
      loadStudents(selectedClassId)
    }
  }, [selectedClassId])

  const filtered = useMemo(() => {
    const q = search.toLowerCase()
    return students.filter(
      (s) =>
        !search ||
        s.fullName.toLowerCase().includes(q) ||
        s.studentNumber.toLowerCase().includes(q) ||
        s.email.toLowerCase().includes(q)
    )
  }, [students, search])

  const selectedClass = classes.find((c) => c.classId === selectedClassId)

  return (
    <section className={card}>
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="flex flex-1 flex-col gap-3 sm:flex-row">
          <label className="relative flex-1">
            <Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by student name or registration number (e.g. 25BCE2501)..."
              className={`${input} w-full pl-9`}
            />
          </label>
          <select
            value={selectedClassId}
            onChange={(e) => setSelectedClassId(e.target.value)}
            className={`${input} min-w-[200px] font-medium`}
          >
            {classes.map((c) => (
              <option key={c.classId} value={c.classId}>
                {c.courseCode} - Section {c.section}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="py-12 text-center text-sm text-muted-foreground">
          <RefreshCw className="mx-auto mb-2 animate-spin" size={18} />
          Loading enrolled students from MongoDB...
        </div>
      ) : (
        <div className="mt-5 overflow-x-auto">
          <table className="w-full min-w-[800px] text-left text-sm">
            <thead className="border-b text-xs uppercase text-muted-foreground">
              <tr>
                {["Registration No.", "Full Name", "University Email", "Section", "Attendance", "Previous CGPA", "Academic Status"].map(
                  (h) => (
                    <th className="p-3 font-semibold" key={h}>
                      {h}
                    </th>
                  )
                )}
              </tr>
            </thead>
            <tbody>
              {filtered.map((s) => {
                const att = s.attendancePercentage
                const isGood = att != null && att >= 75
                return (
                  <tr className="border-b last:border-0 hover:bg-muted/30" key={s.studentId}>
                    <td className="p-3 font-mono font-semibold text-primary">{s.studentNumber}</td>
                    <td className="p-3 font-medium">{s.fullName}</td>
                    <td className="p-3 text-muted-foreground">{s.email}</td>
                    <td className="p-3">Section {s.section}</td>
                    <td className="p-3 font-medium">
                      {att != null ? `${att}%` : "Not recorded"}
                    </td>
                    <td className="p-3 font-medium">
                      {s.previousCgpa != null ? Number(s.previousCgpa).toFixed(2) : "Not recorded"}
                    </td>
                    <td className="p-3">
                      <span
                        className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                          att == null
                            ? "bg-muted text-muted-foreground"
                            : isGood
                            ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
                            : "bg-amber-500/10 text-amber-700 dark:text-amber-300"
                        }`}
                      >
                        {att == null ? "Pending" : isGood ? "On Track" : "Needs Attention"}
                      </span>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          {filtered.length === 0 && (
            <p className="p-6 text-center text-sm text-muted-foreground">
              No students found for this class section.
            </p>
          )}
        </div>
      )}
    </section>
  )
}

function AssessmentsView({
  classes,
  selectedClassId,
  setSelectedClassId,
}: {
  classes: TeacherClass[]
  selectedClassId: string
  setSelectedClassId: (id: string) => void
}) {
  const [viewMode, setViewMode] = useState<"averages" | "marks">("averages")
  const [assessments, setAssessments] = useState<TeacherAssessmentDetail[]>([])
  const [marksData, setMarksData] = useState<ClassMarksResponse | null>(null)
  const [cumulativeAvg, setCumulativeAvg] = useState<CumulativeCourseAverage | null>(null)
  const [loading, setLoading] = useState(false)
  const [savingAvgId, setSavingAvgId] = useState<string | null>(null)
  const [avgInputs, setAvgInputs] = useState<Record<string, string>>({})
  const [feedback, setFeedback] = useState<Record<string, { ok: boolean; msg: string }>>({})

  // Mark Entry state
  const [selectedAssessmentId, setSelectedAssessmentId] = useState<string>("")
  const [marksInputs, setMarksInputs] = useState<Record<string, string>>({})
  const [savingMarks, setSavingMarks] = useState(false)
  const [marksFeedback, setMarksFeedback] = useState<string | null>(null)

  const loadAssessments = (classId: string) => {
    if (!classId) return
    setLoading(true)
    academicApi
      .teacherClassAssessments(classId)
      .then((res) => {
        const list = res.assessments || []
        setAssessments(list)
        if (res.cumulativeCourseAverage) {
          setCumulativeAvg(res.cumulativeCourseAverage)
        }
        if (list.length > 0 && !selectedAssessmentId) {
          setSelectedAssessmentId(list[0].id)
        }
        const inputs: Record<string, string> = {}
        list.forEach((a) => {
          if (a.officialClassAverage != null) {
            inputs[a.id] = String(a.officialClassAverage)
          }
        })
        setAvgInputs(inputs)
        setLoading(false)
      })
      .catch((err) => {
        alert(err instanceof Error ? err.message : "Failed to load class assessments")
        setLoading(false)
      })
  }

  const loadMarks = (classId: string) => {
    if (!classId) return
    academicApi
      .teacherClassMarks(classId)
      .then((res) => {
        setMarksData(res)
        if (res.cumulativeCourseAverage) {
          setCumulativeAvg(res.cumulativeCourseAverage)
        }
        // If an assessment is selected, populate marksInputs
        const targetAid = selectedAssessmentId || (res.assessments && res.assessments[0]?.id)
        if (targetAid) {
          const map: Record<string, string> = {}
          res.students?.forEach((st) => {
            const val = st.marks?.[targetAid]
            if (val != null) map[st.studentId] = String(val)
          })
          setMarksInputs(map)
        }
      })
      .catch((err) => {
        alert(err instanceof Error ? err.message : "Failed to load student marks")
      })
  }

  useEffect(() => {
    if (selectedClassId) {
      loadAssessments(selectedClassId)
      loadMarks(selectedClassId)
    }
  }, [selectedClassId])

  useEffect(() => {
    if (marksData && selectedAssessmentId) {
      const map: Record<string, string> = {}
      marksData.students?.forEach((st) => {
        const val = st.marks?.[selectedAssessmentId]
        if (val != null) map[st.studentId] = String(val)
      })
      setMarksInputs(map)
      setMarksFeedback(null)
    }
  }, [selectedAssessmentId, marksData])

  const handleSaveOfficialAverage = async (a: TeacherAssessmentDetail) => {
    const rawVal = avgInputs[a.id]?.trim()
    let numericVal: number | null = null

    if (rawVal !== "" && rawVal !== undefined) {
      const parsed = Number(rawVal)
      if (!Number.isFinite(parsed) || parsed < 0 || parsed > a.rawMaxMarks) {
        setFeedback((prev) => ({
          ...prev,
          [a.id]: {
            ok: false,
            msg: `Must be between 0.0 and ${a.rawMaxMarks}`,
          },
        }))
        return
      }
      numericVal = parsed
    }

    setSavingAvgId(a.id)
    try {
      await academicApi.updateClassAverage(a.id, numericVal)
      setFeedback((prev) => ({
        ...prev,
        [a.id]: {
          ok: true,
          msg: numericVal != null ? `Official average set to ${numericVal}` : "Cleared official average",
        },
      }))
      loadAssessments(selectedClassId)
    } catch (err) {
      setFeedback((prev) => ({
        ...prev,
        [a.id]: {
          ok: false,
          msg: err instanceof Error ? err.message : "Failed to save official average",
        },
      }))
    } finally {
      setSavingAvgId(null)
    }
  }

  const handleSaveMarks = async () => {
    if (!selectedClassId || !selectedAssessmentId) return
    const targetAssessment = assessments.find((a) => a.id === selectedAssessmentId)
    const maxVal = targetAssessment?.rawMaxMarks ?? 100

    // Validate marks
    for (const [studentId, rawVal] of Object.entries(marksInputs)) {
      if (rawVal.trim() !== "") {
        const parsed = Number(rawVal.trim())
        if (!Number.isFinite(parsed) || parsed < 0 || parsed > maxVal) {
          setMarksFeedback(`Invalid mark: Must be between 0 and ${maxVal}`)
          return
        }
      }
    }

    setSavingMarks(true)
    setMarksFeedback(null)

    const entries = Object.entries(marksInputs).map(([studentId, val]) => ({
      studentId,
      assessmentId: selectedAssessmentId,
      marks: val.trim() !== "" ? Number(val.trim()) : null,
    }))

    try {
      await academicApi.updateClassMarks(selectedClassId, { entries })
      setMarksFeedback("Marks saved successfully to MongoDB.")
      loadAssessments(selectedClassId)
      loadMarks(selectedClassId)
    } catch (err) {
      setMarksFeedback(err instanceof Error ? err.message : "Failed to save marks")
    } finally {
      setSavingMarks(false)
    }
  }

  const selectedAssessment = assessments.find((a) => a.id === selectedAssessmentId)

  return (
    <div className="flex flex-col gap-6">
      <div className={card}>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <label className="text-sm font-semibold">Active Class Section:</label>
            <select
              value={selectedClassId}
              onChange={(e) => setSelectedClassId(e.target.value)}
              className={`${input} font-medium`}
            >
              {classes.map((c) => (
                <option key={c.classId} value={c.classId}>
                  {c.courseCode} - Section {c.section} ({c.courseName})
                </option>
              ))}
            </select>
          </div>

          <div className="flex gap-2">
            <button
              onClick={() => setViewMode("averages")}
              className={`rounded-xl px-4 py-2 text-sm font-semibold transition-colors ${
                viewMode === "averages"
                  ? "bg-primary text-primary-foreground"
                  : "border bg-background hover:bg-muted"
              }`}
            >
              Class Averages Comparison
            </button>
            <button
              onClick={() => setViewMode("marks")}
              className={`rounded-xl px-4 py-2 text-sm font-semibold transition-colors ${
                viewMode === "marks"
                  ? "bg-primary text-primary-foreground"
                  : "border bg-background hover:bg-muted"
              }`}
            >
              Student Mark Entry
            </button>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="py-12 text-center text-sm text-muted-foreground">
          <RefreshCw className="mx-auto mb-2 animate-spin" size={18} />
          Loading assessment evaluations...
        </div>
      ) : viewMode === "averages" ? (
        <section className={card}>
          {/* Cumulative Course Performance Card */}
          <div className="mb-6 rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/5 via-background to-primary/10 p-5 shadow-sm">
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
              <div>
                <div className="flex items-center gap-2">
                  <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary uppercase">
                    Official Grading Formula
                  </span>
                  <span className="rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 dark:text-emerald-300">
                    Cumulative Course Metric
                  </span>
                </div>
                <h4 className="mt-2 text-xl font-bold tracking-tight">Class Cumulative Performance</h4>
                <p className="mt-1 text-xs text-muted-foreground max-w-xl">
                  Weighted course average across all graded evaluations (Theory: CAT 1 & 2 15% each, Quizzes 10% each, FAT 40%; Lab 25% embedded). Excludes missing evaluations.
                </p>
              </div>
              <div className="text-left sm:text-right sm:border-l sm:pl-6">
                <div className="text-3xl font-extrabold text-primary font-mono">
                  {cumulativeAvg?.calculatedAverage != null
                    ? `${cumulativeAvg.calculatedAverage.toFixed(1)}%`
                    : "No evaluations yet"}
                </div>
                <p className="mt-1 text-xs text-muted-foreground font-medium">
                  {cumulativeAvg?.gradedStudentsCount ?? 0} of {cumulativeAvg?.totalEnrolled ?? 0} students evaluated
                </p>
              </div>
            </div>
          </div>

          <div className="border-b pb-4">
            <h3 className="text-lg font-semibold">Individual Assessment Performance & Official Overrides</h3>
            <p className="mt-1 text-sm text-muted-foreground">
              Dynamic averages derived directly from graded student marks. Faculty can set official override averages per assessment.
            </p>
          </div>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full min-w-[850px] text-left text-sm">
              <thead className="border-b text-xs uppercase text-muted-foreground">
                <tr>
                  {["Assessment Name", "Type", "Scale", "Graded Status", "Calculated Average", "Official Class Average", "Action"].map(
                    (h) => (
                      <th className="p-3 font-semibold" key={h}>
                        {h}
                      </th>
                    )
                  )}
                </tr>
              </thead>
              <tbody>
                {assessments.map((a) => {
                  const fb = feedback[a.id]
                  const total = a.enrolledCount ?? a.totalStudents ?? 15
                  return (
                    <tr className="border-b last:border-0 hover:bg-muted/30" key={a.id}>
                      <td className="p-3 font-medium">{a.name}</td>
                      <td className="p-3">
                        <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-semibold">
                          {a.type}
                        </span>
                      </td>
                      <td className="p-3 font-mono">/{a.rawMaxMarks}</td>
                      <td className="p-3">
                        <span className="font-medium text-foreground">
                          {a.submissionsCount ?? 0}/{total} graded
                        </span>
                      </td>
                      <td className="p-3">
                        {a.calculatedAverage != null ? (
                          <span className="font-semibold text-emerald-600 dark:text-emerald-400">
                            {a.calculatedAverage} / {a.rawMaxMarks}
                          </span>
                        ) : (
                          <span className="italic text-muted-foreground">
                            Not available
                          </span>
                        )}
                      </td>
                      <td className="p-3">
                        <div className="flex items-center gap-2">
                          <input
                            type="number"
                            step="0.1"
                            min="0"
                            max={a.rawMaxMarks}
                            value={avgInputs[a.id] ?? ""}
                            onChange={(e) =>
                              setAvgInputs((prev) => ({ ...prev, [a.id]: e.target.value }))
                            }
                            placeholder={
                              a.officialClassAverage != null
                                ? String(a.officialClassAverage)
                                : "Unentered"
                            }
                            className={`${input} h-8 w-24`}
                          />
                          <span className="text-xs text-muted-foreground">/{a.rawMaxMarks}</span>
                        </div>
                        {fb && (
                          <p
                            className={`mt-1 text-xs ${
                              fb.ok ? "text-emerald-600 dark:text-emerald-400" : "text-destructive"
                            }`}
                          >
                            {fb.msg}
                          </p>
                        )}
                      </td>
                      <td className="p-3">
                        <Button
                          variant="outline"
                          disabled={savingAvgId === a.id}
                          onClick={() => handleSaveOfficialAverage(a)}
                        >
                          <Save size={14} />
                          {savingAvgId === a.id ? "Saving…" : "Save"}
                        </Button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </section>
      ) : (
        <section className={card}>
          <div className="flex flex-col gap-4 border-b pb-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h3 className="text-lg font-semibold">Student Mark Entry Grid</h3>
              <p className="mt-1 text-sm text-muted-foreground">
                Enter and persist raw assessment marks for enrolled students in this class section.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <label className="text-sm font-semibold">Assessment:</label>
              <select
                value={selectedAssessmentId}
                onChange={(e) => setSelectedAssessmentId(e.target.value)}
                className={`${input} min-w-[200px] font-medium`}
              >
                {assessments.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name} (Max {a.rawMaxMarks})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {selectedAssessment && (
            <div className="mt-4 flex flex-wrap items-center justify-between gap-4 rounded-xl bg-muted/30 p-4">
              <div className="flex flex-wrap gap-6 text-sm">
                <p>
                  <span className="text-muted-foreground">Assessment: </span>
                  <span className="font-semibold">{selectedAssessment.name}</span>
                </p>
                <p>
                  <span className="text-muted-foreground">Scale: </span>
                  <span className="font-semibold">0 to {selectedAssessment.rawMaxMarks}</span>
                </p>
                <p>
                  <span className="text-muted-foreground">Graded: </span>
                  <span className="font-semibold">
                    {selectedAssessment.submissionsCount ?? 0}/{selectedAssessment.enrolledCount ?? selectedAssessment.totalStudents ?? marksData?.students?.length ?? 15} graded
                  </span>
                </p>
                <p>
                  <span className="text-muted-foreground">Calculated Average: </span>
                  <span className="font-semibold">
                    {selectedAssessment.calculatedAverage != null
                      ? `${selectedAssessment.calculatedAverage} / ${selectedAssessment.rawMaxMarks}`
                      : "Not available"}
                  </span>
                </p>
                <p>
                  <span className="text-muted-foreground">Official Average: </span>
                  <span className="font-semibold">
                    {selectedAssessment.officialClassAverage != null
                      ? `${selectedAssessment.officialClassAverage} / ${selectedAssessment.rawMaxMarks}`
                      : "Unentered"}
                  </span>
                </p>
              </div>

              <Button onClick={handleSaveMarks} disabled={savingMarks}>
                <Save size={15} /> {savingMarks ? "Saving Marks…" : "Save Class Marks"}
              </Button>
            </div>
          )}

          {marksFeedback && (
            <div
              className={`mt-4 rounded-xl p-3 text-sm ${
                marksFeedback.includes("success")
                  ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
                  : "bg-destructive/10 text-destructive"
              }`}
            >
              {marksFeedback}
            </div>
          )}

          <div className="mt-5 overflow-x-auto">
            <table className="w-full min-w-[700px] text-left text-sm">
              <thead className="border-b text-xs uppercase text-muted-foreground">
                <tr>
                  {["Registration No.", "Student Name", "Course Performance", "Marks Obtained", "Status"].map((h) => (
                    <th className="p-3 font-semibold" key={h}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {marksData?.students?.map((s) => {
                  const val = marksInputs[s.studentId] ?? ""
                  const maxMarks = selectedAssessment?.rawMaxMarks ?? 100
                  const isGraded = val.trim() !== ""
                  return (
                    <tr className="border-b last:border-0 hover:bg-muted/30" key={s.studentId}>
                      <td className="p-3 font-mono font-semibold text-primary">
                        {s.studentNumber}
                      </td>
                      <td className="p-3 font-medium">{s.fullName}</td>
                      <td className="p-3">
                        {s.cumulativeScore != null ? (
                          <span className="font-semibold text-emerald-600 dark:text-emerald-400 font-mono text-xs">
                            {s.cumulativeScore.toFixed(1)}%
                          </span>
                        ) : (
                          <span className="text-xs text-muted-foreground italic">No evaluations</span>
                        )}
                        <span className="block text-[10px] text-muted-foreground">
                          {s.completedAssessmentsCount ?? 0} graded
                        </span>
                      </td>
                      <td className="p-3">
                        <div className="flex items-center gap-2">
                          <input
                            type="number"
                            step="0.1"
                            min="0"
                            max={maxMarks}
                            value={val}
                            onChange={(e) =>
                              setMarksInputs((prev) => ({
                                ...prev,
                                [s.studentId]: e.target.value,
                              }))
                            }
                            placeholder="Not graded"
                            className={`${input} h-9 w-28`}
                          />
                          <span className="text-xs text-muted-foreground">/ {maxMarks}</span>
                        </div>
                      </td>
                      <td className="p-3">
                        <span
                          className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                            isGraded
                              ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
                              : "bg-muted text-muted-foreground"
                          }`}
                        >
                          {isGraded ? "Graded" : "Pending"}
                        </span>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  )
}

function TeacherSettings({
  theme,
  setTheme,
  logout,
}: {
  theme: string
  setTheme: (theme: string) => void
  logout: () => void
}) {
  return (
    <div className="flex flex-col gap-6">
      <section className={card}>
        <h3 className="font-semibold">Appearance Theme</h3>
        <p className="mt-1 text-sm text-muted-foreground">
          Choose a theme for the faculty interface.
        </p>
        <div className="mt-4 flex gap-2">
          {["light", "dark"].map((option) => (
            <button
              key={option}
              onClick={() => setTheme(option)}
              className={`rounded-xl border px-4 py-2 text-sm capitalize font-medium transition-colors ${
                theme === option
                  ? "border-primary bg-primary/10 text-primary"
                  : "hover:bg-muted"
              }`}
            >
              {option}
            </button>
          ))}
        </div>
      </section>

      <section className={card}>
        <h3 className="font-semibold">Faculty Account Information</h3>
        <div className="mt-4 grid gap-4 sm:grid-cols-3">
          <label className="text-sm font-medium">
            Role
            <div className="mt-2 flex h-10 items-center rounded-xl border bg-muted/40 px-3 font-semibold text-primary">
              TEACHER
            </div>
          </label>
        </div>
      </section>

      <section className={card}>
        <h3 className="font-semibold">Account Actions</h3>
        <p className="mt-1 text-sm text-muted-foreground">
          End your faculty session and clear stored tokens.
        </p>
        <div className="mt-4">
          <Button
            variant="danger"
            onClick={() => {
              if (window.confirm("Sign out of the faculty portal?")) logout()
            }}
          >
            Sign out of Faculty Portal
          </Button>
        </div>
      </section>
    </div>
  )
}
