# 📚 Phase 2: LMS Implementation Plan

## 🎯 Overview

Phase 2 implements a complete Learning Management System (LMS) with:
- Course/Module/Lesson hierarchy
- Quiz & Assessment system
- Enrollment & Progress tracking
- Achievements & Certificates
- Integration with Phase 1 (CMS) & Phase 3 (Membership)

---

## 📊 Implementation Status

| Component | Files | Status |
|-----------|-------|--------|
| Database Migration | 1 | ✅ Complete |
| Enums | 6 | ✅ Complete |
| Domain Entities | 12 | 🚧 In Progress (3/12) |
| Repositories | 12 | ⏳ Pending |
| DTOs | 20+ | ⏳ Pending |
| Domain Events | 8 | ⏳ Pending |
| Event Handlers | 3 | ⏳ Pending |
| Services | 12 | ⏳ Pending |
| Controllers | 6 | ⏳ Pending |

**Total Estimated: ~80 files**

---

## 🗄️ Database Schema (✅ Complete)

### Tables Created (V113):

1. **courses** - Main course table
   - State machine: DRAFT → REVIEW → PUBLISHED → ARCHIVED
   - Tier-based access (FREE/SILVER/GOLD/VIP)
   - Instructor assignment
   - Category integration
   - Rating system

2. **course_modules** - Course structure
   - Ordered modules within course
   - Duration tracking
   - Preview support

3. **lessons** - Learning content
   - Multiple types: TEXT, VIDEO, QUIZ, ASSIGNMENT, LIVE_SESSION, DOCUMENT
   - Video URL support
   - Attachment support
   - Preview lessons

4. **quizzes** - Assessment system
   - Passing score requirements
   - Time limits
   - Attempt limits
   - Randomization support

5. **quiz_questions** - Quiz content
   - Multiple question types
   - Points system
   - Explanations

6. **enrollments** - User-Course relationship
   - Progress tracking
   - Completion tracking
   - Payment tracking
   - Certificate linking

7. **lesson_progress** - Detailed tracking
   - Time spent per lesson
   - Video position tracking
   - Completion percentage

8. **quiz_attempts** - Assessment history
   - Score tracking
   - Answer storage (JSON)
   - Pass/fail tracking

9. **course_reviews** - Rating & feedback
   - 1-5 star rating
   - Review text
   - Verified purchase flag

10. **achievements** - Gamification
    - Multiple types
    - Point rewards
    - Badge system

11. **user_achievements** - User progress
    - Achievement tracking
    - Progress percentage

12. **certificates** - Completion proof
    - Unique certificate numbers
    - Verification codes
    - PDF generation ready

---

## 🎨 Domain Model Architecture

### Enums (✅ Complete):

```java
// Course related
- CourseStatus (DRAFT, REVIEW, PUBLISHED, ARCHIVED, DELETED)
- CourseLevel (BEGINNER, INTERMEDIATE, ADVANCED, EXPERT)

// Learning related
- LessonType (TEXT, VIDEO, QUIZ, ASSIGNMENT, LIVE_SESSION, DOCUMENT)
- LessonProgressStatus (NOT_STARTED, IN_PROGRESS, COMPLETED)

// Assessment related
- QuestionType (MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, ESSAY)

// Enrollment related
- EnrollmentStatus (ACTIVE, COMPLETED, DROPPED, EXPIRED, SUSPENDED)

// Gamification
- AchievementType (COURSE_COMPLETION, QUIZ_MASTER, STREAK, etc.)

// Reuse from Phase 1
- MemberTier (FREE, SILVER, GOLD, VIP)
```

### Entity Hierarchy:

```
Course (StatefulEntity<CourseStatus>)
├── modules: List<CourseModule>
│   └── lessons: List<Lesson>
│       └── quiz: Quiz
│           └── questions: List<QuizQuestion>
├── enrollments: List<Enrollment>
│   ├── lessonProgress: List<LessonProgress>
│   └── quizAttempts: List<QuizAttempt>
├── reviews: List<CourseReview>
└── category: ContentCategory (from Phase 1)

Achievement
└── userAchievements: List<UserAchievement>

Certificate
└── enrollment: Enrollment
```

---

## 🔄 Business Logic & Workflows

### 1. Course Creation & Publishing

```
Admin creates course (DRAFT)
  ↓
Add modules & lessons
  ↓
Add quizzes & questions
  ↓
Submit for review (REVIEW)
  ↓
Approve & Publish (PUBLISHED)
  ↓
Students can enroll
```

### 2. Enrollment & Learning Flow

```
Student browses courses
  ↓
Check tier requirement (FREE/SILVER/GOLD/VIP)
  ↓
Enroll in course (payment if needed)
  ↓
Start learning (ACTIVE)
  ↓
Complete lessons → Track progress
  ↓
Take quizzes → Score & pass/fail
  ↓
Complete all lessons (100% progress)
  ↓
Mark enrollment as COMPLETED
  ↓
Issue certificate
  ↓
Award achievements
```

### 3. Progress Tracking

```
Student opens lesson
  ↓
Create/Update LessonProgress (IN_PROGRESS)
  ↓
Track time spent & position
  ↓
Complete lesson → Mark COMPLETED
  ↓
Calculate enrollment progress percentage
  ↓
Check for auto-completion (100%)
  ↓
Update enrollment status
```

### 4. Quiz Assessment

```
Student starts quiz
  ↓
Create QuizAttempt (attempt_number++)
  ↓
Check max_attempts limit
  ↓
Present questions (randomized if enabled)
  ↓
Student answers & submits
  ↓
Calculate score & percentage
  ↓
Check passing_score
  ↓
Save attempt with answers (JSON)
  ↓
Show results (if enabled)
  ↓
Update lesson progress
```

### 5. Certificate Generation

```
Enrollment reaches 100% progress
  ↓
All required quizzes passed
  ↓
Generate unique certificate_number
  ↓
Generate verification_code
  ↓
Create Certificate record
  ↓
Generate PDF (optional)
  ↓
Send email notification
  ↓
Update enrollment.certificate_issued_at
```

### 6. Achievement System

```
Student completes action (lesson, quiz, course)
  ↓
Check achievement criteria (JSON rules)
  ↓
If criteria met → Award achievement
  ↓
Create UserAchievement record
  ↓
Award points
  ↓
Send notification
  ↓
Check tier upgrade (Phase 3 integration)
```

---

## 🔗 Integration Points

### With Phase 1 (CMS):

```java
// Content & Course linkage
Course.category → ContentCategory

// Engagement scoring
LessonCompletedEvent → memberScoreService.addPoints(5)
QuizPassedEvent → memberScoreService.addPoints(10)
CourseCompletedEvent → memberScoreService.addPoints(50)

// Content recommendations
User views content about "Spring Boot"
  → Recommend Spring Boot courses
```

### With Phase 3 (Membership):

```java
// Tier-based access
Course.tierRequired → Check user.memberTier

// Tier upgrade triggers
Course completion → Check upgrade eligibility
High quiz scores → Bonus points
Learning streak → Achievement points

// Member benefits
SILVER: 10% off all courses
GOLD: 20% off + exclusive courses
VIP: All courses free + priority support
```

### Email Notifications:

```java
// Reuse EmailService from Phase 1
- Enrollment confirmation
- Course completion congratulations
- Certificate issued
- Achievement unlocked
- Quiz results
- Reminder emails (incomplete courses)
```

---

## 📋 Domain Entities (To Implement)

### 1. Course.java
```java
@Entity
@Table(name = "courses")
public class Course extends StatefulEntity<CourseStatus> {
    private String code;
    private String title;
    private String slug;
    private String description;
    private CourseLevel courseLevel;
    private MemberTier tierRequired;
    private BigDecimal price;

    @ManyToOne
    private User instructor;

    @ManyToOne
    private ContentCategory category;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC")
    private List<CourseModule> modules = new ArrayList<>();

    private Integer enrollmentCount = 0;
    private Integer completionCount = 0;
    private BigDecimal ratingAverage;
    private Integer ratingCount = 0;

    // Business methods
    public void publish();
    public void enroll(User user);
    public void updateRating(int newRating);
    public int getTotalLessons();
    public int getDurationMinutes();
}
```

### 2. CourseModule.java
```java
@Entity
@Table(name = "course_modules")
public class CourseModule extends SoftDeletableEntity {
    @ManyToOne
    private Course course;

    private String title;
    private String description;
    private Integer sortOrder;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC")
    private List<Lesson> lessons = new ArrayList<>();

    public int getLessonCount();
    public int getDurationMinutes();
}
```

### 3. Lesson.java
```java
@Entity
@Table(name = "lessons")
public class Lesson extends SoftDeletableEntity {
    @ManyToOne
    private CourseModule module;

    private String title;
    private String content;
    private LessonType lessonType;
    private String videoUrl;
    private Integer videoDurationSeconds;
    private Integer sortOrder;

    @ManyToOne
    private Attachment attachment;

    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL)
    private Quiz quiz;

    public boolean isCompleted(Enrollment enrollment);
}
```

### 4. Quiz.java
```java
@Entity
@Table(name = "quizzes")
public class Quiz extends SoftDeletableEntity {
    @OneToOne
    private Lesson lesson;

    private String title;
    private String description;
    private Integer passingScore = 70;
    private Integer timeLimitMinutes;
    private Integer maxAttempts;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    public int getTotalPoints();
    public QuizAttempt createAttempt(User user, Enrollment enrollment);
}
```

### 5. Enrollment.java
```java
@Entity
@Table(name = "enrollments")
public class Enrollment extends BaseEntity {
    @ManyToOne
    private Course course;

    @ManyToOne
    private User user;

    private EnrollmentStatus status;
    private Integer progressPercentage = 0;
    private Instant enrolledAt;
    private Instant completedAt;

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL)
    private List<LessonProgress> lessonProgresses = new ArrayList<>();

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL)
    private List<QuizAttempt> quizAttempts = new ArrayList<>();

    @OneToOne(mappedBy = "enrollment")
    private Certificate certificate;

    // Business methods
    public void updateProgress();
    public void completeLesson(Lesson lesson);
    public void complete();
    public boolean canAccessLesson(Lesson lesson);
}
```

---

## 🌐 API Endpoints (To Implement)

### Course APIs (~15 endpoints)
```
POST   /api/courses                     - Create course
PUT    /api/courses/{id}                - Update course
DELETE /api/courses/{id}                - Delete course
GET    /api/courses                     - List courses
GET    /api/courses/{slug}              - Get by slug
GET    /api/courses/instructor/{id}     - By instructor
GET    /api/courses/category/{id}       - By category
GET    /api/courses/featured            - Featured courses
GET    /api/courses/search              - Search
POST   /api/courses/{id}/publish        - Publish
POST   /api/courses/{id}/enroll         - Enroll student
GET    /api/courses/{id}/students       - List students
GET    /api/courses/{id}/analytics      - Course analytics
```

### Module & Lesson APIs (~10 endpoints)
```
POST   /api/courses/{courseId}/modules           - Add module
PUT    /api/modules/{id}                         - Update module
DELETE /api/modules/{id}                         - Delete module
POST   /api/modules/{moduleId}/lessons           - Add lesson
PUT    /api/lessons/{id}                         - Update lesson
DELETE /api/lessons/{id}                         - Delete lesson
GET    /api/lessons/{id}                         - Get lesson
POST   /api/lessons/{id}/complete                - Mark complete
GET    /api/lessons/{id}/next                    - Get next lesson
```

### Quiz APIs (~8 endpoints)
```
POST   /api/lessons/{lessonId}/quiz              - Create quiz
PUT    /api/quizzes/{id}                         - Update quiz
DELETE /api/quizzes/{id}                         - Delete quiz
POST   /api/quizzes/{id}/questions               - Add question
POST   /api/quizzes/{id}/start                   - Start attempt
POST   /api/quizzes/{id}/submit                  - Submit answers
GET    /api/quizzes/{id}/attempts                - Get attempts
GET    /api/quizzes/{id}/results/{attemptId}    - Get results
```

### Enrollment APIs (~12 endpoints)
```
GET    /api/enrollments/my-courses              - My enrolled courses
GET    /api/enrollments/{id}                    - Get enrollment
GET    /api/enrollments/{id}/progress           - Get progress
POST   /api/enrollments/{id}/drop               - Drop course
GET    /api/enrollments/{id}/certificate        - Get certificate
POST   /api/enrollments/{id}/review             - Add review
```

### Achievement APIs (~6 endpoints)
```
GET    /api/achievements                        - List all
GET    /api/achievements/my                     - My achievements
GET    /api/achievements/{id}                   - Get achievement
POST   /api/achievements                        - Create (admin)
```

**Total: ~51 REST endpoints**

---

## 🔔 Domain Events

### Learning Events:
1. `CourseCreatedEvent`
2. `CoursePublishedEvent`
3. `CourseEnrolledEvent`
4. `LessonStartedEvent`
5. `LessonCompletedEvent`
6. `QuizAttemptedEvent`
7. `QuizPassedEvent`
8. `CourseCompletedEvent`
9. `CertificateIssuedEvent`
10. `AchievementEarnedEvent`

### Event Handlers:
- `CourseEventHandler` - CQRS sync
- `LearningProgressEventHandler` - Progress tracking
- `AchievementEventHandler` - Award achievements

---

## 📈 Analytics & Reporting

### Course Analytics:
- Total enrollments
- Active students
- Completion rate
- Average rating
- Average time to complete
- Drop-off points (which lessons lose students)

### Student Analytics:
- Courses enrolled
- Courses completed
- Total learning time
- Quiz average score
- Achievements earned
- Learning streak

### Instructor Dashboard:
- Total students taught
- Course ratings
- Revenue generated
- Student feedback
- Popular courses

---

## 🎮 Gamification Features

### Achievements:
- **First Step**: Complete first lesson
- **Quiz Master**: Pass all quizzes with 100%
- **Speed Demon**: Complete course in < 7 days
- **Perfectionist**: Perfect score on all assessments
- **Marathon Runner**: 30-day learning streak
- **Knowledge Seeker**: Complete 10 courses

### Points System:
```
Enroll in course:        +10 points
Complete lesson:         +5 points
Pass quiz (70-79%):      +10 points
Pass quiz (80-89%):      +15 points
Pass quiz (90-100%):     +20 points
Complete course:         +50 points
Write review:            +5 points
Daily login:             +1 point
7-day streak:            +20 points
Achievement earned:      Variable (5-100 points)
```

---

## 🔐 Access Control & Permissions

### Tier-Based Course Access:
```java
FREE tier:
  - Access FREE courses only
  - Limited previews of paid courses

SILVER tier:
  - All FREE courses
  - 10% discount on SILVER courses
  - Full SILVER course access

GOLD tier:
  - All FREE + SILVER courses
  - 20% discount on GOLD courses
  - Full GOLD course access
  - Priority support

VIP tier:
  - Access to ALL courses (free)
  - Exclusive VIP-only courses
  - 1-on-1 instructor sessions
  - Custom learning paths
```

### Role-Based Permissions:
- **Student**: Enroll, learn, take quizzes, review
- **Instructor**: Create/manage own courses, view analytics
- **Admin**: Full course management, approve reviews
- **Super Admin**: System configuration, achievement management

---

## 📝 Implementation Order (Recommended)

### Week 1: Core Entities & Repositories
- ✅ Database migration
- ✅ Enums
- 🚧 Domain entities (12 files)
- ⏳ Repositories (12 files)

### Week 2: Services & Business Logic
- ⏳ Course service
- ⏳ Enrollment service
- ⏳ Quiz service
- ⏳ Progress tracking service
- ⏳ Certificate service
- ⏳ Achievement service

### Week 3: APIs & Event Handling
- ⏳ REST controllers (6 files)
- ⏳ DTOs (20+ files)
- ⏳ Domain events (10 files)
- ⏳ Event handlers (3 files)

### Week 4: Integration & Testing
- ⏳ Phase 1 (CMS) integration
- ⏳ Phase 3 (Membership) prep
- ⏳ Email notifications
- ⏳ Analytics endpoints
- ⏳ Testing & documentation

---

## 🚀 Next Steps

**Current Status**: Database schema ✅, Enums ✅, Starting entities

**To Continue**:
1. Complete domain entities (Course, Enrollment, Lesson, etc.)
2. Create repositories with advanced queries
3. Implement services with business logic
4. Build REST APIs
5. Add event handling
6. Integrate with Phase 1 & 3

**Want to proceed? Let me know and I'll continue with the entities!** 🎓
