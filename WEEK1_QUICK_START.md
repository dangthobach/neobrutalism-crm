# 🚀 Week 1 Quick Start Guide

## Prerequisites

- Java 21+
- PostgreSQL 15+ (hoặc H2 for dev)
- MailHog (for email testing) - **RECOMMENDED**

## 1. Setup Email Testing với MailHog

```bash
# Start MailHog (SMTP server + Web UI)
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog

# MailHog Web UI: http://localhost:8025
# SMTP Server: localhost:1025
```

**application.yml đã configured:**
```yaml
spring.mail:
  host: localhost
  port: 1025  # MailHog SMTP
```

## 2. Start Application

```bash
mvn spring-boot:run

# Or với profile specific:
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Verify startup logs:**
```
[INFO] Loading Casbin policies from database...
[INFO] Successfully loaded 45 policies for 5 roles
```

## 3. Test Casbin Authorization

### Get JWT Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# Save token
export TOKEN="<your-jwt-token>"
```

### Test Protected Endpoint
```bash
# Without token → 401 Unauthorized
curl http://localhost:8080/api/users

# With token → Check Casbin permission
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN"

# If no permission → 403 Forbidden
# If has permission → 200 Success
```

### Verify Policies in Database
```sql
-- Check casbin_rule table
SELECT * FROM casbin_rule WHERE ptype = 'p' LIMIT 10;

-- Example output:
-- | ptype | v0    | v1                                   | v2         | v3     | v4    |
-- |-------|-------|--------------------------------------|------------|--------|-------|
-- | p     | ADMIN | 123e4567-e89b-12d3-a456-426614174000 | /api/users | read   | allow |
-- | p     | ADMIN | 123e4567-e89b-12d3-a456-426614174000 | /api/users | create | allow |
```

## 4. Test Email Service

### Simple Test Email
```bash
curl -X POST "http://localhost:8080/api/email/test/send?toEmail=test@example.com" \
  -H "Authorization: Bearer $TOKEN"

# Check MailHog UI: http://localhost:8025
# You should see email with Neobrutalism template
```

### Email with Attachment
```bash
curl -X POST "http://localhost:8080/api/email/test/send-with-file" \
  -H "Authorization: Bearer $TOKEN" \
  -d "toEmail=test@example.com" \
  -d "subject=Test with Attachment" \
  -d "body=<h1>Hello</h1><p>This is a test email with attachment</p>" \
  -d "fileName=test-document.txt" \
  -d "fileContent=This is the content of my test file. Lorem ipsum dolor sit amet."

# Check MailHog UI → Email should have attachment
```

### Test 10MB Limit
```bash
# Generate 11MB file content (will fail)
python3 -c "print('x' * (11 * 1024 * 1024))" > large.txt

curl -X POST "http://localhost:8080/api/email/test/send-with-file" \
  -H "Authorization: Bearer $TOKEN" \
  -d "toEmail=test@example.com" \
  -d "subject=Large File" \
  -d "body=This should fail" \
  -F "fileContent=@large.txt"

# Expected response: 
# { "success": false, "message": "File size exceeds maximum 10485760 bytes" }
```

## 5. Test Notification APIs

### Create Notification
```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": "<user-uuid>",
    "title": "Test Notification",
    "message": "This is a test notification with <b>HTML</b> content",
    "notificationType": "SYSTEM",
    "priority": 1,
    "actionUrl": "/tasks/123"
  }'
```

### Get My Notifications
```bash
curl http://localhost:8080/api/notifications/me \
  -H "Authorization: Bearer $TOKEN"
```

### Bulk Mark as Read
```bash
curl -X POST http://localhost:8080/api/notifications/bulk/read \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '["uuid1", "uuid2", "uuid3"]'
```

### Mark as Unread
```bash
curl -X PUT http://localhost:8080/api/notifications/{id}/unread \
  -H "Authorization: Bearer $TOKEN"
```

### Bulk Delete
```bash
curl -X DELETE http://localhost:8080/api/notifications/bulk \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '["uuid1", "uuid2", "uuid3"]'
```

## 6. Verify Casbin Policy Sync

### Update Role Permissions (via SQL)
```sql
-- Add new permission to ADMIN role
INSERT INTO role_menus (id, role_id, menu_id, can_view, can_create, can_edit, can_delete)
VALUES (
  gen_random_uuid(),
  (SELECT id FROM roles WHERE code = 'ADMIN'),
  (SELECT id FROM menus WHERE code = 'REPORTS'),
  true, true, false, false
);
```

### Trigger Policy Reload
```bash
# Call manual reload endpoint (if you create one)
curl -X POST http://localhost:8080/api/admin/casbin/reload \
  -H "Authorization: Bearer $TOKEN"

# OR restart application (auto-loads on startup)
```

### Verify New Policy
```bash
# Try accessing new resource
curl http://localhost:8080/api/reports \
  -H "Authorization: Bearer $TOKEN"

# Should now return 200 (if policy loaded correctly)
```

## 7. Test Email with Real Notification

### Create Notification that Triggers Email
```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": "<user-uuid>",
    "title": "🚨 High Priority Task Assigned",
    "message": "You have been assigned a critical task: <b>Fix Production Bug</b><br/>Deadline: Tomorrow 9 AM",
    "notificationType": "TASK_ASSIGNED",
    "priority": 2,
    "actionUrl": "/tasks/456",
    "entityType": "TASK",
    "entityId": "456"
  }'

# This should:
# 1. Save notification to database
# 2. Send WebSocket notification (real-time)
# 3. Send email to user (check MailHog)
```

### Check Email in MailHog
1. Open http://localhost:8025
2. You should see email with:
   - Subject: "🚨 High Priority Task Assigned"
   - Professional Neobrutalism template
   - HTML content with styling
   - "View in CRM" button

## 8. Test Quiet Hours

### Enable Quiet Hours
```yaml
# application.yml
notification:
  email:
    quiet-hours:
      enabled: true
      start: 22  # 10 PM
      end: 7     # 7 AM
      timezone: Asia/Ho_Chi_Minh
```

### Test During Quiet Hours
```bash
# Change system time to 23:00 or create notification during quiet hours
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": "<user-uuid>",
    "title": "Late Night Notification",
    "message": "This should NOT send email",
    "notificationType": "SYSTEM"
  }'

# Check logs:
# [DEBUG] Quiet hours active. Skipping email to: user@example.com
```

## 9. Production Email Setup

### Gmail SMTP
```yaml
spring.mail:
  host: smtp.gmail.com
  port: 587
  username: ${MAIL_USERNAME}
  password: ${MAIL_PASSWORD}  # App password, not regular password
  properties:
    mail.smtp:
      auth: true
      starttls.enable: true
      starttls.required: true
```

### SendGrid
```yaml
spring.mail:
  host: smtp.sendgrid.net
  port: 587
  username: apikey
  password: ${SENDGRID_API_KEY}
  properties:
    mail.smtp:
      auth: true
      starttls.enable: true
```

### AWS SES
```yaml
spring.mail:
  host: email-smtp.us-east-1.amazonaws.com
  port: 587
  username: ${AWS_SES_USERNAME}
  password: ${AWS_SES_PASSWORD}
  properties:
    mail.smtp:
      auth: true
      starttls.enable: true
```

## 10. Troubleshooting

### Email Not Sending
```bash
# Check logs
tail -f logs/application.log | grep -i email

# Common issues:
# 1. MailHog not running → Start Docker container
# 2. Wrong SMTP config → Check application.yml
# 3. Email disabled → Check notification.email.enabled=true
# 4. Quiet hours active → Check time and quiet-hours config
```

### Casbin Authorization Failing
```bash
# Check if policies loaded
SELECT COUNT(*) FROM casbin_rule WHERE ptype = 'p';

# Should return > 0

# Check specific user's roles
SELECT r.* FROM roles r
JOIN user_roles ur ON r.id = ur.role_id
WHERE ur.user_id = '<user-uuid>';

# Check role's policies
SELECT * FROM casbin_rule 
WHERE ptype = 'p' AND v0 = 'ADMIN';
```

### Build Errors
```bash
# Clean and rebuild
mvn clean compile -DskipTests

# Check Java version
java -version  # Should be 21+

# Check PostgreSQL connection
psql -h localhost -U postgres -d crm_db -c "SELECT 1"
```

## 11. Next Steps

After verifying Week 1 implementation:

1. **Week 2 - Phase 1 Frontend:**
   - Notification dropdown component
   - Real-time updates via WebSocket
   - Notification preferences UI

2. **Week 2 - Phase 2 Tasks:**
   - Remove hardcoded orgId
   - Dynamic assignee selection
   - Task detail page

3. **Week 2 - Phase 4 CI/CD:**
   - GitHub Actions workflow
   - Docker build
   - E2E tests

---

**Quick Health Check:**
```bash
# All systems operational?
curl http://localhost:8080/actuator/health

# Expected:
# {"status":"UP"}
```

**Happy Testing! 🎉**
