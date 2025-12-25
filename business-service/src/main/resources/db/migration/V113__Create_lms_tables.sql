-- ===================================
-- Learning Management System (LMS) Tables
-- Version: V113
-- Description: Course, Module, Lesson, Quiz, Enrollment, Progress tracking
-- ===================================

-- Courses
CREATE TABLE courses (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(500) NOT NULL,
    slug VARCHAR(500) NOT NULL,
    description TEXT,
    short_description VARCHAR(500),
    thumbnail_url VARCHAR(1000),
    preview_video_url VARCHAR(1000),
    learning_objectives TEXT,
    prerequisites TEXT,
    target_audience TEXT,
    course_level VARCHAR(50) NOT NULL DEFAULT 'BEGINNER',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    tier_required VARCHAR(50) DEFAULT 'FREE',
    price DECIMAL(12,2) DEFAULT 0,
    instructor_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    category_id UUID REFERENCES content_categories(id) ON DELETE SET NULL,
    duration_hours INTEGER,
    enrollment_count INTEGER DEFAULT 0,
    completion_count INTEGER DEFAULT 0,
    rating_average DECIMAL(3,2),
    rating_count INTEGER DEFAULT 0,
    is_featured BOOLEAN DEFAULT FALSE,
    published_at TIMESTAMP WITHOUT TIME ZONE,

    -- Audit fields
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by UUID REFERENCES users(id) ON DELETE SET NULL,

    -- Optimistic locking
    version BIGINT DEFAULT 0,

    CONSTRAINT uq_course_slug_tenant UNIQUE (tenant_id, slug, deleted),
    CONSTRAINT chk_course_level CHECK (course_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    CONSTRAINT chk_course_status CHECK (status IN ('DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED', 'DELETED')),
    CONSTRAINT chk_course_tier CHECK (tier_required IN ('FREE', 'SILVER', 'GOLD', 'VIP'))
);

-- Course Modules
CREATE TABLE course_modules (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    sort_order INTEGER NOT NULL,
    duration_minutes INTEGER,
    is_preview BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT uq_module_course_order UNIQUE (course_id, sort_order, deleted)
);

-- Lessons
CREATE TABLE lessons (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL REFERENCES course_modules(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    lesson_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    video_url VARCHAR(1000),
    video_duration_seconds INTEGER,
    attachment_id UUID REFERENCES attachments(id) ON DELETE SET NULL,
    sort_order INTEGER NOT NULL,
    duration_minutes INTEGER,
    is_preview BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT uq_lesson_module_order UNIQUE (module_id, sort_order, deleted),
    CONSTRAINT chk_lesson_type CHECK (lesson_type IN ('TEXT', 'VIDEO', 'QUIZ', 'ASSIGNMENT', 'LIVE_SESSION', 'DOCUMENT'))
);

-- Quizzes
CREATE TABLE quizzes (
    id UUID PRIMARY KEY,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    passing_score INTEGER DEFAULT 70,
    time_limit_minutes INTEGER,
    max_attempts INTEGER,
    is_randomized BOOLEAN DEFAULT FALSE,
    show_correct_answers BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT chk_passing_score CHECK (passing_score >= 0 AND passing_score <= 100)
);

-- Quiz Questions
CREATE TABLE quiz_questions (
    id UUID PRIMARY KEY,
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    options TEXT, -- JSON array for multiple choice
    correct_answer TEXT NOT NULL,
    explanation TEXT,
    points INTEGER DEFAULT 1,
    sort_order INTEGER NOT NULL,

    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT chk_question_type CHECK (question_type IN ('MULTIPLE_CHOICE', 'TRUE_FALSE', 'SHORT_ANSWER', 'ESSAY'))
);

-- Course Enrollments
CREATE TABLE enrollments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    certificate_issued_at TIMESTAMP WITHOUT TIME ZONE,
    progress_percentage INTEGER DEFAULT 0,
    last_accessed_at TIMESTAMP WITHOUT TIME ZONE,
    price_paid DECIMAL(12,2) DEFAULT 0,
    notes TEXT,

    -- Audit fields
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by UUID REFERENCES users(id) ON DELETE SET NULL,

    CONSTRAINT uq_enrollment_user_course UNIQUE (course_id, user_id),
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'DROPPED', 'EXPIRED', 'SUSPENDED')),
    CONSTRAINT chk_progress_percentage CHECK (progress_percentage >= 0 AND progress_percentage <= 100)
);

-- Lesson Progress
CREATE TABLE lesson_progress (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    time_spent_seconds INTEGER DEFAULT 0,
    last_position_seconds INTEGER DEFAULT 0,
    completion_percentage INTEGER DEFAULT 0,

    CONSTRAINT uq_progress_enrollment_lesson UNIQUE (enrollment_id, lesson_id),
    CONSTRAINT chk_lesson_progress_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT chk_completion_percentage CHECK (completion_percentage >= 0 AND completion_percentage <= 100)
);

-- Quiz Attempts
CREATE TABLE quiz_attempts (
    id UUID PRIMARY KEY,
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    attempt_number INTEGER NOT NULL,
    score INTEGER,
    total_points INTEGER,
    percentage DECIMAL(5,2),
    passed BOOLEAN,
    started_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP WITHOUT TIME ZONE,
    time_spent_seconds INTEGER,
    answers TEXT, -- JSON array of answers

    CONSTRAINT uq_quiz_attempt UNIQUE (quiz_id, user_id, attempt_number)
);

-- Course Reviews
CREATE TABLE course_reviews (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL,
    review_title VARCHAR(200),
    review_text TEXT,
    is_verified_purchase BOOLEAN DEFAULT FALSE,
    helpful_count INTEGER DEFAULT 0,

    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT uq_review_user_course UNIQUE (course_id, user_id, deleted),
    CONSTRAINT chk_rating CHECK (rating >= 1 AND rating <= 5)
);

-- Achievements
CREATE TABLE achievements (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    icon VARCHAR(200),
    achievement_type VARCHAR(50) NOT NULL,
    criteria TEXT, -- JSON rules
    points INTEGER DEFAULT 0,
    badge_image_url VARCHAR(1000),

    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT chk_achievement_type CHECK (achievement_type IN ('COURSE_COMPLETION', 'QUIZ_MASTER', 'STREAK', 'SPEED_LEARNER', 'PERFECT_SCORE', 'CUSTOM'))
);

-- User Achievements
CREATE TABLE user_achievements (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id UUID NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    earned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    progress_percentage INTEGER DEFAULT 100,
    metadata TEXT, -- JSON for additional data

    CONSTRAINT uq_user_achievement UNIQUE (user_id, achievement_id)
);

-- Course Certificates
CREATE TABLE certificates (
    id UUID PRIMARY KEY,
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    certificate_number VARCHAR(100) UNIQUE NOT NULL,
    issued_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP WITHOUT TIME ZONE,
    certificate_url VARCHAR(1000),
    verification_code VARCHAR(50) UNIQUE,

    CONSTRAINT uq_certificate_enrollment UNIQUE (enrollment_id)
);

-- ===================================
-- Indexes for Performance
-- ===================================

-- Courses
CREATE INDEX idx_courses_tenant ON courses(tenant_id);
CREATE INDEX idx_courses_slug ON courses(slug);
CREATE INDEX idx_courses_status ON courses(status);
CREATE INDEX idx_courses_instructor ON courses(instructor_id);
CREATE INDEX idx_courses_category ON courses(category_id);
CREATE INDEX idx_courses_tier ON courses(tier_required);
CREATE INDEX idx_courses_featured ON courses(is_featured);
CREATE INDEX idx_courses_published_at ON courses(published_at DESC);

-- Course Modules
CREATE INDEX idx_modules_course ON course_modules(course_id);
CREATE INDEX idx_modules_sort ON course_modules(course_id, sort_order);

-- Lessons
CREATE INDEX idx_lessons_module ON lessons(module_id);
CREATE INDEX idx_lessons_type ON lessons(lesson_type);
CREATE INDEX idx_lessons_sort ON lessons(module_id, sort_order);

-- Quizzes
CREATE INDEX idx_quizzes_lesson ON quizzes(lesson_id);

-- Quiz Questions
CREATE INDEX idx_questions_quiz ON quiz_questions(quiz_id);
CREATE INDEX idx_questions_sort ON quiz_questions(quiz_id, sort_order);

-- Enrollments
CREATE INDEX idx_enrollments_course ON enrollments(course_id);
CREATE INDEX idx_enrollments_user ON enrollments(user_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);
CREATE INDEX idx_enrollments_date ON enrollments(enrolled_at DESC);

-- Lesson Progress
CREATE INDEX idx_progress_enrollment ON lesson_progress(enrollment_id);
CREATE INDEX idx_progress_lesson ON lesson_progress(lesson_id);
CREATE INDEX idx_progress_status ON lesson_progress(status);

-- Quiz Attempts
CREATE INDEX idx_attempts_quiz ON quiz_attempts(quiz_id);
CREATE INDEX idx_attempts_user ON quiz_attempts(user_id);
CREATE INDEX idx_attempts_enrollment ON quiz_attempts(enrollment_id);

-- Course Reviews
CREATE INDEX idx_reviews_course ON course_reviews(course_id);
CREATE INDEX idx_reviews_user ON course_reviews(user_id);
CREATE INDEX idx_reviews_rating ON course_reviews(rating);

-- Achievements
CREATE INDEX idx_achievements_tenant ON achievements(tenant_id);
CREATE INDEX idx_achievements_type ON achievements(achievement_type);

-- User Achievements
CREATE INDEX idx_user_achievements_user ON user_achievements(user_id);
CREATE INDEX idx_user_achievements_achievement ON user_achievements(achievement_id);
CREATE INDEX idx_user_achievements_date ON user_achievements(earned_at DESC);

-- Certificates
CREATE INDEX idx_certificates_user ON certificates(user_id);
CREATE INDEX idx_certificates_course ON certificates(course_id);
CREATE INDEX idx_certificates_number ON certificates(certificate_number);
CREATE INDEX idx_certificates_verification ON certificates(verification_code);

-- Note: Table comments removed for H2 compatibility
-- For PostgreSQL production, consider adding COMMENT ON TABLE statements
