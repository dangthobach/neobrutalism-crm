# Email & Notification Testing Guide

**Date:** November 23, 2025  
**Status:** Ready for Testing

## Quick Start - Test Email Notifications

### 1. Start MailHog

```bash
# From project root
docker-compose up -d mailhog

# Verify MailHog is running
docker ps | grep mailhog

# Access Web UI
open http://localhost:8025
```

### 2. Configure Application

**Verify `application.yml` has correct settings:**
```yaml
spring:
  mail:
    host: localhost
    port: 1025
    username:
    password:
```

### 3. Start Backend

```bash
# Terminal 1 - Backend
cd d:\project\neobrutalism-crm
mvn spring-boot:run

# Or if using IDE, run CrmApplication.java
```

### 4. Start Frontend

```bash
# Terminal 2 - Frontend
cd d:\project\neobrutalism-crm
pnpm dev

# Access: http://localhost:3000
```

### 5. Test Email Sending

#### Option A: Via API (curl)

```bash
# Login first
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin@crm.local",
    "password": "admin123"
  }' | jq -r '.data.accessToken')

# Create notification (triggers email)
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TASK_ASSIGNED",
    "priority": "HIGH",
    "title": "Test Email Notification",
    "message": "This is a test to verify email delivery works correctly",
    "recipientIds": ["YOUR_USER_ID"],
    "organizationId": "YOUR_ORG_ID",
    "entityType": "TASK",
    "entityId": "123e4567-e89b-12d3-a456-426614174000",
    "actionUrl": "/admin/tasks/123e4567-e89b-12d3-a456-426614174000"
  }'
```

#### Option B: Via UI

1. Login to app: http://localhost:3000
2. Navigate to Tasks
3. Create a new task and assign to someone
4. This will trigger a TASK_ASSIGNED notification
5. Check MailHog UI for the email

#### Option C: Via Postman/Thunder Client

**Import this collection:**
```json
{
  "name": "Notification Email Test",
  "requests": [
    {
      "name": "Login",
      "method": "POST",
      "url": "http://localhost:8080/api/auth/login",
      "body": {
        "username": "admin@crm.local",
        "password": "admin123"
      }
    },
    {
      "name": "Send Notification",
      "method": "POST",
      "url": "http://localhost:8080/api/notifications",
      "headers": {
        "Authorization": "Bearer {{token}}"
      },
      "body": {
        "type": "TASK_ASSIGNED",
        "priority": "HIGH",
        "title": "Test Email",
        "message": "Testing email delivery",
        "recipientIds": ["user-id"]
      }
    }
  ]
}
```

### 6. Verify Email in MailHog

1. Open http://localhost:8025
2. You should see the email in the list
3. Click on it to view:
   - HTML rendering (styled email with Thymeleaf template)
   - Priority badge (color-coded: Red=URGENT, Orange=HIGH, etc.)
   - Action button (links to task/entity)
   - Plain text fallback
   - Email headers

**Expected Email Content:**
```
From: Neobrutalism CRM <noreply@crm.local>
To: user@example.com
Subject: Test Email Notification

[Beautiful HTML email with:]
- Priority badge (RED for HIGH priority)
- Title: "Test Email Notification"
- Message: "This is a test..."
- "View Task" button
- Footer with preferences link
```

---

## Test Different Notification Types

### TASK_ASSIGNED
```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TASK_ASSIGNED",
    "priority": "NORMAL",
    "title": "New Task Assigned",
    "message": "You have been assigned: Update customer database",
    "recipientIds": ["user-id"]
  }'
```

### TASK_COMPLETED
```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TASK_COMPLETED",
    "priority": "LOW",
    "title": "Task Completed",
    "message": "Database backup task has been completed",
    "recipientIds": ["user-id"]
  }'
```

### SYSTEM (URGENT)
```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "SYSTEM",
    "priority": "URGENT",
    "title": "System Alert",
    "message": "Critical: Database backup failed",
    "recipientIds": ["user-id"]
  }'
```

---

## Test Email Preferences

### 1. Disable Email for User

```bash
# Update user's notification preferences
curl -X PUT http://localhost:8080/api/notifications/preferences/TASK_ASSIGNED \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "emailEnabled": false,
    "pushEnabled": true,
    "inAppEnabled": true
  }'
```

### 2. Send Notification

```bash
# Should NOT receive email (preference disabled)
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TASK_ASSIGNED",
    "priority": "HIGH",
    "title": "Test - Should NOT Send Email",
    "message": "Email is disabled for this notification type",
    "recipientIds": ["user-id"]
  }'
```

### 3. Verify in MailHog

- Should see NO new email (preference blocked it)
- Check backend logs: `Email notifications disabled for user`

---

## Test Quiet Hours

### 1. Enable Quiet Hours

**Update `application.yml`:**
```yaml
notification:
  email:
    quiet-hours:
      enabled: true
      start: 22  # 10 PM
      end: 7     # 7 AM
      timezone: Asia/Ho_Chi_Minh
```

### 2. Test During Quiet Hours

```bash
# If current time is between 22:00-07:00
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TASK_ASSIGNED",
    "priority": "HIGH",
    "title": "Test - Quiet Hours",
    "message": "Should be queued, not sent immediately",
    "recipientIds": ["user-id"]
  }'
```

### 3. Verify

- Check MailHog: No immediate email
- Check backend logs: `User is in quiet hours, skipping email`
- Email will be queued for later delivery

---

## Test Priority Badges

Send notifications with different priorities and verify badge colors:

| Priority | Badge Color | Background |
|----------|-------------|------------|
| URGENT   | White text  | Red (#DC2626) |
| HIGH     | White text  | Orange (#EA580C) |
| NORMAL   | Black text  | Yellow (#FACC15) |
| LOW      | White text  | Gray (#6B7280) |

```bash
# Test all priorities
for priority in URGENT HIGH NORMAL LOW; do
  curl -X POST http://localhost:8080/api/notifications \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"type\": \"TASK_ASSIGNED\",
      \"priority\": \"$priority\",
      \"title\": \"Test $priority Priority\",
      \"message\": \"Testing priority badge rendering\",
      \"recipientIds\": [\"user-id\"]
    }"
done
```

---

## Test Action Links

### 1. Notification with Action URL

```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TASK_ASSIGNED",
    "priority": "HIGH",
    "title": "Review Customer Feedback",
    "message": "Please review the customer feedback form",
    "recipientIds": ["user-id"],
    "entityType": "TASK",
    "entityId": "123e4567-e89b-12d3-a456-426614174000",
    "actionUrl": "http://localhost:3000/admin/tasks/123e4567-e89b-12d3-a456-426614174000"
  }'
```

### 2. Verify in Email

- Email should have "View Task" button
- Button links to: `http://localhost:3000/admin/tasks/...`
- Clicking should navigate to task detail page

---

## Test Digest Email

### 1. Enable Digest Mode

**Update `application.yml`:**
```yaml
notification:
  email:
    digest:
      enabled: true
      schedule: "0 */5 * * * ?"  # Every 5 minutes (for testing)
      max-age-hours: 24
```

### 2. Send Multiple Notifications

```bash
# Send 5 notifications rapidly
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/notifications \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"type\": \"TASK_ASSIGNED\",
      \"priority\": \"NORMAL\",
      \"title\": \"Test Notification $i\",
      \"message\": \"This is test notification number $i\",
      \"recipientIds\": [\"user-id\"]
    }"
done
```

### 3. Wait for Digest

- Wait 5 minutes (per schedule)
- Check MailHog for digest email
- Should show all 5 notifications grouped by priority

---

## Test Template Rendering

### 1. Check HTML Structure

In MailHog, inspect the HTML:
```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    /* Inline CSS for email clients */
  </style>
</head>
<body>
  <table width="100%" cellpadding="0" cellspacing="0">
    <!-- Email header -->
    <tr>
      <td style="background: #3B82F6; padding: 20px;">
        <h1 style="color: white;">Neobrutalism CRM</h1>
      </td>
    </tr>
    
    <!-- Priority badge -->
    <tr>
      <td style="padding: 20px;">
        <span style="background: #DC2626; color: white; padding: 4px 12px;">
          URGENT
        </span>
      </td>
    </tr>
    
    <!-- Content -->
    <tr>
      <td style="padding: 20px;">
        <h2>Test Email Notification</h2>
        <p>This is a test to verify email delivery works correctly</p>
      </td>
    </tr>
    
    <!-- Action button -->
    <tr>
      <td style="padding: 20px;">
        <a href="..." style="background: #3B82F6; color: white; padding: 12px 24px;">
          View Task
        </a>
      </td>
    </tr>
    
    <!-- Footer -->
    <tr>
      <td style="background: #F3F4F6; padding: 20px; text-align: center;">
        <a href="http://localhost:3000/admin/notifications/preferences">
          Manage notification preferences
        </a>
      </td>
    </tr>
  </table>
</body>
</html>
```

### 2. Test Plain Text Fallback

In MailHog, switch to "Plain text" tab:
```
Neobrutalism CRM

URGENT

Test Email Notification

This is a test to verify email delivery works correctly

View Task: http://localhost:3000/admin/tasks/...

---
Manage notification preferences: http://localhost:3000/admin/notifications/preferences
```

---

## Troubleshooting

### Email Not Appearing in MailHog

**Check:**
1. MailHog is running: `docker ps | grep mailhog`
2. Application config has correct host/port
3. Backend logs for errors: `tail -f logs/spring.log`
4. User has email preferences enabled
5. Not in quiet hours

**Debug Commands:**
```bash
# Check MailHog logs
docker logs crm-mailhog

# Check backend email service
curl http://localhost:8080/actuator/health

# Test SMTP connection
telnet localhost 1025
```

### Template Not Rendering

**Check:**
1. Template file exists: `src/main/resources/templates/email/notification.html`
2. Thymeleaf is configured correctly
3. Context variables are set
4. Check backend logs for Thymeleaf errors

### Action Links Not Working

**Check:**
1. `actionUrl` is set in notification
2. URL is absolute (includes protocol: `http://` or `https://`)
3. Frontend route exists: `/admin/tasks/[taskId]`
4. Task ID is valid UUID

---

## Success Criteria

✅ **Email Delivery:**
- Email appears in MailHog within 2 seconds
- HTML renders correctly
- Priority badge shows correct color
- Action button links to correct page

✅ **Preferences:**
- Disabled email types don't send
- Enabled types send successfully
- Quiet hours respected

✅ **Templates:**
- All variables populated
- No broken images
- Responsive design (mobile/desktop)
- Plain text fallback works

✅ **Performance:**
- Email sent asynchronously (doesn't block API)
- Batch notifications use digest
- No memory leaks after 100+ emails

---

## Next Steps

After testing locally:
1. Configure production SMTP (Gmail/SendGrid/AWS SES)
2. Update environment variables
3. Test with real email addresses
4. Monitor delivery rates
5. Setup alerts for failures

---

**Last Updated:** November 23, 2025  
**Testing Status:** Ready for QA
