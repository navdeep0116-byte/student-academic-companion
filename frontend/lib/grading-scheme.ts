import type { LiveAssessment, LiveCourseMarks } from "./api-client"

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

export interface CourseScoreCalculation {
  isComplete: boolean
  hasRequiredAssessments: boolean
  scoreText: string
  scoreSub: string
  scoreValue: number | null
  earnedWeightedMarks: number
  completedWeight: number
  theoryScore: number | null
  labScore: number | null
  completedAssessmentsCount: number
  totalAssessmentsCount: number
  cat1Raw: number | null
  cat2Raw: number | null
  quiz1Raw: number | null
  quiz2Raw: number | null
  quiz3Raw: number | null
  completedLabsCount: number
  fatRaw: number | null
}

const norm = (s?: string) => (s || "").toUpperCase().replace(/\s+/g, "")

export function getRawMark(
  a: LiveAssessment | undefined,
  targetMax: number
): { raw: number | null; display: string } {
  if (!a) return { raw: null, display: "Not yet" }
  if (a.status !== "COMPLETED") return { raw: null, display: "Not yet" }

  // If backend provided pre-calculated rawMarks on official scale:
  if (typeof a.rawMarks === "number") {
    const rawVal = Math.round(a.rawMarks * 10) / 10
    const rawMax = a.rawMaxMarks || targetMax
    return { raw: rawVal, display: `${rawVal}/${rawMax}` }
  }

  // Fallback normalization:
  const obtained = a.obtainedMarks ?? a.marksObtained
  if (typeof obtained !== "number") return { raw: null, display: "Not yet" }

  const storedMax = a.maxMarks && a.maxMarks > 0 ? a.maxMarks : targetMax
  const normalized = Math.round(((obtained / storedMax) * targetMax) * 10) / 10
  return { raw: normalized, display: `${normalized}/${targetMax}` }
}

export function findAssessment(
  course: LiveCourseMarks,
  matchName: string,
  matchType?: string
): LiveAssessment | undefined {
  const target = norm(matchName)
  return course.assessments.find((a) => {
    const aName = norm(a.assessmentName)
    const aType = norm(a.assessmentType)
    if (aName === target || aName.includes(target)) return true
    if (matchType && aType === norm(matchType) && aName.includes(target)) return true
    return false
  })
}

export function calculateCourseRecordedScore(course: LiveCourseMarks): CourseScoreCalculation {
  const assessments = course.assessments || []
  const evalType = (course.evaluationType || "THEORY").toUpperCase()
  const isEmbedded = evalType === "EMBEDDED" || evalType === "THEORY_AND_LAB" || (evalType.includes("THEORY") && evalType.includes("LAB"))
  const isLabOnly = evalType === "LAB"

  // 1. Identify theory assessments
  const cat1A = findAssessment(course, "CAT 1", "CAT")
  const cat2A = findAssessment(course, "CAT 2", "CAT")

  const quizzes = assessments.filter((a) => {
    const t = norm(a.assessmentType)
    const n = norm(a.assessmentName)
    return t === "QUIZ" || n.includes("QUIZ")
  })
  const quiz1A = quizzes.find((q) => norm(q.assessmentName).includes("1")) || quizzes[0]
  const quiz2A = quizzes.find((q) => norm(q.assessmentName).includes("2")) || (quizzes.length > 1 ? quizzes[1] : undefined)
  const quiz3A = quizzes.find((q) => norm(q.assessmentName).includes("3")) || (quizzes.length > 2 ? quizzes[2] : undefined)

  const fatA = findAssessment(course, "FAT", "FAT")

  // 2. Identify lab assessments
  const labs = assessments.filter((a) => {
    const t = norm(a.assessmentType)
    const n = norm(a.assessmentName)
    return t === "LAB" || (t === "OTHER" && n.includes("LAB")) || n.includes("LAB")
  })
  const completedLabs = labs.filter((l) => l.status === "COMPLETED" && typeof (l.obtainedMarks ?? l.marksObtained) === "number")

  // 3. Extract raw marks
  const cat1 = getRawMark(cat1A, 50)
  const cat2 = getRawMark(cat2A, 50)
  const q1 = getRawMark(quiz1A, 10)
  const q2 = getRawMark(quiz2A, 10)
  const q3 = getRawMark(quiz3A, 10)
  const fat = getRawMark(fatA, 100)

  // 4. Compute theory earned contribution and completed weight
  let earnedTheory = 0
  let completedTheoryWeight = 0

  if (cat1.raw !== null) {
    earnedTheory += (cat1.raw / 50) * THEORY_WEIGHTS.cat1
    completedTheoryWeight += THEORY_WEIGHTS.cat1
  }
  if (cat2.raw !== null) {
    earnedTheory += (cat2.raw / 50) * THEORY_WEIGHTS.cat2
    completedTheoryWeight += THEORY_WEIGHTS.cat2
  }
  if (q1.raw !== null) {
    earnedTheory += (q1.raw / 10) * THEORY_WEIGHTS.quiz1
    completedTheoryWeight += THEORY_WEIGHTS.quiz1
  }
  if (q2.raw !== null) {
    earnedTheory += (q2.raw / 10) * THEORY_WEIGHTS.quiz2
    completedTheoryWeight += THEORY_WEIGHTS.quiz2
  }
  if (q3.raw !== null) {
    earnedTheory += (q3.raw / 10) * THEORY_WEIGHTS.quiz3
    completedTheoryWeight += THEORY_WEIGHTS.quiz3
  }
  if (fat.raw !== null) {
    earnedTheory += (fat.raw / 100) * THEORY_WEIGHTS.fat
    completedTheoryWeight += THEORY_WEIGHTS.fat
  }

  // 5. Compute lab score
  let labScore: number | null = null
  if (completedLabs.length > 0) {
    const totalLabRaw = completedLabs.reduce((sum, l) => {
      const raw = getRawMark(l, 10).raw
      return sum + (raw ?? 0)
    }, 0)
    labScore = (totalLabRaw / (completedLabs.length * 10)) * 100
  }

  // 6. Check completeness
  const theoryComplete =
    cat1.raw !== null &&
    cat2.raw !== null &&
    q1.raw !== null &&
    q2.raw !== null &&
    q3.raw !== null &&
    fat.raw !== null

  const labsComplete = completedLabs.length >= 10
  const isComplete = isLabOnly ? labsComplete : isEmbedded ? theoryComplete && labsComplete : theoryComplete

  const completedCount = assessments.filter((a) => a.status === "COMPLETED" && typeof (a.obtainedMarks ?? a.marksObtained) === "number").length

  if (completedTheoryWeight === 0 && labScore === null) {
    return {
      isComplete: false,
      hasRequiredAssessments: false,
      scoreText: "Not enough data",
      scoreSub: "0 evaluations completed",
      scoreValue: null,
      earnedWeightedMarks: 0,
      completedWeight: 0,
      theoryScore: null,
      labScore: null,
      completedAssessmentsCount: 0,
      totalAssessmentsCount: assessments.length,
      cat1Raw: null,
      cat2Raw: null,
      quiz1Raw: null,
      quiz2Raw: null,
      quiz3Raw: null,
      completedLabsCount: 0,
      fatRaw: null,
    }
  }

  if (isComplete) {
    // True Final Overall Score (all required components available)
    let finalScore: number
    if (isLabOnly) {
      finalScore = labScore ?? 0
    } else if (isEmbedded && labScore !== null) {
      finalScore = earnedTheory * EMBEDDED_RATIO.theory + labScore * EMBEDDED_RATIO.lab
    } else {
      finalScore = earnedTheory
    }

    finalScore = Math.round(finalScore * 10) / 10

    return {
      isComplete: true,
      hasRequiredAssessments: true,
      scoreText: `Final: ${finalScore.toFixed(1)}%`,
      scoreSub: "Completed final score",
      scoreValue: finalScore,
      earnedWeightedMarks: Math.round(earnedTheory * 10) / 10,
      completedWeight: 100,
      theoryScore: Math.round(earnedTheory * 10) / 10,
      labScore: labScore !== null ? Math.round(labScore * 10) / 10 : null,
      completedAssessmentsCount: completedCount,
      totalAssessmentsCount: assessments.length,
      cat1Raw: cat1.raw,
      cat2Raw: cat2.raw,
      quiz1Raw: q1.raw,
      quiz2Raw: q2.raw,
      quiz3Raw: q3.raw,
      completedLabsCount: completedLabs.length,
      fatRaw: fat.raw,
    }
  }

  // Partial Recorded Score (DO NOT treat missing as zero)
  const partialTheoryScore = completedTheoryWeight > 0 ? (earnedTheory / completedTheoryWeight) * 100 : null
  let partialOverall: number
  if (isLabOnly) {
    partialOverall = labScore ?? 0
  } else if (isEmbedded && labScore !== null && partialTheoryScore !== null) {
    partialOverall = partialTheoryScore * EMBEDDED_RATIO.theory + labScore * EMBEDDED_RATIO.lab
  } else {
    partialOverall = partialTheoryScore ?? labScore ?? 0
  }

  partialOverall = Math.round(partialOverall * 10) / 10

  return {
    isComplete: false,
    hasRequiredAssessments: false,
    scoreText: `Partial: ${partialOverall.toFixed(1)}%`,
    scoreSub: `${earnedTheory.toFixed(1)} / ${completedTheoryWeight} weighted marks completed`,
    scoreValue: partialOverall,
    earnedWeightedMarks: Math.round(earnedTheory * 10) / 10,
    completedWeight: completedTheoryWeight,
    theoryScore: partialTheoryScore !== null ? Math.round(partialTheoryScore * 10) / 10 : null,
    labScore: labScore !== null ? Math.round(labScore * 10) / 10 : null,
    completedAssessmentsCount: completedCount,
    totalAssessmentsCount: assessments.length,
    cat1Raw: cat1.raw,
    cat2Raw: cat2.raw,
    quiz1Raw: q1.raw,
    quiz2Raw: q2.raw,
    quiz3Raw: q3.raw,
    completedLabsCount: completedLabs.length,
    fatRaw: fat.raw,
  }
}
