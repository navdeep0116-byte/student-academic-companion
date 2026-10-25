import type { AssessmentRecord, CourseRecord } from "./demo-data"

/**
 * Official Student Academic Companion marking scheme:
 * - CAT 1: raw marks out of 50, weighted contribution 15/100 to the theory score.
 * - CAT 2: raw marks out of 50, weighted contribution 15/100 to the theory score.
 * - Quiz 1, Quiz 2, Quiz 3: each out of 10, each contributing 10/100 to the theory score.
 * - FAT: raw marks out of 100, contributing 40/100 to the theory score.
 * - Theory score = (CAT1/50)*15 + (CAT2/50)*15 + Quiz1 + Quiz2 + Quiz3 + (FAT/100)*40.
 * - Lab: 10 assessments, each out of 10. Calculate lab score out of 100 from the 10 assessments.
 * - Embedded overall score = theory score * 0.75 + lab score * 0.25.
 * - Missing assessments are never treated as zero.
 * - Final overall score is not computed until all required components are available.
 */
export const THEORY_WEIGHTS = {
  cat1: 15,
  cat2: 15,
  quiz1: 10,
  quiz2: 10,
  quiz3: 10,
  fat: 40,
} as const

export const EMBEDDED_RATIO = {
  theory: 0.75,
  lab: 0.25,
} as const

export type PredictionStage = "CAT 1 only" | "CAT 1, CAT 2 and quizzes" | "Pre-FAT estimate" | "Actual final score"

export interface RecordedScoreResult {
  isComplete: boolean
  score: number | null
  text: string
  sub: string
  earned: number
  completedWeight: number
  totalWeight: number
  theoryScore: number | null
  labScore: number | null
  completedAssessmentsCount: number
  totalAssessmentsCount: number
}

export function completedAssessments(course: CourseRecord) {
  return course.assessmentRecords.filter((record) => record.status === "COMPLETED" && typeof record.obtainedMarks === "number")
}

export function calculateTheoryScore(records: AssessmentRecord[]) {
  const norm = (s?: string) => (s || "").toUpperCase().replace(/\s+/g, "")
  const findRecord = (label: string) =>
    records.find((r) => norm(r.label) === norm(label) || norm(r.label).includes(norm(label)))

  const cat1 = findRecord("CAT 1")
  const cat2 = findRecord("CAT 2")
  const quizzes = records.filter((r) => r.type === "QUIZ" || norm(r.label).includes("QUIZ"))
  const quiz1 = quizzes.find((q) => norm(q.label).includes("1")) || quizzes[0]
  const quiz2 = quizzes.find((q) => norm(q.label).includes("2")) || (quizzes.length > 1 ? quizzes[1] : undefined)
  const quiz3 = quizzes.find((q) => norm(q.label).includes("3")) || (quizzes.length > 2 ? quizzes[2] : undefined)
  const fat = findRecord("FAT")

  const getRaw = (r?: AssessmentRecord, targetMax = 10): number | null => {
    if (!r || r.status !== "COMPLETED" || typeof r.obtainedMarks !== "number") return null
    const max = r.maxMarks && r.maxMarks > 0 ? r.maxMarks : targetMax
    return (r.obtainedMarks / max) * targetMax
  }

  const cat1Val = getRaw(cat1, 50)
  const cat2Val = getRaw(cat2, 50)
  const q1Val = getRaw(quiz1, 10)
  const q2Val = getRaw(quiz2, 10)
  const q3Val = getRaw(quiz3, 10)
  const fatVal = getRaw(fat, 100)

  let earned = 0
  let completedWeight = 0

  if (cat1Val !== null) {
    earned += (cat1Val / 50) * THEORY_WEIGHTS.cat1
    completedWeight += THEORY_WEIGHTS.cat1
  }
  if (cat2Val !== null) {
    earned += (cat2Val / 50) * THEORY_WEIGHTS.cat2
    completedWeight += THEORY_WEIGHTS.cat2
  }
  if (q1Val !== null) {
    earned += (q1Val / 10) * THEORY_WEIGHTS.quiz1
    completedWeight += THEORY_WEIGHTS.quiz1
  }
  if (q2Val !== null) {
    earned += (q2Val / 10) * THEORY_WEIGHTS.quiz2
    completedWeight += THEORY_WEIGHTS.quiz2
  }
  if (q3Val !== null) {
    earned += (q3Val / 10) * THEORY_WEIGHTS.quiz3
    completedWeight += THEORY_WEIGHTS.quiz3
  }
  if (fatVal !== null) {
    earned += (fatVal / 100) * THEORY_WEIGHTS.fat
    completedWeight += THEORY_WEIGHTS.fat
  }

  const isComplete = cat1Val !== null && cat2Val !== null && q1Val !== null && q2Val !== null && q3Val !== null && fatVal !== null
  const score = completedWeight > 0 ? (earned / completedWeight) * 100 : null

  return {
    isComplete,
    score,
    earned,
    completedWeight,
    hasFat: fatVal !== null,
    cat1: cat1Val,
    cat2: cat2Val,
    quiz1: q1Val,
    quiz2: q2Val,
    quiz3: q3Val,
    fat: fatVal,
  }
}

export function calculateLabScore(course: CourseRecord) {
  const labs = course.assessmentRecords.filter(
    (r) => (r.type === "LAB" || r.label.toUpperCase().includes("LAB")) && r.status === "COMPLETED" && typeof r.obtainedMarks === "number"
  )
  if (!labs.length) return { isComplete: false, score: null, completedCount: 0 }

  const totalRaw = labs.reduce((sum, r) => {
    const max = r.maxMarks && r.maxMarks > 0 ? r.maxMarks : 10
    const raw10 = ((r.obtainedMarks ?? 0) / max) * 10
    return sum + raw10
  }, 0)

  const isComplete = labs.length >= 10
  const score = (totalRaw / (labs.length * 10)) * 100

  return {
    isComplete,
    score,
    completedCount: labs.length,
    rawSum: totalRaw,
  }
}

export function getPrediction(course: CourseRecord) {
  const theory = calculateTheoryScore(course.assessmentRecords)
  const lab = calculateLabScore(course)
  const completed = completedAssessments(course)
  const pending = course.assessmentRecords.length - completed.length

  if (theory.score === null && lab.score === null) {
    return { score: null, grade: null, stage: null, completed: completed.length, pending, weakAreas: [] as string[] }
  }

  const isEmbedded = course.evaluationType === "EMBEDDED"
  const isLabOnly = course.evaluationType === "LAB"

  let score: number | null = null
  if (isLabOnly) {
    score = lab.score
  } else if (isEmbedded && lab.score !== null && theory.score !== null) {
    score = theory.score * EMBEDDED_RATIO.theory + lab.score * EMBEDDED_RATIO.lab
  } else {
    score = theory.score ?? lab.score
  }

  const allComplete = isLabOnly ? lab.isComplete : isEmbedded ? theory.isComplete && lab.isComplete : theory.isComplete
  const stage: PredictionStage = allComplete && theory.hasFat ? "Actual final score" : theory.completedWeight >= 40 ? "Pre-FAT estimate" : completed.some((r) => r.label === "CAT 2" || r.type === "QUIZ") ? "CAT 1, CAT 2 and quizzes" : "CAT 1 only"
  const grade = score === null ? null : score >= 90 ? "A+" : score >= 80 ? "A" : score >= 70 ? "B+" : score >= 60 ? "B" : score >= 50 ? "C" : "F"
  const weakAreas = completed
    .filter((r) => typeof r.obtainedMarks === "number" && r.obtainedMarks / r.maxMarks < 0.7)
    .map((r) => `${r.label} (${r.obtainedMarks}/${r.maxMarks})`)

  return { score, grade, stage, completed: completed.length, pending, weakAreas }
}
