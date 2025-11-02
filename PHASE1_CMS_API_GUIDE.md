# 📘 Phase 1: CMS API Guide

## 🎯 Tổng quan

Phase 1 CMS đã hoàn tất với đầy đủ tính năng quản lý nội dung, bao gồm:
- ✅ Content Management (Blog/Articles/Pages)
- ✅ Category Management (Hierarchical)
- ✅ Tag Management
- ✅ Series Management
- ✅ View Tracking & Analytics
- ✅ Tier-based Access Control

---

## 🚀 Cách Test

### 1. Start Application

```bash
# Run database migration (auto)
./mvnw spring-boot:run

# Migration V112__Create_content_management_tables.sql sẽ tự động chạy
```

### 2. Kiểm tra Database

```sql
-- Kiểm tra tables đã được tạo
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name LIKE 'content%';

-- Kết quả mong đợi:
-- contents
-- content_categories
-- content_tags
-- content_series
-- content_views
-- content_read_models
-- content_category_mappings
-- content_tag_mappings
```

---

## 📋 API Endpoints

### **Content Management**

#### **1. Create Content** (Admin)
```http
POST /api/contents
X-Tenant-ID: default
Content-Type: application/json

{
  "title": "Introduction to Spring Boot",
  "slug": "introduction-to-spring-boot",
  "summary": "Learn the basics of Spring Boot framework",
  "body": "<h1>Welcome to Spring Boot</h1><p>This is a comprehensive guide...</p>",
  "contentType": "BLOG",
  "tierRequired": "FREE",
  "categoryIds": [],
  "tagIds": [],
  "seoTitle": "Spring Boot Tutorial for Beginners",
  "seoDescription": "Complete Spring Boot tutorial covering all basics",
  "seoKeywords": "spring boot, java, tutorial"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Content created successfully",
  "data": {
    "id": "uuid",
    "title": "Introduction to Spring Boot",
    "slug": "introduction-to-spring-boot",
    "status": "DRAFT",
    "viewCount": 0,
    ...
  }
}
```

#### **2. Submit for Review**
```http
POST /api/contents/{id}/submit-review?reason=Ready for review
```

#### **3. Publish Content**
```http
POST /api/contents/{id}/publish?reason=Approved
```

#### **4. Get Published Content** (Public)
```http
GET /api/contents?page=0&size=20&sortBy=publishedAt&sortDirection=DESC
X-Tenant-ID: default
```

#### **5. Get Content by Slug** (Public)
```http
GET /api/contents/introduction-to-spring-boot
X-Tenant-ID: default
```

#### **6. Search Content**
```http
GET /api/contents/search?keyword=spring&page=0&size=20
```

#### **7. Get Trending Content**
```http
GET /api/contents/trending?days=7&page=0&size=10
```

#### **8. Get Recent Content**
```http
GET /api/contents/recent?days=30&page=0&size=20
```

#### **9. Filter by Tier**
```http
GET /api/contents/tier/SILVER?page=0&size=20
```

#### **10. Track View** (Analytics)
```http
POST /api/contents/{id}/view
Content-Type: application/json

{
  "sessionId": "session-abc-123",
  "timeSpentSeconds": 120,
  "scrollPercentage": 85,
  "referrer": "https://google.com"
}
```

#### **11. Get Content Stats** (Admin)
```http
GET /api/contents/{id}/stats
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalViews": 1250,
    "uniqueUsers": 890,
    "averageTimeSpentSeconds": 145,
    "averageScrollPercentage": 72
  }
}
```

---

### **Category Management**

#### **1. Create Category**
```http
POST /api/content-categories
X-Tenant-ID: default
Content-Type: application/json

{
  "name": "Technology",
  "slug": "technology",
  "description": "All about technology",
  "parentId": null,
  "sortOrder": 0
}
```

#### **2. Create Sub-category**
```http
POST /api/content-categories
X-Tenant-ID: default

{
  "name": "Programming",
  "slug": "programming",
  "description": "Programming tutorials",
  "parentId": "{parent-category-uuid}",
  "sortOrder": 0
}
```

#### **3. Get Root Categories**
```http
GET /api/content-categories/roots
X-Tenant-ID: default
```

#### **4. Get Category Children**
```http
GET /api/content-categories/{id}/children
```

#### **5. Get All Categories**
```http
GET /api/content-categories
X-Tenant-ID: default
```

#### **6. Get Categories with Content Count**
```http
GET /api/content-categories/with-count
X-Tenant-ID: default
```

#### **7. Get Category by Slug**
```http
GET /api/content-categories/slug/technology
X-Tenant-ID: default
```

#### **8. Update Category**
```http
PUT /api/content-categories/{id}

{
  "name": "Tech & Innovation",
  "description": "Updated description"
}
```

#### **9. Delete Category**
```http
DELETE /api/content-categories/{id}
```

---

### **Tag Management**

#### **1. Create Tag**
```http
POST /api/content-tags
X-Tenant-ID: default

{
  "name": "Spring Boot",
  "slug": "spring-boot",
  "color": "#3B82F6"
}
```

#### **2. Get All Tags**
```http
GET /api/content-tags
X-Tenant-ID: default
```

#### **3. Search Tags**
```http
GET /api/content-tags/search?name=spring
```

#### **4. Get Popular Tags**
```http
GET /api/content-tags/popular
X-Tenant-ID: default
```

#### **5. Get Tags with Content Count**
```http
GET /api/content-tags/with-count
X-Tenant-ID: default
```

#### **6. Get Tag by Slug**
```http
GET /api/content-tags/slug/spring-boot
X-Tenant-ID: default
```

#### **7. Get Tag by Name**
```http
GET /api/content-tags/name/Spring Boot
X-Tenant-ID: default
```

#### **8. Update Tag**
```http
PUT /api/content-tags/{id}

{
  "color": "#10B981"
}
```

#### **9. Delete Tag**
```http
DELETE /api/content-tags/{id}
```

---

### **Series Management**

#### **1. Create Series**
```http
POST /api/content-series
X-Tenant-ID: default

{
  "name": "Mastering Spring Framework",
  "slug": "mastering-spring-framework",
  "description": "A complete series on Spring Framework",
  "thumbnailId": null,
  "sortOrder": 0
}
```

#### **2. Get All Series**
```http
GET /api/content-series
X-Tenant-ID: default
```

#### **3. Get Series with Content Count**
```http
GET /api/content-series/with-count
X-Tenant-ID: default
```

#### **4. Get Series by Slug**
```http
GET /api/content-series/slug/mastering-spring-framework
X-Tenant-ID: default
```

#### **5. Update Series**
```http
PUT /api/content-series/{id}

{
  "description": "Updated description"
}
```

#### **6. Delete Series**
```http
DELETE /api/content-series/{id}
```

---

## 🔄 Complete Workflow Example

### **Scenario: Tạo và publish một blog post hoàn chỉnh**

#### **Step 1: Tạo Category**
```bash
POST /api/content-categories
{
  "name": "Web Development",
  "slug": "web-development",
  "description": "Web development tutorials"
}
# Lấy categoryId từ response
```

#### **Step 2: Tạo Tags**
```bash
POST /api/content-tags
{
  "name": "Java",
  "slug": "java",
  "color": "#F59E0B"
}

POST /api/content-tags
{
  "name": "Spring Boot",
  "slug": "spring-boot",
  "color": "#10B981"
}
# Lấy tagIds từ response
```

#### **Step 3: Tạo Series** (Optional)
```bash
POST /api/content-series
{
  "name": "Spring Boot Basics",
  "slug": "spring-boot-basics",
  "description": "Learn Spring Boot from scratch"
}
# Lấy seriesId từ response
```

#### **Step 4: Tạo Content**
```bash
POST /api/contents
{
  "title": "Getting Started with Spring Boot",
  "slug": "getting-started-spring-boot",
  "summary": "A beginner's guide to Spring Boot",
  "body": "<h1>Introduction</h1><p>Spring Boot makes...</p>",
  "contentType": "BLOG",
  "tierRequired": "FREE",
  "categoryIds": ["{categoryId}"],
  "tagIds": ["{javaTagId}", "{springBootTagId}"],
  "seriesId": "{seriesId}",
  "seoTitle": "Spring Boot Tutorial for Beginners 2025",
  "seoDescription": "Complete guide to getting started with Spring Boot",
  "seoKeywords": "spring boot, java, tutorial, beginners"
}
# Lấy contentId từ response
```

#### **Step 5: Submit for Review**
```bash
POST /api/contents/{contentId}/submit-review?reason=Content ready
```

#### **Step 6: Publish**
```bash
POST /api/contents/{contentId}/publish?reason=Approved by editor
```

#### **Step 7: Verify Published**
```bash
GET /api/contents/getting-started-spring-boot
# Should return published content
```

#### **Step 8: Simulate User Reading**
```bash
POST /api/contents/{contentId}/view
{
  "sessionId": "user-session-123",
  "timeSpentSeconds": 300,
  "scrollPercentage": 95
}
```

#### **Step 9: Check Analytics**
```bash
GET /api/contents/{contentId}/stats
# Should show 1 view with analytics data
```

---

## 🎨 Content Lifecycle

```
DRAFT → REVIEW → PUBLISHED → ARCHIVED
  ↓       ↓          ↓           ↓
DELETE  DELETE    DELETE      DELETE
```

**State Transitions:**
- `DRAFT → REVIEW`: Submit for review
- `REVIEW → PUBLISHED`: Publish
- `REVIEW → DRAFT`: Return to draft
- `PUBLISHED → ARCHIVED`: Archive
- `PUBLISHED → DRAFT`: Unpublish (return to draft)
- Any → `DELETED`: Soft delete

---

## 🔐 Tier-Based Access Control

### **Member Tiers:**
- `FREE` (Level 0)
- `SILVER` (Level 1)
- `GOLD` (Level 2)
- `VIP` (Level 3)

### **Access Rules:**
```java
// FREE tier can access: FREE content only
// SILVER tier can access: FREE + SILVER content
// GOLD tier can access: FREE + SILVER + GOLD content
// VIP tier can access: ALL content
```

### **Example:**
```bash
# Create VIP-only content
POST /api/contents
{
  "title": "Advanced Spring Boot Internals",
  "tierRequired": "VIP",
  ...
}

# Only VIP users can view this content
GET /api/contents/tier/VIP
```

---

## 📊 Analytics & Engagement

### **Engagement Scoring:**

```
Base Score: 1 point (for any view)
+ Authenticated: +2 points
+ Significant time (>30s): +3 points
+ Fully read (>70% scroll): +4 points
─────────────────────────────
Maximum: 10 points per view
```

### **View Tracking Captures:**
- ✅ User ID (if authenticated)
- ✅ Session ID
- ✅ IP Address
- ✅ User Agent
- ✅ Referrer URL
- ✅ Time spent (seconds)
- ✅ Scroll percentage
- ✅ Timestamp

---

## 🧪 Testing Checklist

### **Database**
- [ ] Run migration V112
- [ ] Verify all tables created
- [ ] Check indexes created
- [ ] Test full-text search indexes

### **Category APIs**
- [ ] Create root category
- [ ] Create sub-category
- [ ] Get roots
- [ ] Get children
- [ ] Update category
- [ ] Delete category

### **Tag APIs**
- [ ] Create tag
- [ ] Search tags
- [ ] Get popular tags
- [ ] Update tag
- [ ] Delete tag

### **Series APIs**
- [ ] Create series
- [ ] Get all series
- [ ] Update series
- [ ] Delete series

### **Content APIs**
- [ ] Create content in DRAFT
- [ ] Submit for REVIEW
- [ ] Publish content
- [ ] List published content
- [ ] Get by slug
- [ ] Search content
- [ ] Filter by category
- [ ] Filter by tag
- [ ] Get trending
- [ ] Get recent
- [ ] Filter by tier

### **View Tracking**
- [ ] Track anonymous view
- [ ] Track authenticated view
- [ ] Verify view count increment
- [ ] Get content stats

### **CQRS**
- [ ] Verify read model sync on create
- [ ] Verify read model sync on update
- [ ] Verify read model sync on publish
- [ ] Query from read model is fast

---

## 🐛 Common Issues & Solutions

### **Issue 1: Migration fails**
```
Error: Table already exists
```
**Solution:** Drop tables and re-run
```sql
DROP TABLE IF EXISTS content_tag_mappings CASCADE;
DROP TABLE IF EXISTS content_category_mappings CASCADE;
DROP TABLE IF EXISTS content_views CASCADE;
DROP TABLE IF EXISTS content_read_models CASCADE;
DROP TABLE IF EXISTS contents CASCADE;
DROP TABLE IF EXISTS content_series CASCADE;
DROP TABLE IF EXISTS content_tags CASCADE;
DROP TABLE IF EXISTS content_categories CASCADE;
```

### **Issue 2: Slug already exists**
```
Category with slug 'technology' already exists
```
**Solution:** Use unique slug or query existing:
```bash
GET /api/content-categories/slug/technology
```

### **Issue 3: Cannot publish from DRAFT**
```
Invalid state transition from DRAFT to PUBLISHED
```
**Solution:** Must go through REVIEW first:
```bash
POST /api/contents/{id}/submit-review
POST /api/contents/{id}/publish
```

---

## 🎯 Next Steps

Phase 1 hoàn tất! Bạn có thể:

1. **Test toàn bộ APIs** theo guide này
2. **Tạo sample data** cho demo
3. **Tiếp tục Phase 2 (LMS)** - Course/Lesson system
4. **Integrate Frontend** - Build React/Next.js UI

---

## 📞 Support

Nếu gặp issue, check:
1. Database migration logs
2. Application logs
3. API response errors
4. This guide's troubleshooting section

**Good luck testing! 🚀**
