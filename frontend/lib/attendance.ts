export const MIN_ATTENDANCE_PERCENTAGE = 75

export function attendancePercentage(attended: number, conducted: number) {
  if (!Number.isFinite(attended) || !Number.isFinite(conducted) || conducted <= 0 || attended < 0 || attended > conducted) return null
  return Math.min(100, attended / conducted * 100)
}

export function attendanceStatus(attended: number, conducted: number) {
  const percentage = attendancePercentage(attended, conducted)
  return percentage === null ? "Unavailable" : percentage < MIN_ATTENDANCE_PERCENTAGE ? "Below 75%" : "On track"
}

export function belowThresholdCourseCount(courses: Array<{ attended: number; conducted: number }>) {
  return courses.filter(course => attendancePercentage(course.attended, course.conducted) !== null && attendancePercentage(course.attended, course.conducted)! < MIN_ATTENDANCE_PERCENTAGE).length
}
