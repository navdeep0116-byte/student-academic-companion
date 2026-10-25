// ============================================================
// STUDENT ACADEMIC COMPANION
// MongoDB Atlas - Initial Database Schema
// Backend: Java Spring Boot
// ============================================================

// Select the project database
const projectDb = db.getSiblingDB("student_academic_companion");

print("\nInitializing Student Academic Companion database...\n");

// ------------------------------------------------------------
// HELPER: Create a collection or update its validator.
// This script does not delete existing documents.
// ------------------------------------------------------------
function setupCollection(name, validator) {
  const existing = projectDb.getCollectionNames().includes(name);

  if (!existing) {
    projectDb.createCollection(name, {
      validator: validator,
      validationLevel: "strict",
      validationAction: "error"
    });
    print("CREATED: " + name);
  } else {
    projectDb.runCommand({
      collMod: name,
      validator: validator,
      validationLevel: "strict",
      validationAction: "error"
    });
    print("UPDATED VALIDATOR: " + name);
  }
}

// ------------------------------------------------------------
// 1. USERS
// Authentication, roles, account status.
// Store only password hashes, never plaintext passwords.
// ------------------------------------------------------------
setupCollection("users", {
  $jsonSchema: {
    bsonType: "object",
    required: ["email", "passwordHash", "role", "status", "createdAt"],
    properties: {
      email: {
        bsonType: "string",
        minLength: 3,
        maxLength: 254
      },
      passwordHash: {
        bsonType: "string",
        minLength: 20
      },
      role: {
        enum: ["STUDENT", "TEACHER", "ADMIN"]
      },
      status: {
        enum: ["PENDING", "ACTIVE", "SUSPENDED"]
      },
      createdAt: { bsonType: "date" },
      updatedAt: { bsonType: "date" },
      lastLoginAt: { bsonType: ["date", "null"] }
    }
  }
});

// ------------------------------------------------------------
// 2. STUDENTS
// One profile per student user.
// ------------------------------------------------------------
setupCollection("students", {
  $jsonSchema: {
    bsonType: "object",
    required: ["userId", "studentNumber", "fullName"],
    properties: {
      userId: { bsonType: "objectId" },
      studentNumber: { bsonType: "string", minLength: 1 },
      fullName: { bsonType: "string", minLength: 1 },
      program: { bsonType: "string" },
      department: { bsonType: "string" },
      yearOfStudy: { bsonType: "int", minimum: 1, maximum: 10 },
      section: { bsonType: "string" },
      createdAt: { bsonType: "date" }
    }
  }
});

// ------------------------------------------------------------
// 3. TEACHERS
// One profile per teacher user.
// ------------------------------------------------------------
setupCollection("teachers", {
  $jsonSchema: {
    bsonType: "object",
    required: ["userId", "employeeNumber", "fullName"],
    properties: {
      userId: { bsonType: "objectId" },
      employeeNumber: { bsonType: "string", minLength: 1 },
      fullName: { bsonType: "string", minLength: 1 },
      department: { bsonType: "string" },
      designation: { bsonType: "string" },
      createdAt: { bsonType: "date" }
    }
  }
});

// ------------------------------------------------------------
// 4. COURSES
// ------------------------------------------------------------
setupCollection("courses", {
  $jsonSchema: {
    bsonType: "object",
    required: ["code", "name", "credits"],
    properties: {
      code: { bsonType: "string", minLength: 1 },
      name: { bsonType: "string", minLength: 1 },
      description: { bsonType: "string" },
      credits: {
        bsonType: ["int", "double", "decimal"],
        minimum: 0
      },
      department: { bsonType: "string" },
      createdAt: { bsonType: "date" }
    }
  }
});

// ------------------------------------------------------------
// 5. SEMESTERS
// ------------------------------------------------------------
setupCollection("semesters", {
  $jsonSchema: {
    bsonType: "object",
    required: ["name", "startDate", "endDate"],
    properties: {
      name: { bsonType: "string", minLength: 1 },
      academicYear: { bsonType: "string" },
      startDate: { bsonType: "date" },
      endDate: { bsonType: "date" },
      status: { enum: ["UPCOMING", "ACTIVE", "COMPLETED"] }
    }
  }
});

// ------------------------------------------------------------
// 6. CLASSES
// Represents a class or section in a semester.
// ------------------------------------------------------------
setupCollection("classes", {
  $jsonSchema: {
    bsonType: "object",
    required: ["name", "semesterId"],
    properties: {
      name: { bsonType: "string", minLength: 1 },
      semesterId: { bsonType: "objectId" },
      program: { bsonType: "string" },
      yearOfStudy: { bsonType: "int", minimum: 1 },
      section: { bsonType: "string" },
      createdAt: { bsonType: "date" }
    }
  }
});

// ------------------------------------------------------------
// 7. TEACHING ASSIGNMENTS
// Which teacher can manage which class/course/semester.
// ------------------------------------------------------------
setupCollection("teaching_assignments", {
  $jsonSchema: {
    bsonType: "object",
    required: ["teacherId", "classId", "courseId", "semesterId"],
    properties: {
      teacherId: { bsonType: "objectId" },
      classId: { bsonType: "objectId" },
      courseId: { bsonType: "objectId" },
      semesterId: { bsonType: "objectId" },
      assignedAt: { bsonType: "date" },
      status: { enum: ["ACTIVE", "INACTIVE"] }
    }
  }
});

// ------------------------------------------------------------
// 8. ENROLLMENTS
// Connects a student to a class, course and semester.
// ------------------------------------------------------------
setupCollection("enrollments", {
  $jsonSchema: {
    bsonType: "object",
    required: [
      "studentId", "classId", "courseId", "semesterId", "status"
    ],
    properties: {
      studentId: { bsonType: "objectId" },
      classId: { bsonType: "objectId" },
      courseId: { bsonType: "objectId" },
      semesterId: { bsonType: "objectId" },
      status: { enum: ["ACTIVE", "COMPLETED", "WITHDRAWN"] },
      enrolledAt: { bsonType: "date" }
    }
  }
});

// ------------------------------------------------------------
// 9. ASSESSMENTS
// Quiz, assignment exam, CAT, FAT, etc.
// ------------------------------------------------------------
setupCollection("assessments", {
  $jsonSchema: {
    bsonType: "object",
    required: [
      "courseId", "classId", "semesterId",
      "title", "type", "maxMarks", "date"
    ],
    properties: {
      courseId: { bsonType: "objectId" },
      classId: { bsonType: "objectId" },
      semesterId: { bsonType: "objectId" },
      teacherId: { bsonType: "objectId" },
      title: { bsonType: "string", minLength: 1 },
      type: {
        enum: ["QUIZ", "ASSIGNMENT", "CAT", "FAT", "PROJECT", "OTHER"]
      },
      maxMarks: {
        bsonType: ["int", "double", "decimal"],
        minimum: 0
      },
      weightage: {
        bsonType: ["int", "double", "decimal"],
        minimum: 0,
        maximum: 100
      },
      date: { bsonType: "date" },
      createdAt: { bsonType: "date" }
    }
  }
});

// ------------------------------------------------------------
// 10. RESULTS
// Marks awarded for a student's enrollment and assessment.
// ------------------------------------------------------------
setupCollection("results", {
  $jsonSchema: {
    bsonType: "object",
    required: ["enrollmentId", "assessmentId", "marksObtained"],
    properties: {
      enrollmentId: { bsonType: "objectId" },
      assessmentId: { bsonType: "objectId" },
      marksObtained: {
        bsonType: ["int", "double", "decimal"],
        minimum: 0
      },
      feedback: { bsonType: "string" },
      gradedBy: { bsonType: "objectId" },
      gradedAt: { bsonType: "date" },
      updatedAt: { bsonType: "date" }
    }
  }
});

// ------------------------------------------------------------
// 11. ATTENDANCE
// One record per enrollment, date and session.
// ------------------------------------------------------------
setupCollection("attendance", {
  $jsonSchema: {
    bsonType: "object",
    required: ["enrollmentId", "date", "status"],
    properties: {
      enrollmentId: { bsonType: "objectId" },
      date: { bsonType: "date" },
      sessionKey: { bsonType: "string" },
      status: { enum: ["PRESENT", "ABSENT", "LATE", "EXCUSED"] },
      markedBy: { bsonType: "objectId" },
      remarks: { bsonType: "string" },
      createdAt: { bsonType: "date" }
    }
  }
});

// ------------------------------------------------------------
// 12. ASSIGNMENTS
// Academic tasks that students must submit.
// ------------------------------------------------------------
setupCollection("assignments", {
  $jsonSchema: {
    bsonType: "object",
    required: [
      "courseId", "classId", "semesterId", "title", "dueDate"
    ],
    properties: {
      courseId: { bsonType: "objectId" },
      classId: { bsonType: "objectId" },
      semesterId: { bsonType: "objectId" },
      teacherId: { bsonType: "objectId" },
      title: { bsonType: "string", minLength: 1 },
      description: { bsonType: "string" },
      dueDate: { bsonType: "date" },
      maxMarks: {
        bsonType: ["int", "double", "decimal"],
        minimum: 0
      },
      createdAt: { bsonType: "date" }
    }
  }
});

// ------------------------------------------------------------
// 13. SUBMISSIONS
// A student's submission for an assignment.
// ------------------------------------------------------------
setupCollection("submissions", {
  $jsonSchema: {
    bsonType: "object",
    required: [
      "assignmentId", "studentId", "submittedAt", "status"
    ],
    properties: {
      assignmentId: { bsonType: "objectId" },
      studentId: { bsonType: "objectId" },
      submittedAt: { bsonType: "date" },
      status: { enum: ["SUBMITTED", "LATE", "GRADED"] },
      fileUrl: { bsonType: "string" },
      comments: { bsonType: "string" },
      marksObtained: {
        bsonType: ["int", "double", "decimal"],
        minimum: 0
      },
      attemptNumber: { bsonType: "int", minimum: 1 }
    }
  }
});

// ------------------------------------------------------------
// 14. STUDY SESSIONS
// Tracks student study time.
// ------------------------------------------------------------
setupCollection("study_sessions", {
  $jsonSchema: {
    bsonType: "object",
    required: ["studentId", "startedAt", "durationMinutes"],
    properties: {
      studentId: { bsonType: "objectId" },
      courseId: { bsonType: "objectId" },
      startedAt: { bsonType: "date" },
      endedAt: { bsonType: "date" },
      durationMinutes: {
        bsonType: "int",
        minimum: 0,
        maximum: 1440
      },
      topic: { bsonType: "string" },
      notes: { bsonType: "string" }
    }
  }
});

// ------------------------------------------------------------
// 15. PREDICTIONS
// Output from the machine-learning service.
// ------------------------------------------------------------
setupCollection("predictions", {
  $jsonSchema: {
    bsonType: "object",
    required: ["studentId", "predictedAt", "modelVersion"],
    properties: {
      studentId: { bsonType: "objectId" },
      courseId: { bsonType: "objectId" },
      predictedAt: { bsonType: "date" },
      modelVersion: { bsonType: "string" },
      predictedScore: {
        bsonType: ["int", "double", "decimal"],
        minimum: 0,
        maximum: 100
      },
      riskLevel: { enum: ["LOW", "MEDIUM", "HIGH"] },
      inputSnapshot: { bsonType: "object" },
      confidence: {
        bsonType: ["int", "double", "decimal"],
        minimum: 0,
        maximum: 1
      }
    }
  }
});

// ------------------------------------------------------------
// 16. RECOMMENDATIONS
// Personalized academic guidance.
// ------------------------------------------------------------
setupCollection("recommendations", {
  $jsonSchema: {
    bsonType: "object",
    required: ["studentId", "title", "createdAt", "status"],
    properties: {
      studentId: { bsonType: "objectId" },
      courseId: { bsonType: "objectId" },
      predictionId: { bsonType: "objectId" },
      title: { bsonType: "string", minLength: 1 },
      description: { bsonType: "string" },
      category: {
        enum: ["STUDY_PLAN", "ATTENDANCE", "PERFORMANCE", "GENERAL"]
      },
      createdAt: { bsonType: "date" },
      status: { enum: ["NEW", "IN_PROGRESS", "COMPLETED"] }
    }
  }
});

// ============================================================
// INDEXES
// Enforce uniqueness and speed up common lookups.
// ============================================================

projectDb.users.createIndex(
  { email: 1 },
  { unique: true, name: "uq_users_email" }
);

projectDb.students.createIndex(
  { userId: 1 },
  { unique: true, name: "uq_students_user" }
);
projectDb.students.createIndex(
  { studentNumber: 1 },
  { unique: true, name: "uq_students_number" }
);

projectDb.teachers.createIndex(
  { userId: 1 },
  { unique: true, name: "uq_teachers_user" }
);
projectDb.teachers.createIndex(
  { employeeNumber: 1 },
  { unique: true, name: "uq_teachers_employee" }
);

projectDb.courses.createIndex(
  { code: 1 },
  { unique: true, name: "uq_courses_code" }
);
projectDb.semesters.createIndex(
  { name: 1, academicYear: 1 },
  { unique: true, name: "uq_semester_year" }
);

projectDb.classes.createIndex(
  { name: 1, semesterId: 1 },
  { unique: true, name: "uq_class_semester" }
);

projectDb.teaching_assignments.createIndex(
  { teacherId: 1, classId: 1, courseId: 1, semesterId: 1 },
  { unique: true, name: "uq_teaching_assignment" }
);

projectDb.enrollments.createIndex(
  { studentId: 1, classId: 1, courseId: 1, semesterId: 1 },
  { unique: true, name: "uq_enrollment" }
);
projectDb.enrollments.createIndex(
  { studentId: 1, status: 1 },
  { name: "ix_student_enrollments" }
);

projectDb.assessments.createIndex(
  { courseId: 1, classId: 1, semesterId: 1, date: 1 },
  { name: "ix_course_assessments" }
);

projectDb.results.createIndex(
  { enrollmentId: 1, assessmentId: 1 },
  { unique: true, name: "uq_result_enrollment_assessment" }
);

projectDb.attendance.createIndex(
  { enrollmentId: 1, date: 1, sessionKey: 1 },
  { unique: true, name: "uq_attendance_session" }
);

projectDb.assignments.createIndex(
  { courseId: 1, classId: 1, dueDate: 1 },
  { name: "ix_assignment_due_date" }
);

projectDb.submissions.createIndex(
  { assignmentId: 1, studentId: 1, attemptNumber: 1 },
  { unique: true, name: "uq_submission_attempt" }
);

projectDb.study_sessions.createIndex(
  { studentId: 1, startedAt: -1 },
  { name: "ix_student_study_history" }
);

projectDb.predictions.createIndex(
  { studentId: 1, predictedAt: -1 },
  { name: "ix_student_predictions" }
);

projectDb.recommendations.createIndex(
  { studentId: 1, status: 1, createdAt: -1 },
  { name: "ix_student_recommendations" }
);

// ============================================================
// VERIFICATION
// ============================================================

print("\nDatabase initialization complete.");
print("Database: " + projectDb.getName());
print("Collections:");

projectDb.getCollectionNames().sort().forEach(function (name) {
  print(" - " + name);
});

print("\nExpected main collections: 16");
print("Actual collections: " + projectDb.getCollectionNames().length);
print("\nIMPORTANT: Cross-collection references and permissions");
print("must also be enforced by the Spring Boot backend.");