# Phase 2 LMS - Quick Start Guide

## 🚀 Quick Test Commands

### 1. Create a Course
```bash
POST http://localhost:8080/api/courses
Headers:
  Content-Type: application/json
  X-Tenant-Id: {your-tenant-id}
  Authorization: Bearer {token}

Body:
{
  "code": "DEMO101",
  "title": "Demo Course",
  "slug": "demo-course",
  "description": "A demo course for testing",
  "shortDescription": "Demo course",
  "courseLevel": "BEGINNER",
  "tierRequired": "FREE",
  "price": 0,
  "instructorId": "{instructor-user-id}"
}
```

### 2. Publish the Course
```bash
POST http://localhost:8080/api/courses/{courseId}/publish
Headers:
  Authorization: Bearer {token}
```

### 3. List Published Courses
```bash
GET http://localhost:8080/api/courses?page=0&size=10
```

### 4. Enroll in Course
```bash
POST http://localhost:8080/api/courses/{courseId}/enroll
Headers:
  X-Tenant-Id: {your-tenant-id}
  X-User-Id: {student-user-id}
  Authorization: Bearer {token}
```

### 5. View My Enrollments
```bash
GET http://localhost:8080/api/enrollments/my?page=0&size=10
Headers:
  X-User-Id: {student-user-id}
```

### 6. Update Progress
```bash
PUT http://localhost:8080/api/enrollments/{enrollmentId}/progress?progressPercentage=75
```

### 7. Complete Course
```bash
PUT http://localhost:8080/api/enrollments/{enrollmentId}/progress?progressPercentage=100
```

---

## 🗃️ Database Setup

### Run Migrations
```bash
# Migrations will run automatically on startup
# V112 - CMS tables (Phase 1)
# V113 - LMS tables (Phase 2)
# V114 - Add member_tier to users
```

### Verify Tables Created
```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name IN ('courses', 'enrollments', 'course_modules', 'lessons');
```

### Check User Member Tier
```sql
SELECT id, username, email, member_tier
FROM users
LIMIT 10;
```

---

## 📋 Test Checklist

### Basic Flow
- [ ] Create instructor user
- [ ] Create student user (ensure member_tier = 'FREE')
- [ ] Create course via API
- [ ] Publish course via API
- [ ] List courses - verify course appears
- [ ] Enroll student via API
- [ ] Verify enrollment created
- [ ] Update progress to 50%
- [ ] Update progress to 100%
- [ ] Verify CourseCompletedEvent logged

### Tier Access
- [ ] Create GOLD tier course (tierRequired = 'GOLD')
- [ ] Try enrolling FREE user → Should fail
- [ ] Update user.member_tier to 'GOLD'
- [ ] Try enrolling again → Should succeed

### Validation Tests
- [ ] Try creating course with duplicate code → Should fail
- [ ] Try enrolling in unpublished course → Should fail
- [ ] Try enrolling same user twice → Should fail
- [ ] Try progress > 100 → Should fail

---

## 🔍 Debugging

### Check Application Logs
```bash
# Look for these log messages:
"Creating course: {code} for tenant: {tenantId}"
"Course created successfully: {courseId}"
"Publishing course: {courseId}"
"Course published successfully: {courseId}"
"Enrolling user {userId} in course {courseId}"
"Enrollment created: {enrollmentId}"
```

### Verify Events Published
```sql
-- If you have event outbox table
SELECT * FROM outbox_events
WHERE aggregate_type = 'Course'
OR aggregate_type = 'Enrollment'
ORDER BY occurred_at DESC;
```

### Check Enrollment Status
```sql
SELECT
    e.id,
    u.username AS student,
    c.title AS course,
    e.status,
    e.progress_percentage,
    e.enrolled_at,
    e.completed_at
FROM enrollments e
JOIN users u ON e.user_id = u.id
JOIN courses c ON e.course_id = c.id
ORDER BY e.enrolled_at DESC;
```

---

## ⚠️ Common Issues

### Issue: "User tier FREE cannot access course requiring tier GOLD"
**Solution:** Update user's member_tier:
```sql
UPDATE users SET member_tier = 'GOLD' WHERE id = '{user-id}';
```

### Issue: "Course code already exists"
**Solution:** Use a unique code or delete existing course:
```sql
DELETE FROM courses WHERE code = 'DEMO101';
```

### Issue: "User is already enrolled in this course"
**Solution:** This is expected! Each user can only enroll once. Drop the enrollment first:
```bash
POST /api/enrollments/{enrollmentId}/drop
```

### Issue: "Cannot enroll in unpublished course"
**Solution:** Publish the course first:
```bash
POST /api/courses/{courseId}/publish
```

---

## 📊 Sample Data

### Create Sample Users
```sql
-- Instructor
INSERT INTO users (id, username, email, password_hash, first_name, last_name,
                   organization_id, tenant_id, status, member_tier, data_scope)
VALUES (
    gen_random_uuid(),
    'instructor1',
    'instructor@demo.com',
    '$2a$10$...', -- bcrypt hash
    'John',
    'Instructor',
    '{org-id}',
    '{tenant-id}',
    'ACTIVE',
    'FREE',
    'SELF_ONLY'
);

-- Student
INSERT INTO users (id, username, email, password_hash, first_name, last_name,
                   organization_id, tenant_id, status, member_tier, data_scope)
VALUES (
    gen_random_uuid(),
    'student1',
    'student@demo.com',
    '$2a$10$...', -- bcrypt hash
    'Jane',
    'Student',
    '{org-id}',
    '{tenant-id}',
    'ACTIVE',
    'FREE',
    'SELF_ONLY'
);
```

---

## 🎯 Expected Results

### After Creating Course
```json
{
  "id": "...",
  "code": "DEMO101",
  "title": "Demo Course",
  "status": "DRAFT",
  "enrollmentCount": 0,
  "completionCount": 0
}
```

### After Publishing
```json
{
  "id": "...",
  "status": "PUBLISHED",
  "publishedAt": "2025-11-02T..."
}
```

### After Enrollment
```json
{
  "id": "...",
  "userId": "...",
  "courseId": "...",
  "status": "ACTIVE",
  "progressPercentage": 0,
  "enrolledAt": "2025-11-02T..."
}
```

### After Completion
```json
{
  "id": "...",
  "status": "COMPLETED",
  "progressPercentage": 100,
  "completedAt": "2025-11-02T..."
}
```

---

## 🔗 API Documentation

Full Swagger docs available at:
```
http://localhost:8080/swagger-ui.html
```

Look for:
- **Courses** tag - Course management
- **Enrollments** tag - Enrollment management

---

## 📞 Next Steps

1. ✅ Test basic enrollment flow
2. ⏳ Implement LessonProgressService
3. ⏳ Implement QuizService
4. ⏳ Add event handlers for notifications
5. ⏳ Implement certificate generation

---

## 🎓 Learning Resources

### Understanding the Flow
1. **Course Creation** → CourseService.createCourse()
2. **Publishing** → CourseService.publishCourse() → CoursePublishedEvent
3. **Enrollment** → EnrollmentService.enrollUserInCourse() → StudentEnrolledEvent
4. **Progress** → EnrollmentService.updateProgress() → CourseCompletedEvent (if 100%)

### Key Files to Review
- [CourseService.java](src/main/java/com/neobrutalism/crm/domain/course/service/CourseService.java)
- [EnrollmentService.java](src/main/java/com/neobrutalism/crm/domain/course/service/EnrollmentService.java)
- [CourseController.java](src/main/java/com/neobrutalism/crm/domain/course/controller/CourseController.java)
- [EnrollmentController.java](src/main/java/com/neobrutalism/crm/domain/course/controller/EnrollmentController.java)
