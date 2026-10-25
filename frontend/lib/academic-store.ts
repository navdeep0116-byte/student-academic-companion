"use client"

import { useSyncExternalStore } from "react"
import { demoStudent } from "./demo-data"
import type { CourseRecord } from "./demo-data"

export type PreviewAssessment = { id: string; name: string; course: string; section: string; type: string; max: number; date: string; marks: Record<string, number> }
export type PreviewUser = { id: number; name: string; email: string; role: "Student" | "Teacher" | "Admin"; status: "Active" | "Pending" | "Inactive"; date: string }
export type PreviewCourse = CourseRecord & { department: string; status: "Active" | "Inactive"; section: string; teacher: string; enrolled: number; classes: number }
export type PreviewClass = { section: string; year: string; semester: string; department: string; students: number; courses: number; conducted: number; planned: number; advisor: string; status: "Active" | "Inactive" }
export type AcademicPreviewData = { version: 1; assessments: PreviewAssessment[]; users: PreviewUser[]; courses: PreviewCourse[]; classes: PreviewClass[] }

const storageKey = "sac-academic-preview-v1"
const initialData: AcademicPreviewData = {
  version: 1,
  assessments: [
    { id: "a1", name: "CAT 1", course: "CS301", section: "CSE-A", type: "CAT", max: 50, date: "2025-09-12", marks: { CSE202401: 42, CSE202402: 35 } },
    { id: "a2", name: "Quiz 1", course: "CS302", section: "CSE-B", type: "QUIZ", max: 10, date: "2025-09-18", marks: { CSE202403: 8 } },
  ],
  users: [
    { id: 1, name: "Aarav Mehta", email: "aarav.mehta@university.edu", role: "Student", status: "Active", date: "2025-08-14" },
    { id: 2, name: "Diya Shah", email: "diya.shah@university.edu", role: "Student", status: "Active", date: "2025-08-15" },
    { id: 3, name: "Dr. Priya Sharma", email: "priya.sharma@university.edu", role: "Teacher", status: "Active", date: "2025-07-22" },
    { id: 4, name: "Riya Kapoor", email: "riya.kapoor@university.edu", role: "Student", status: "Pending", date: "2025-10-18" },
    { id: 5, name: "Dr. N. Banerjee", email: "n.banerjee@university.edu", role: "Teacher", status: "Pending", date: "2025-10-17" },
    { id: 6, name: "Admin Preview", email: "admin@university.edu", role: "Admin", status: "Active", date: "2025-01-01" },
  ],
  courses: demoStudent.coursesList.map(course => ({ ...course, department: "Computer Science", status: "Active", section: "CSE-A", teacher: "Dr. Priya Sharma", enrolled: 42, classes: 2 })),
  classes: [
    { section: "CSE-B", year: "2025–26", semester: "Semester 5", department: "Computer Science", students: 42, courses: 4, conducted: 148, planned: 200, advisor: "Dr. Priya Sharma", status: "Active" },
    { section: "CSE-A", year: "2025–26", semester: "Semester 5", department: "Computer Science", students: 40, courses: 4, conducted: 154, planned: 200, advisor: "Dr. N. Banerjee", status: "Active" },
    { section: "MAT-A", year: "2025–26", semester: "Semester 5", department: "Mathematics", students: 36, courses: 3, conducted: 112, planned: 150, advisor: "Dr. Kavita Rao", status: "Active" },
  ],
}
let data = initialData
const listeners = new Set<() => void>()
let hydrated = false
function hydrate() { if (hydrated || typeof window === "undefined") return; hydrated = true; try { const parsed = JSON.parse(window.localStorage.getItem(storageKey) || "null") as AcademicPreviewData | null; if (parsed?.version === 1 && Array.isArray(parsed.assessments) && Array.isArray(parsed.users) && Array.isArray(parsed.courses) && Array.isArray(parsed.classes)) data = parsed } catch { data = initialData } }
function emit() { if (typeof window !== "undefined") window.localStorage.setItem(storageKey, JSON.stringify(data)); listeners.forEach(listener => listener()) }
export function useAcademicPreview() { return useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot) }
function subscribe(listener: () => void) { hydrate(); listeners.add(listener); return () => listeners.delete(listener) }
function getSnapshot() { hydrate(); return data }
function getServerSnapshot() { return initialData }
export function updateAcademicPreview(updater: (current: AcademicPreviewData) => AcademicPreviewData) { hydrate(); data = updater(data); emit() }
export function resetAcademicPreview() { data = initialData; emit() }
export function assessmentAverage(assessment: PreviewAssessment) { const marks = Object.values(assessment.marks).filter(mark => Number.isFinite(mark)); return marks.length ? marks.reduce((sum, mark) => sum + mark, 0) / marks.length : null }
