// =====================================================
// LMS (Learning Management System) Types
// =====================================================

// Enums
export enum CourseLevel {
  BEGINNER = 'BEGINNER',
  INTERMEDIATE = 'INTERMEDIATE',
  ADVANCED = 'ADVANCED',
  EXPERT = 'EXPERT',
}

export enum CourseStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED',
}

export enum EnrollmentStatus {
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED',
}

export enum LessonType {
  VIDEO = 'VIDEO',
  TEXT = 'TEXT',
  QUIZ = 'QUIZ',
  ASSIGNMENT = 'ASSIGNMENT',
  RESOURCE = 'RESOURCE',
}

export enum QuizQuestionType {
  MULTIPLE_CHOICE = 'MULTIPLE_CHOICE',
  TRUE_FALSE = 'TRUE_FALSE',
  SHORT_ANSWER = 'SHORT_ANSWER',
  ESSAY = 'ESSAY',
}

// =====================================================
// Entity Types
// =====================================================

export interface Course {
  id: string
  title: string
  slug: string
  description?: string
  summary?: string
  thumbnailUrl?: string
  level: CourseLevel
  status: CourseStatus
  price: number
  currency: string
  duration: number // in minutes
  language: string
  instructorId: string
  instructorName?: string
  categoryId?: string
  categoryName?: string
  tags?: string[]
  whatYouWillLearn?: string[]
  prerequisites?: string[]
  targetAudience?: string[]
  enrollmentCount: number
  rating?: number
  reviewCount: number
  certificateEnabled: boolean
  createdAt: string
  updatedAt: string
  publishedAt?: string
}

export interface CourseModule {
  id: string
  courseId: string
  title: string
  description?: string
  displayOrder: number
  duration: number // in minutes
  lessonCount: number
  isLocked: boolean
  unlockAfterModuleId?: string
  createdAt: string
  updatedAt: string
}

export interface CourseLesson {
  id: string
  moduleId: string
  courseid: string
  title: string
  description?: string
  type: LessonType
  content?: string // markdown or HTML content
  videoUrl?: string
  videoDuration?: number // in seconds
  resourceUrls?: string[]
  displayOrder: number
  duration: number // in minutes
  isPreview: boolean // can be viewed without enrollment
  isMandatory: boolean
  quizId?: string
  createdAt: string
  updatedAt: string
}

export interface CourseEnrollment {
  id: string
  courseId: string
  courseName?: string
  userId: string
  userName?: string
  userEmail?: string
  status: EnrollmentStatus
  progress: number // percentage 0-100
  completedLessons: number
  totalLessons: number
  lastAccessedAt?: string
  completedAt?: string
  certificateIssued: boolean
  certificateUrl?: string
  enrolledAt: string
  expiresAt?: string
}

export interface Quiz {
  id: string
  lessonId: string
  title: string
  description?: string
  passingScore: number // percentage
  timeLimit?: number // in minutes
  maxAttempts?: number
  shuffleQuestions: boolean
  showResults: boolean
  createdAt: string
  updatedAt: string
}

export interface QuizQuestion {
  id: string
  quizId: string
  type: QuizQuestionType
  question: string
  options?: string[] // for multiple choice
  correctAnswer: string
  explanation?: string
  points: number
  displayOrder: number
}

export interface QuizAttempt {
  id: string
  quizId: string
  enrollmentId: string
  userId: string
  score: number
  passed: boolean
  answers: Record<string, string> // questionId -> answer
  startedAt: string
  submittedAt?: string
  timeSpent: number // in seconds
}

export interface Certificate {
  id: string
  enrollmentId: string
  courseId: string
  courseName: string
  userId: string
  userName: string
  issueDate: string
  certificateUrl: string
  verificationCode: string
}

// =====================================================
// Request DTOs
// =====================================================

export interface CreateCourseRequest {
  title: string
  slug?: string
  description?: string
  summary?: string
  thumbnailUrl?: string
  level: CourseLevel
  status: CourseStatus
  price: number
  currency?: string
  duration?: number
  language?: string
  categoryId?: string
  tags?: string[]
  whatYouWillLearn?: string[]
  prerequisites?: string[]
  targetAudience?: string[]
  certificateEnabled?: boolean
}

export interface UpdateCourseRequest {
  title?: string
  slug?: string
  description?: string
  summary?: string
  thumbnailUrl?: string
  level?: CourseLevel
  status?: CourseStatus
  price?: number
  currency?: string
  duration?: number
  language?: string
  categoryId?: string
  tags?: string[]
  whatYouWillLearn?: string[]
  prerequisites?: string[]
  targetAudience?: string[]
  certificateEnabled?: boolean
}

export interface CreateModuleRequest {
  courseId: string
  title: string
  description?: string
  displayOrder: number
  isLocked?: boolean
  unlockAfterModuleId?: string
}

export interface UpdateModuleRequest {
  title?: string
  description?: string
  displayOrder?: number
  isLocked?: boolean
  unlockAfterModuleId?: string
}

export interface CreateLessonRequest {
  moduleId: string
  courseId: string
  title: string
  description?: string
  type: LessonType
  content?: string
  videoUrl?: string
  videoDuration?: number
  resourceUrls?: string[]
  displayOrder: number
  duration?: number
  isPreview?: boolean
  isMandatory?: boolean
}

export interface UpdateLessonRequest {
  title?: string
  description?: string
  type?: LessonType
  content?: string
  videoUrl?: string
  videoDuration?: number
  resourceUrls?: string[]
  displayOrder?: number
  duration?: number
  isPreview?: boolean
  isMandatory?: boolean
}

export interface CreateEnrollmentRequest {
  courseId: string
  userId: string
}

export interface UpdateEnrollmentRequest {
  status?: EnrollmentStatus
  progress?: number
  completedLessons?: number
  lastAccessedAt?: string
}

export interface CreateQuizRequest {
  lessonId: string
  title: string
  description?: string
  passingScore: number
  timeLimit?: number
  maxAttempts?: number
  shuffleQuestions?: boolean
  showResults?: boolean
}

export interface CreateQuizQuestionRequest {
  quizId: string
  type: QuizQuestionType
  question: string
  options?: string[]
  correctAnswer: string
  explanation?: string
  points: number
  displayOrder: number
}

export interface SubmitQuizAttemptRequest {
  quizId: string
  enrollmentId: string
  answers: Record<string, string>
  timeSpent: number
}

// =====================================================
// Response DTOs
// =====================================================

export interface CourseStatsResponse {
  totalCourses: number
  publishedCourses: number
  draftCourses: number
  totalEnrollments: number
  activeEnrollments: number
  completedEnrollments: number
  totalRevenue: number
  averageRating: number
  byLevel: Record<CourseLevel, number>
  byStatus: Record<CourseStatus, number>
}

export interface EnrollmentStatsResponse {
  totalEnrollments: number
  activeEnrollments: number
  completedEnrollments: number
  averageProgress: number
  completionRate: number
  byStatus: Record<EnrollmentStatus, number>
}

export interface CourseProgressResponse {
  enrollmentId: string
  courseId: string
  progress: number
  completedModules: number
  totalModules: number
  completedLessons: number
  totalLessons: number
  lastAccessedLesson?: string
  timeSpent: number // in minutes
  quizzesPassed: number
  quizzesTotal: number
  certificateEligible: boolean
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

// =====================================================
// Filter Types
// =====================================================

export interface CourseFilters {
  level?: CourseLevel
  status?: CourseStatus
  categoryId?: string
  instructorId?: string
  minPrice?: number
  maxPrice?: number
  language?: string
  keyword?: string
  tags?: string[]
}

export interface EnrollmentFilters {
  status?: EnrollmentStatus
  courseId?: string
  userId?: string
  minProgress?: number
  maxProgress?: number
}
