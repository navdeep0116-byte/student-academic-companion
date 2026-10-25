'use client'

import { useEffect, useMemo, useState } from "react"
import {
  BookOpen,
  CheckCircle2,
  Edit3,
  GraduationCap,
  Plus,
  RefreshCw,
  Search,
  Settings2,
  ShieldCheck,
  UserCheck,
  UserPlus,
  Users,
  X,
  XCircle,
} from "lucide-react"
import {
  academicApi,
  type AdminClass,
  type AdminCourse,
  type AdminOverviewStats,
  type AdminUser,
  type AvailableTeacher,
  type EligibleStudent,
} from "@/lib/api-client"

type AdminPage = "Overview" | "User Management" | "Courses & Classes" | "Settings"
type UserRole = "ALL" | "STUDENT" | "TEACHER" | "ADMIN"
type UserStatus = "ALL" | "ACTIVE" | "INACTIVE" | "PENDING"

const card = "rounded-2xl border bg-card p-5 shadow-sm"
const input = "h-10 rounded-xl border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"

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

function Status({ value }: { value: string }) {
  const norm = (value || "").toUpperCase()
  return (
    <span
      className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
        norm === "ACTIVE"
          ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
          : norm === "PENDING"
          ? "bg-amber-500/10 text-amber-700 dark:text-amber-300"
          : "bg-muted text-muted-foreground"
      }`}
    >
      {norm}
    </span>
  )
}

function Heading({
  title,
  description,
  children,
}: {
  title: string
  description: string
  children?: React.ReactNode
}) {
  return (
    <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
      <div>
        <span className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
          <ShieldCheck size={14} /> UNIVERSITY ADMINISTRATION
        </span>
        <h2 className="mt-3 text-3xl font-semibold tracking-tight">{title}</h2>
        <p className="mt-2 text-muted-foreground">{description}</p>
      </div>
      {children}
    </div>
  )
}

function Overview({ go }: { go: (page: AdminPage) => void }) {
  const [stats, setStats] = useState<AdminOverviewStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadStats = () => {
    setLoading(true)
    academicApi
      .adminOverview()
      .then((data) => {
        setStats(data)
        setLoading(false)
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Failed to load overview statistics")
        setLoading(false)
      })
  }

  useEffect(() => {
    loadStats()
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <RefreshCw className="mr-2 animate-spin" size={20} /> Loading administrator overview...
      </div>
    )
  }

  if (error || !stats) {
    return (
      <div className="rounded-2xl border border-destructive/20 bg-destructive/10 p-6 text-destructive">
        <p className="font-semibold">Failed to connect to administrative service</p>
        <p className="mt-1 text-sm">{error || "Please ensure your admin credentials are valid."}</p>
        <Button variant="outline" onClick={loadStats} className="mt-4">
          Retry Connection
        </Button>
      </div>
    )
  }

  const statItems: Array<[React.ElementType, string, string | number, string]> = [
    [Users, "Total students", stats.totalStudents, `${stats.activeStudents} active students`],
    [ShieldCheck, "Faculty members", stats.totalTeachers, `${stats.activeTeachers} active teachers`],
    [BookOpen, "Active courses", stats.totalCourses, "Standard CSE & Mathematics catalog"],
    [Settings2, "Class sections", stats.totalClasses, `${stats.totalClasses} sections across 4 courses`],
    [GraduationCap, "Total enrollments", stats.totalEnrollments, "Student-course registrations"],
    [UserCheck, "System status", "Connected", "MongoDB Atlas synchronized"],
  ]

  return (
    <div className="flex flex-col gap-7">
      <Heading
        title="Admin dashboard"
        description="Comprehensive overview of university programs, faculty, student registrations, and course offerings."
      >
        <div className="flex gap-2">
          <Button variant="outline" onClick={loadStats}>
            <RefreshCw size={15} /> Refresh
          </Button>
          <Button onClick={() => go("User Management")}>
            <UserPlus size={16} /> Manage users
          </Button>
        </div>
      </Heading>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
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

      <div className="grid gap-5 xl:grid-cols-[1.35fr_1fr]">
        <section className={card}>
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold">Recent academic activity</h3>
              <p className="mt-1 text-sm text-muted-foreground">Live university database state.</p>
            </div>
            <Button variant="outline" onClick={() => go("User Management")}>
              View all users
            </Button>
          </div>
          <div className="mt-4 flex flex-col gap-3">
            {stats.recentActivity.map((item, idx) => (
              <div className="flex items-center gap-3 rounded-xl bg-muted/40 p-3" key={idx}>
                <CheckCircle2 className="text-primary shrink-0" size={18} />
                <div>
                  <p className="text-sm font-medium">{item.title}</p>
                  <p className="text-xs text-muted-foreground">{item.detail}</p>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className={card}>
          <h3 className="font-semibold">Quick administrative actions</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Execute verified operations directly against MongoDB.
          </p>
          <div className="mt-4 grid gap-2">
            <Button onClick={() => go("User Management")}>
              <UserPlus size={16} /> Add a user
            </Button>
            <Button variant="outline" onClick={() => go("Courses & Classes")}>
              <BookOpen size={16} /> View course catalog
            </Button>
            <Button variant="outline" onClick={() => go("Courses & Classes")}>
              <Settings2 size={16} /> Manage class sections
            </Button>
          </div>
        </section>
      </div>
    </div>
  )
}

function UserManagement() {
  const [users, setUsers] = useState<AdminUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [role, setRole] = useState<UserRole>("ALL")
  const [status, setStatus] = useState<UserStatus>("ALL")
  const [formOpen, setFormOpen] = useState(false)
  const [formLoading, setFormLoading] = useState(false)
  const [feedback, setFeedback] = useState<string | null>(null)

  const [cgpaModalUser, setCgpaModalUser] = useState<AdminUser | null>(null)
  const [cgpaInput, setCgpaInput] = useState<string>("")
  const [cgpaSaving, setCgpaSaving] = useState<boolean>(false)
  const [cgpaError, setCgpaError] = useState<string | null>(null)
  const [cgpaSuccess, setCgpaSuccess] = useState<string | null>(null)

  const openCgpaModal = (user: AdminUser) => {
    setCgpaModalUser(user)
    setCgpaInput(user.previousCgpa != null ? String(user.previousCgpa) : "")
    setCgpaError(null)
    setCgpaSuccess(null)
  }

  const handleSaveCgpa = async (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    if (!cgpaModalUser) return
    setCgpaError(null)
    setCgpaSuccess(null)

    const valStr = cgpaInput.trim()
    let targetValue: number | null = null

    if (valStr !== "") {
      const parsed = Number(valStr)
      if (isNaN(parsed) || !/^\d+(\.\d+)?$/.test(valStr)) {
        setCgpaError("Previous CGPA must be a valid numeric value between 0.00 and 10.00")
        return
      }
      if (parsed < 0 || parsed > 10) {
        setCgpaError("Previous CGPA must be between 0.00 and 10.00")
        return
      }
      const parts = valStr.split(".")
      if (parts.length > 1 && parts[1].length > 2) {
        setCgpaError("Previous CGPA cannot have more than two decimal places")
        return
      }
      targetValue = parsed
    }

    setCgpaSaving(true)
    try {
      const targetId = cgpaModalUser.studentId || cgpaModalUser.studentNumber || cgpaModalUser.id
      const res = await academicApi.updateStudentCgpa(targetId, targetValue)
      setCgpaSuccess(res.message || "CGPA updated successfully")
      setUsers(prev => prev.map(u => {
        if (u.id === cgpaModalUser.id) {
          return {
            ...u,
            previousCgpa: res.previousCgpa,
            previousCgpaUpdatedAt: res.previousCgpaUpdatedAt,
            previousCgpaUpdatedBy: res.previousCgpaUpdatedBy,
          }
        }
        return u
      }))
      setTimeout(() => {
        setCgpaModalUser(null)
      }, 700)
    } catch (err) {
      setCgpaError(err instanceof Error ? err.message : "Failed to update CGPA")
    } finally {
      setCgpaSaving(false)
    }
  }

  const handleClearCgpa = async () => {
    if (!cgpaModalUser) return
    setCgpaInput("")
    setCgpaError(null)
    setCgpaSuccess(null)
    setCgpaSaving(true)
    try {
      const targetId = cgpaModalUser.studentId || cgpaModalUser.studentNumber || cgpaModalUser.id
      const res = await academicApi.updateStudentCgpa(targetId, null)
      setCgpaSuccess(res.message || "CGPA cleared")
      setUsers(prev => prev.map(u => {
        if (u.id === cgpaModalUser.id) {
          return {
            ...u,
            previousCgpa: null,
            previousCgpaUpdatedAt: res.previousCgpaUpdatedAt,
            previousCgpaUpdatedBy: res.previousCgpaUpdatedBy,
          }
        }
        return u
      }))
      setTimeout(() => {
        setCgpaModalUser(null)
      }, 700)
    } catch (err) {
      setCgpaError(err instanceof Error ? err.message : "Failed to clear CGPA")
    } finally {
      setCgpaSaving(false)
    }
  }

  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    role: "STUDENT",
    studentNumber: "",
    employeeNumber: "",
    department: "Computer Science",
    section: "A",
  })

  const loadUsers = () => {
    setLoading(true)
    setError(null)
    academicApi
      .adminUsers()
      .then((res) => {
        setUsers(res.users || [])
        setLoading(false)
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Failed to load users")
        setLoading(false)
      })
  }

  useEffect(() => {
    loadUsers()
  }, [])

  const filtered = useMemo(() => {
    return users.filter((u) => {
      const q = query.toLowerCase()
      const matchQuery =
        !query ||
        u.name.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        (u.studentNumber && u.studentNumber.toLowerCase().includes(q)) ||
        (u.employeeNumber && u.employeeNumber.toLowerCase().includes(q))
      const matchRole = role === "ALL" || (u.role || "").toUpperCase() === role
      const matchStatus = status === "ALL" || (u.status || "").toUpperCase() === status
      return matchQuery && matchRole && matchStatus
    })
  }, [users, query, role, status])

  const handleToggleStatus = async (user: AdminUser) => {
    try {
      const next = (user.status || "").toUpperCase() === "ACTIVE" ? "INACTIVE" : "ACTIVE"
      const res = await academicApi.updateUserStatus(user.id, next)
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? { ...u, status: res.status } : u))
      )
    } catch (err) {
      alert(err instanceof Error ? err.message : "Failed to update user status")
    }
  }

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormLoading(true)
    setFeedback(null)
    try {
      await academicApi.createAdminUser({
        name: form.name.trim(),
        email: form.email.trim(),
        role: form.role,
        password: form.password.trim() || undefined,
        studentNumber: form.studentNumber.trim() || undefined,
        employeeNumber: form.employeeNumber.trim() || undefined,
        department: form.department.trim() || undefined,
        section: form.section.trim() || undefined,
      })
      setFormOpen(false)
      setForm({
        name: "",
        email: "",
        password: "",
        role: "STUDENT",
        studentNumber: "",
        employeeNumber: "",
        department: "Computer Science",
        section: "A",
      })
      loadUsers()
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : "Failed to create user")
    } finally {
      setFormLoading(false)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <Heading
        title="User Management"
        description="Search, view, and manage university students, faculty, and administrative staff."
      >
        <div className="flex gap-2">
          <Button variant="outline" onClick={loadUsers}>
            <RefreshCw size={15} /> Refresh
          </Button>
          <Button onClick={() => setFormOpen(true)}>
            <Plus size={16} /> Add user
          </Button>
        </div>
      </Heading>

      <div className={card}>
        <div className="grid gap-3 md:grid-cols-[1fr_180px_180px]">
          <label className="relative">
            <Search className="absolute left-3 top-2.5 text-muted-foreground" size={17} />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search name, email, or registration number"
              className={`${input} w-full pl-9`}
            />
          </label>
          <select
            value={role}
            onChange={(e) => setRole(e.target.value as UserRole)}
            className={input}
          >
            <option value="ALL">All Roles</option>
            <option value="STUDENT">Students</option>
            <option value="TEACHER">Teachers</option>
            <option value="ADMIN">Admins</option>
          </select>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value as UserStatus)}
            className={input}
          >
            <option value="ALL">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="PENDING">Pending</option>
          </select>
        </div>

        {loading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            <RefreshCw className="mx-auto mb-2 animate-spin" size={18} />
            Loading accounts from MongoDB...
          </div>
        ) : error ? (
          <div className="p-6 text-center text-sm text-destructive">{error}</div>
        ) : (
          <div className="mt-5 overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="border-b text-xs uppercase text-muted-foreground">
                <tr>
                  {["Name", "Identifier", "Email", "Role", "Status", "Previous CGPA", "Registered", "Actions"].map(
                    (h) => (
                      <th className="p-3 font-semibold" key={h}>
                        {h}
                      </th>
                    )
                  )}
                </tr>
              </thead>
              <tbody>
                {filtered.map((user) => (
                  <tr className="border-b last:border-0 hover:bg-muted/30" key={user.id}>
                    <td className="p-3 font-medium">{user.name}</td>
                    <td className="p-3 text-muted-foreground">
                      {user.studentNumber || user.employeeNumber || "—"}
                    </td>
                    <td className="p-3">{user.email}</td>
                    <td className="p-3 font-medium">{user.role}</td>
                    <td className="p-3">
                      <Status value={user.status} />
                    </td>
                    <td className="p-3 font-medium">
                      {user.role === "STUDENT"
                        ? (user.previousCgpa != null ? Number(user.previousCgpa).toFixed(2) : "Not recorded")
                        : "—"}
                    </td>
                    <td className="p-3 text-muted-foreground">{user.date}</td>
                    <td className="p-3">
                      <div className="flex items-center gap-2">
                        {user.role === "STUDENT" && (
                          <Button
                            variant="outline"
                            onClick={() => openCgpaModal(user)}
                          >
                            Edit CGPA
                          </Button>
                        )}
                        {user.role !== "ADMIN" && (
                          <Button
                            variant="outline"
                            onClick={() => handleToggleStatus(user)}
                          >
                            {(user.status || "").toUpperCase() === "ACTIVE"
                              ? "Deactivate"
                              : "Activate"}
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filtered.length === 0 && (
              <p className="p-6 text-center text-sm text-muted-foreground">
                No university users match these search filters.
              </p>
            )}
          </div>
        )}
      </div>

      {formOpen && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <form
            onSubmit={handleCreateUser}
            className={`${card} w-full max-w-lg shadow-xl`}
          >
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-lg font-semibold">Create New User</h3>
              <button
                type="button"
                onClick={() => setFormOpen(false)}
                className="text-muted-foreground hover:text-foreground"
              >
                <X size={18} />
              </button>
            </div>

            <p className="mt-2 text-sm text-muted-foreground">
              User will be created in MongoDB Atlas with a securely hashed BCrypt password.
            </p>

            {feedback && (
              <p className="mt-3 rounded-xl bg-destructive/10 p-3 text-sm text-destructive">
                {feedback}
              </p>
            )}

            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <label className="text-sm font-medium sm:col-span-2">
                Full Name
                <input
                  required
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="e.g. Navdeep Shukla"
                  className={`${input} mt-1 w-full`}
                />
              </label>

              <label className="text-sm font-medium sm:col-span-2">
                University Email
                <input
                  required
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  placeholder="e.g. user@university.edu"
                  className={`${input} mt-1 w-full`}
                />
              </label>

              <label className="text-sm font-medium">
                Role
                <select
                  value={form.role}
                  onChange={(e) => setForm({ ...form, role: e.target.value })}
                  className={`${input} mt-1 w-full`}
                >
                  <option value="STUDENT">Student</option>
                  <option value="TEACHER">Teacher</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </label>

              <label className="text-sm font-medium">
                Password (optional, default provided)
                <input
                  type="password"
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  placeholder="Welcome@2026!"
                  className={`${input} mt-1 w-full`}
                />
              </label>

              {form.role === "STUDENT" && (
                <>
                  <label className="text-sm font-medium">
                    Registration Number (optional)
                    <input
                      value={form.studentNumber}
                      onChange={(e) => setForm({ ...form, studentNumber: e.target.value })}
                      placeholder="e.g. 25BCE2531"
                      className={`${input} mt-1 w-full`}
                    />
                  </label>
                  <label className="text-sm font-medium">
                    Section
                    <input
                      value={form.section}
                      onChange={(e) => setForm({ ...form, section: e.target.value })}
                      placeholder="A"
                      className={`${input} mt-1 w-full`}
                    />
                  </label>
                </>
              )}

              {form.role === "TEACHER" && (
                <>
                  <label className="text-sm font-medium">
                    Employee Number (optional)
                    <input
                      value={form.employeeNumber}
                      onChange={(e) => setForm({ ...form, employeeNumber: e.target.value })}
                      placeholder="e.g. EMP1009"
                      className={`${input} mt-1 w-full`}
                    />
                  </label>
                  <label className="text-sm font-medium">
                    Department
                    <input
                      value={form.department}
                      onChange={(e) => setForm({ ...form, department: e.target.value })}
                      placeholder="Computer Science"
                      className={`${input} mt-1 w-full`}
                    />
                  </label>
                </>
              )}
            </div>

            <div className="mt-6 flex justify-end gap-2 border-t pt-4">
              <Button variant="outline" onClick={() => setFormOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={formLoading}>
                {formLoading ? "Creating…" : "Save User"}
              </Button>
            </div>
          </form>
        </div>
      )}

      {cgpaModalUser && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <form
            onSubmit={handleSaveCgpa}
            className={`${card} w-full max-w-md shadow-xl`}
          >
            <div className="flex items-center justify-between border-b pb-3">
              <div>
                <h3 className="text-lg font-semibold">Update Student Previous CGPA</h3>
                <p className="text-xs text-muted-foreground">
                  {cgpaModalUser.name} ({cgpaModalUser.studentNumber || "Student"})
                </p>
              </div>
              <button
                type="button"
                onClick={() => setCgpaModalUser(null)}
                className="text-muted-foreground hover:text-foreground"
              >
                <X size={18} />
              </button>
            </div>

            <div className="mt-4 flex flex-col gap-3">
              <div className="rounded-xl bg-muted/40 p-3 text-xs text-muted-foreground">
                <p><b>Current Record:</b> {cgpaModalUser.previousCgpa != null ? `${Number(cgpaModalUser.previousCgpa).toFixed(2)} / 10` : "Not recorded"}</p>
                {cgpaModalUser.previousCgpaUpdatedAt && (
                  <p className="mt-1">Last updated: {cgpaModalUser.previousCgpaUpdatedAt.substring(0, 10)} by {cgpaModalUser.previousCgpaUpdatedBy || "Admin"}</p>
                )}
              </div>

              <label className="text-sm font-medium">
                Previous CGPA (0.00 – 10.00)
                <input
                  type="text"
                  value={cgpaInput}
                  onChange={(e) => setCgpaInput(e.target.value)}
                  placeholder="e.g. 8.45 (or leave empty to clear)"
                  className={`${input} mt-1 w-full`}
                  autoFocus
                />
                <span className="mt-1 block text-xs text-muted-foreground">
                  Allowed range: 0.00 to 10.00, maximum 2 decimal places. Leave empty to clear.
                </span>
              </label>

              {cgpaError && (
                <p className="rounded-xl bg-destructive/10 p-3 text-xs text-destructive">
                  {cgpaError}
                </p>
              )}

              {cgpaSuccess && (
                <p className="rounded-xl bg-emerald-500/10 p-3 text-xs text-emerald-700 dark:text-emerald-300">
                  {cgpaSuccess}
                </p>
              )}

              <div className="mt-2 flex items-center justify-between gap-2 border-t pt-3">
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleClearCgpa}
                  disabled={cgpaSaving || cgpaModalUser.previousCgpa == null}
                >
                  Clear (Not recorded)
                </Button>

                <div className="flex items-center gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setCgpaModalUser(null)}
                    disabled={cgpaSaving}
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    disabled={cgpaSaving}
                  >
                    {cgpaSaving ? "Saving..." : "Save CGPA"}
                  </Button>
                </div>
              </div>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}

function CoursesClasses() {
  const [tab, setTab] = useState<"Courses" | "Classes">("Courses")
  const [courses, setCourses] = useState<AdminCourse[]>([])
  const [classes, setClasses] = useState<AdminClass[]>([])
  const [teachers, setTeachers] = useState<AvailableTeacher[]>([])
  const [students, setStudents] = useState<EligibleStudent[]>([])
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState("")

  // Create Course Modal
  const [courseModal, setCourseModal] = useState(false)
  const [courseForm, setCourseForm] = useState({
    code: "",
    name: "",
    credits: 4,
    department: "Computer Science",
    description: "",
  })

  // Create Class Modal
  const [classModal, setClassModal] = useState(false)
  const [classForm, setClassForm] = useState({
    courseCode: "",
    section: "A",
    teacherId: "",
  })

  // Enroll Student Modal
  const [enrollModal, setEnrollModal] = useState(false)
  const [enrollForm, setEnrollForm] = useState({
    studentId: "",
    classId: "",
  })

  const [actionLoading, setActionLoading] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  const loadData = () => {
    setLoading(true)
    Promise.all([
      academicApi.adminCourses(),
      academicApi.adminClasses(),
      academicApi.availableTeachers(),
      academicApi.eligibleStudents(),
    ])
      .then(([cRes, clRes, tRes, sRes]) => {
        setCourses(cRes.courses || [])
        setClasses(clRes.classes || [])
        setTeachers(tRes.teachers || [])
        setStudents(sRes.students || [])
        if (cRes.courses && cRes.courses.length > 0 && !classForm.courseCode) {
          setClassForm((prev) => ({ ...prev, courseCode: cRes.courses[0].code }))
        }
        if (clRes.classes && clRes.classes.length > 0 && !enrollForm.classId) {
          setEnrollForm((prev) => ({ ...prev, classId: clRes.classes[0].classId }))
        }
        if (sRes.students && sRes.students.length > 0 && !enrollForm.studentId) {
          setEnrollForm((prev) => ({ ...prev, studentId: sRes.students[0].studentId }))
        }
        setLoading(false)
      })
      .catch((err) => {
        setActionError(err instanceof Error ? err.message : "Failed to load academic data")
        setLoading(false)
      })
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleCreateCourse = async (e: React.FormEvent) => {
    e.preventDefault()
    setActionLoading(true)
    setActionError(null)
    try {
      await academicApi.createCourse(courseForm)
      setCourseModal(false)
      setCourseForm({
        code: "",
        name: "",
        credits: 4,
        department: "Computer Science",
        description: "",
      })
      loadData()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Failed to create course")
    } finally {
      setActionLoading(false)
    }
  }

  const handleCreateClass = async (e: React.FormEvent) => {
    e.preventDefault()
    setActionLoading(true)
    setActionError(null)
    try {
      await academicApi.createClass({
        courseCode: classForm.courseCode,
        section: classForm.section,
        teacherId: classForm.teacherId || undefined,
      })
      setClassModal(false)
      loadData()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Failed to create class section")
    } finally {
      setActionLoading(false)
    }
  }

  const handleEnrollStudent = async (e: React.FormEvent) => {
    e.preventDefault()
    setActionLoading(true)
    setActionError(null)
    try {
      await academicApi.enrollStudent({
        studentId: enrollForm.studentId,
        classId: enrollForm.classId,
      })
      setEnrollModal(false)
      loadData()
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Failed to enroll student")
    } finally {
      setActionLoading(false)
    }
  }

  const filteredCourses = courses.filter((c) =>
    `${c.code} ${c.name} ${c.department}`.toLowerCase().includes(query.toLowerCase())
  )

  const filteredClasses = classes.filter((cl) =>
    `${cl.courseCode} ${cl.courseName} ${cl.section} ${cl.advisor}`
      .toLowerCase()
      .includes(query.toLowerCase())
  )

  return (
    <div className="flex flex-col gap-6">
      <Heading
        title="Courses & Classes"
        description="Manage the university course catalog, teaching assignments, and student enrollments."
      >
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={loadData}>
            <RefreshCw size={15} /> Refresh
          </Button>
          <Button variant="outline" onClick={() => setEnrollModal(true)}>
            <UserPlus size={16} /> Enroll Student
          </Button>
          <Button
            onClick={() => (tab === "Courses" ? setCourseModal(true) : setClassModal(true))}
          >
            <Plus size={16} /> {tab === "Courses" ? "Add course" : "Create class"}
          </Button>
        </div>
      </Heading>

      <div className={card}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex gap-2">
            {(["Courses", "Classes"] as const).map((item) => (
              <button
                key={item}
                onClick={() => {
                  setTab(item)
                  setQuery("")
                }}
                className={`rounded-xl px-4 py-2 text-sm font-semibold transition-colors ${
                  tab === item
                    ? "bg-primary text-primary-foreground"
                    : "border bg-background hover:bg-muted"
                }`}
              >
                {item}
              </button>
            ))}
          </div>

          <label className="relative">
            <Search className="absolute left-3 top-2.5 text-muted-foreground" size={17} />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={`Search ${tab.toLowerCase()}...`}
              className={`${input} pl-9`}
            />
          </label>
        </div>

        {loading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            <RefreshCw className="mx-auto mb-2 animate-spin" size={18} />
            Loading catalog from MongoDB...
          </div>
        ) : tab === "Courses" ? (
          <div className="mt-5 overflow-x-auto">
            <table className="w-full min-w-[950px] text-left text-sm">
              <thead className="border-b text-xs uppercase text-muted-foreground">
                <tr>
                  {["Code", "Course Name", "Credits", "Department", "Sections", "Enrolled", "Faculty Assigned", "Status"].map(
                    (h) => (
                      <th className="p-3 font-semibold" key={h}>
                        {h}
                      </th>
                    )
                  )}
                </tr>
              </thead>
              <tbody>
                {filteredCourses.map((c) => (
                  <tr className="border-b last:border-0 hover:bg-muted/30" key={c.code}>
                    <td className="p-3 font-semibold text-primary">{c.code}</td>
                    <td className="p-3 font-medium">{c.name}</td>
                    <td className="p-3">{c.credits}</td>
                    <td className="p-3">{c.department}</td>
                    <td className="p-3">{c.classes} sections</td>
                    <td className="p-3">{c.enrolled} students</td>
                    <td className="p-3">{c.teacher}</td>
                    <td className="p-3">
                      <Status value={c.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="mt-5 overflow-x-auto">
            <table className="w-full min-w-[950px] text-left text-sm">
              <thead className="border-b text-xs uppercase text-muted-foreground">
                <tr>
                  {["Class Section", "Course Code", "Course Name", "Semester", "Enrolled", "Assigned Teacher", "Status"].map(
                    (h) => (
                      <th className="p-3 font-semibold" key={h}>
                        {h}
                      </th>
                    )
                  )}
                </tr>
              </thead>
              <tbody>
                {filteredClasses.map((cl) => (
                  <tr className="border-b last:border-0 hover:bg-muted/30" key={cl.classId}>
                    <td className="p-3 font-semibold">Section {cl.section}</td>
                    <td className="p-3 font-medium text-primary">{cl.courseCode}</td>
                    <td className="p-3">{cl.courseName}</td>
                    <td className="p-3 text-muted-foreground">{cl.semester}</td>
                    <td className="p-3 font-medium">{cl.students} students</td>
                    <td className="p-3">{cl.advisor}</td>
                    <td className="p-3">
                      <Status value={cl.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add Course Modal */}
      {courseModal && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <form
            onSubmit={handleCreateCourse}
            className={`${card} w-full max-w-md shadow-xl`}
          >
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-lg font-semibold">Add New Course</h3>
              <button
                type="button"
                onClick={() => setCourseModal(false)}
                className="text-muted-foreground hover:text-foreground"
              >
                <X size={18} />
              </button>
            </div>

            {actionError && (
              <p className="mt-3 rounded-xl bg-destructive/10 p-3 text-sm text-destructive">
                {actionError}
              </p>
            )}

            <div className="mt-4 grid gap-4">
              <label className="text-sm font-medium">
                Course Code
                <input
                  required
                  value={courseForm.code}
                  onChange={(e) => setCourseForm({ ...courseForm, code: e.target.value })}
                  placeholder="e.g. BACSE401"
                  className={`${input} mt-1 w-full`}
                />
              </label>
              <label className="text-sm font-medium">
                Course Name
                <input
                  required
                  value={courseForm.name}
                  onChange={(e) => setCourseForm({ ...courseForm, name: e.target.value })}
                  placeholder="e.g. Advanced Algorithms"
                  className={`${input} mt-1 w-full`}
                />
              </label>
              <label className="text-sm font-medium">
                Department
                <input
                  required
                  value={courseForm.department}
                  onChange={(e) => setCourseForm({ ...courseForm, department: e.target.value })}
                  placeholder="Computer Science"
                  className={`${input} mt-1 w-full`}
                />
              </label>
              <label className="text-sm font-medium">
                Credits
                <input
                  type="number"
                  min="1"
                  max="6"
                  required
                  value={courseForm.credits}
                  onChange={(e) =>
                    setCourseForm({ ...courseForm, credits: Number(e.target.value) })
                  }
                  className={`${input} mt-1 w-full`}
                />
              </label>
            </div>

            <div className="mt-6 flex justify-end gap-2 border-t pt-4">
              <Button variant="outline" onClick={() => setCourseModal(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={actionLoading}>
                {actionLoading ? "Saving…" : "Save Course"}
              </Button>
            </div>
          </form>
        </div>
      )}

      {/* Create Class Section Modal */}
      {classModal && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <form
            onSubmit={handleCreateClass}
            className={`${card} w-full max-w-md shadow-xl`}
          >
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-lg font-semibold">Create Class Section</h3>
              <button
                type="button"
                onClick={() => setClassModal(false)}
                className="text-muted-foreground hover:text-foreground"
              >
                <X size={18} />
              </button>
            </div>

            {actionError && (
              <p className="mt-3 rounded-xl bg-destructive/10 p-3 text-sm text-destructive">
                {actionError}
              </p>
            )}

            <div className="mt-4 grid gap-4">
              <label className="text-sm font-medium">
                Select Course
                <select
                  value={classForm.courseCode}
                  onChange={(e) => setClassForm({ ...classForm, courseCode: e.target.value })}
                  className={`${input} mt-1 w-full`}
                >
                  {courses.map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.code} - {c.name}
                    </option>
                  ))}
                </select>
              </label>

              <label className="text-sm font-medium">
                Section Identifier
                <input
                  required
                  value={classForm.section}
                  onChange={(e) => setClassForm({ ...classForm, section: e.target.value })}
                  placeholder="e.g. C or Section C"
                  className={`${input} mt-1 w-full`}
                />
              </label>

              <label className="text-sm font-medium">
                Assign Faculty Instructor
                <select
                  value={classForm.teacherId}
                  onChange={(e) => setClassForm({ ...classForm, teacherId: e.target.value })}
                  className={`${input} mt-1 w-full`}
                >
                  <option value="">Unassigned</option>
                  {teachers.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name} ({t.employeeNumber}) {t.isAvailable ? "— Available" : `(${t.assignedClassesCount} assigned)`}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className="mt-6 flex justify-end gap-2 border-t pt-4">
              <Button variant="outline" onClick={() => setClassModal(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={actionLoading}>
                {actionLoading ? "Creating…" : "Create Class"}
              </Button>
            </div>
          </form>
        </div>
      )}

      {/* Enroll Student Modal */}
      {enrollModal && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <form
            onSubmit={handleEnrollStudent}
            className={`${card} w-full max-w-md shadow-xl`}
          >
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-lg font-semibold">Enroll Student in Class</h3>
              <button
                type="button"
                onClick={() => setEnrollModal(false)}
                className="text-muted-foreground hover:text-foreground"
              >
                <X size={18} />
              </button>
            </div>

            {actionError && (
              <p className="mt-3 rounded-xl bg-destructive/10 p-3 text-sm text-destructive">
                {actionError}
              </p>
            )}

            <div className="mt-4 grid gap-4">
              <label className="text-sm font-medium">
                Select Student
                <select
                  value={enrollForm.studentId}
                  onChange={(e) => setEnrollForm({ ...enrollForm, studentId: e.target.value })}
                  className={`${input} mt-1 w-full`}
                >
                  {students.map((s) => (
                    <option key={s.studentId} value={s.studentId}>
                      {s.studentNumber} - {s.fullName}
                    </option>
                  ))}
                </select>
              </label>

              <label className="text-sm font-medium">
                Select Class Offering
                <select
                  value={enrollForm.classId}
                  onChange={(e) => setEnrollForm({ ...enrollForm, classId: e.target.value })}
                  className={`${input} mt-1 w-full`}
                >
                  {classes.map((cl) => (
                    <option key={cl.classId} value={cl.classId}>
                      {cl.courseCode} ({cl.name})
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className="mt-6 flex justify-end gap-2 border-t pt-4">
              <Button variant="outline" onClick={() => setEnrollModal(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={actionLoading}>
                {actionLoading ? "Enrolling…" : "Enroll Student"}
              </Button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}

function AdminSettings({
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
      <Heading
        title="Settings"
        description="System appearance, verified credentials, and administrative session controls."
      />

      <section className={card}>
        <h3 className="font-semibold">Appearance Theme</h3>
        <p className="mt-1 text-sm text-muted-foreground">
          Choose a theme for the administrator interface. Preference is persisted.
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
        <h3 className="font-semibold">Account Information</h3>
        <div className="mt-4 grid gap-4 sm:grid-cols-3">
          <label className="text-sm font-medium">
            Administrator Name
            <input
              readOnly
              value="System Administrator"
              className={`${input} mt-2 w-full opacity-80`}
            />
          </label>
          <label className="text-sm font-medium">
            Email
            <input
              readOnly
              value="admin@university.edu"
              className={`${input} mt-2 w-full opacity-80`}
            />
          </label>
          <div className="text-sm font-medium">
            Role
            <div className="mt-2 flex h-10 items-center rounded-xl border bg-muted/40 px-3 font-semibold text-primary">
              ADMIN
            </div>
          </div>
        </div>
      </section>

      <section className={card}>
        <h3 className="font-semibold">Session Management</h3>
        <p className="mt-1 text-sm text-muted-foreground">
          End your administrator session and clear authentication tokens.
        </p>
        <div className="mt-4">
          <Button
            variant="danger"
            onClick={() => {
              if (window.confirm("Sign out of the administrator portal?")) logout()
            }}
          >
            Sign out of Admin Portal
          </Button>
        </div>
      </section>
    </div>
  )
}

export function AdminDashboard({
  page,
  go,
  theme,
  setTheme,
  logout,
}: {
  page: AdminPage
  go: (page: AdminPage) => void
  theme: string
  setTheme: (theme: string) => void
  logout: () => void
}) {
  if (page === "User Management") return <UserManagement />
  if (page === "Courses & Classes") return <CoursesClasses />
  if (page === "Settings") return <AdminSettings theme={theme} setTheme={setTheme} logout={logout} />
  return <Overview go={go} />
}
