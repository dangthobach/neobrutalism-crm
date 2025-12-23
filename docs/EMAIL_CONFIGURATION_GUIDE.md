# Email Configuration Guide

**Last Updated:** November 23, 2025  
**Version:** 1.0

## Overview

This guide covers email configuration for the Neobrutalism CRM notification system. The application supports multiple email providers and includes local testing tools for development.

---

## Table of Contents

1. [Quick Start - Local Development](#quick-start---local-development)
2. [Production Configuration](#production-configuration)
3. [Email Providers](#email-providers)
4. [Testing](#testing)
5. [Troubleshooting](#troubleshooting)
6. [Email Templates](#email-templates)

---

## Quick Start - Local Development

### Using MailHog (Recommended)

MailHog is an email testing tool that catches all outgoing emails without sending them.

**1. Start MailHog:**
```bash
docker-compose up -d mailhog
```

**2. Configure `application.yml` for MailHog:**
```yaml
spring:
  mail:
    host: localhost
    port: 1025
    username:  # Leave empty
    password:  # Leave empty
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false
            required: false
```

**3. Access MailHog Web UI:**
- Open browser: http://localhost:8025
- View all sent emails in real-time
- Test HTML rendering
- Inspect email headers

**4. Test Email Sending:**
```bash
# Create a test notification to trigger email
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TASK_ASSIGNED",
    "priority": "HIGH",
    "title": "Test Email",
    "message": "This is a test email notification",
    "recipientIds": ["user-id-here"]
  }'
```

**5. Verify in MailHog:**
- Check MailHog UI for the email
- Verify Thymeleaf template rendering
- Test different notification types

---

## Production Configuration

### Environment Variables

Set these environment variables in your production environment:

```bash
# Gmail Example
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password

# SendGrid Example
export MAIL_HOST=smtp.sendgrid.net
export MAIL_PORT=587
export MAIL_USERNAME=apikey
export MAIL_PASSWORD=your-sendgrid-api-key

# AWS SES Example
export MAIL_HOST=email-smtp.us-east-1.amazonaws.com
export MAIL_PORT=587
export MAIL_USERNAME=your-ses-smtp-username
export MAIL_PASSWORD=your-ses-smtp-password
```

### Application Configuration

**`application-prod.yml`:**
```yaml
spring:
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
        # Optional: Enable debug logging
        debug: false
    # From address
    from: noreply@yourcompany.com
    from-name: Neobrutalism CRM
```

### Docker Compose Production

**`docker-compose.prod.yml`:**
```yaml
services:
  crm-backend:
    environment:
      SPRING_PROFILES_ACTIVE: prod
      MAIL_HOST: ${MAIL_HOST}
      MAIL_PORT: ${MAIL_PORT}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      # ... other env vars
```

---

## Email Providers

### 1. Gmail

**Requirements:**
- Gmail account with 2FA enabled
- App Password (not regular password)

**Setup:**
1. Go to https://myaccount.google.com/security
2. Enable 2-Step Verification
3. Generate App Password:
   - Select "Mail" and "Other (Custom name)"
   - Copy the 16-character password
4. Use this password in `MAIL_PASSWORD`

**Configuration:**
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: xxxx-xxxx-xxxx-xxxx  # App password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**Limits:**
- 500 emails/day for free Gmail
- 2000 emails/day for Google Workspace

---

### 2. SendGrid

**Requirements:**
- SendGrid account (free tier: 100 emails/day)
- API Key with Mail Send permissions

**Setup:**
1. Sign up at https://sendgrid.com
2. Create API Key: Settings > API Keys > Create API Key
3. Select "Mail Send" permission
4. Copy the API key

**Configuration:**
```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey  # Literal string "apikey"
    password: SG.xxxxxxxxxxxxxxxxxxxxx  # Your API key
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**Limits:**
- Free: 100 emails/day
- Essentials: 100K emails/month ($19.95)
- Pro: 1.5M emails/month ($89.95)

---

### 3. AWS SES (Amazon Simple Email Service)

**Requirements:**
- AWS account
- Verified domain or email address
- SMTP credentials

**Setup:**
1. Verify domain in AWS SES console
2. Create SMTP credentials: SMTP Settings > Create SMTP Credentials
3. Move out of sandbox (production):
   - Request production access
   - Provide use case details

**Configuration:**
```yaml
spring:
  mail:
    host: email-smtp.us-east-1.amazonaws.com  # Region-specific
    port: 587
    username: AKIAIOSFODNN7EXAMPLE  # SMTP username
    password: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY  # SMTP password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**Limits:**
- Sandbox: 200 emails/day, only verified addresses
- Production: 50K emails/day (free tier), scales to millions
- $0.10 per 1,000 emails after free tier

**Regions:**
- US East (N. Virginia): `email-smtp.us-east-1.amazonaws.com`
- EU (Ireland): `email-smtp.eu-west-1.amazonaws.com`
- Asia Pacific (Tokyo): `email-smtp.ap-northeast-1.amazonaws.com`

---

### 4. Azure Communication Services

**Requirements:**
- Azure account
- Communication Services resource
- Email communication service

**Configuration:**
```yaml
spring:
  mail:
    host: smtp.azurecomm.net
    port: 587
    username: your-connection-string
    password: your-access-key
```

**Limits:**
- Pay-as-you-go: $0.00025 per email
- No daily limit (rate limits apply)

---

### 5. Mailgun

**Setup:**
```yaml
spring:
  mail:
    host: smtp.mailgun.org
    port: 587
    username: postmaster@your-domain.mailgun.org
    password: your-api-key
```

**Limits:**
- Free: 5,000 emails/month for 3 months
- Foundation: $35/month for 50K emails

---

## Testing

### Manual Testing

**1. Test SMTP Connection:**
```java
// src/test/java/com/neobrutalism/crm/EmailConnectionTest.java
@SpringBootTest
class EmailConnectionTest {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Test
    void testEmailConnection() throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo("test@example.com");
        helper.setSubject("Test Email");
        helper.setText("This is a test email");
        
        mailSender.send(message);
    }
}
```

**2. Test Notification Email:**
```bash
# Via REST API
POST /api/notifications
{
  "type": "TASK_ASSIGNED",
  "priority": "HIGH",
  "title": "Test Task Assignment",
  "message": "You have been assigned a new task",
  "recipientIds": ["user-uuid"]
}
```

**3. Test Thymeleaf Template:**
```java
@Test
void testTemplateRendering() {
    Context context = new Context();
    context.setVariable("notification", notification);
    context.setVariable("recipientName", "John Doe");
    
    String html = templateEngine.process("email/notification", context);
    
    assertThat(html).contains("Test Task Assignment");
    assertThat(html).contains("HIGH");
}
```

### Automated Testing

**Integration Test:**
```java
@SpringBootTest
@Testcontainers
class NotificationEmailIntegrationTest {
    
    @Container
    static GenericContainer<?> mailhog = new GenericContainer<>("mailhog/mailhog:latest")
        .withExposedPorts(1025, 8025);
    
    @Test
    void shouldSendEmailNotification() {
        // Create notification
        Notification notification = createTestNotification();
        
        // Send email
        emailService.sendNotificationEmail(notification, user);
        
        // Verify email sent (check MailHog API)
        RestTemplate restTemplate = new RestTemplate();
        String mailhogUrl = "http://localhost:" + mailhog.getMappedPort(8025);
        Messages messages = restTemplate.getForObject(mailhogUrl + "/api/v2/messages", Messages.class);
        
        assertThat(messages.getCount()).isEqualTo(1);
    }
}
```

---

## Troubleshooting

### Common Issues

#### 1. Authentication Failed

**Error:** `Authentication failed; nested exception is javax.mail.AuthenticationFailedException`

**Solutions:**
- Verify username and password are correct
- For Gmail: Use App Password, not regular password
- Check if 2FA is enabled (required for Gmail)
- For SendGrid: Use literal "apikey" as username

#### 2. Connection Timeout

**Error:** `Mail server connection timeout`

**Solutions:**
- Check `MAIL_HOST` is correct
- Verify port number (587 for TLS, 465 for SSL, 25 for plain)
- Check firewall rules
- Verify network connectivity: `telnet smtp.gmail.com 587`

#### 3. TLS/SSL Issues

**Error:** `Could not convert socket to TLS`

**Solutions:**
```yaml
spring:
  mail:
    properties:
      mail:
        smtp:
          starttls:
            enable: true
            required: true  # Force TLS
          ssl:
            trust: "*"  # Trust all certificates (dev only!)
```

#### 4. Rate Limiting

**Error:** `550 5.4.5 Daily sending quota exceeded`

**Solutions:**
- Monitor email sending volume
- Implement batch/digest mode for high-volume notifications
- Use quiet hours to reduce peak load
- Upgrade email provider plan
- Implement exponential backoff for retries

#### 5. Emails Going to Spam

**Solutions:**
- Configure SPF record: `v=spf1 include:_spf.google.com ~all`
- Configure DKIM signing (provider-specific)
- Configure DMARC policy: `v=DMARC1; p=quarantine; rua=mailto:dmarc@yourdomain.com`
- Use verified domain (not free email)
- Avoid spam trigger words in subject/body
- Include unsubscribe link
- Monitor sender reputation

---

## Email Templates

### Available Templates

**1. `notification.html` - Single Notification**
- Used for individual notification emails
- Shows priority badge, title, message, action button
- Responsive design

**Variables:**
```java
context.setVariable("notification", notification);
context.setVariable("recipientName", "John Doe");
context.setVariable("actionUrl", "https://crm.example.com/tasks/123");
```

**2. `notification-digest.html` - Daily Digest**
- Used for daily notification summary
- Lists multiple notifications grouped by priority
- Summary statistics

**Variables:**
```java
context.setVariable("notifications", notificationList);
context.setVariable("recipientName", "John Doe");
context.setVariable("totalCount", 15);
context.setVariable("date", LocalDate.now());
```

### Template Customization

**Location:** `src/main/resources/templates/email/`

**Thymeleaf Syntax:**
```html
<div th:if="${notification.priority == 'URGENT'}" class="priority-badge urgent">
  URGENT
</div>

<h2 th:text="${notification.title}">Task Title</h2>

<p th:text="${notification.message}">Task description...</p>

<a th:href="${actionUrl}" class="btn">View Task</a>
```

**Testing Templates:**
```bash
# Preview in browser
open src/main/resources/templates/email/notification.html

# Test with live data via MailHog
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN" \
  -d @test-notification.json
```

---

## Advanced Configuration

### Quiet Hours

**Enable in `application.yml`:**
```yaml
notification:
  email:
    quiet-hours:
      enabled: true
      start: 22  # 10 PM
      end: 7     # 7 AM
      timezone: Asia/Ho_Chi_Minh
```

### Digest Mode

**Configure batch sending:**
```yaml
notification:
  email:
    digest:
      enabled: true
      schedule: "0 0 8 * * ?"  # 8 AM daily
      max-age-hours: 24
```

### Email Retry Policy

**Configure in code:**
```java
@Configuration
public class EmailConfig {
    
    @Bean
    public RetryTemplate emailRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Exponential backoff: 1s, 2s, 4s, 8s
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMaxInterval(10000);
        backOffPolicy.setMultiplier(2);
        
        // Retry 4 times
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(4);
        
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }
}
```

---

## Monitoring

### Email Delivery Metrics

**Track in `EmailNotificationService`:**
```java
@Slf4j
@Service
public class EmailNotificationService {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public void sendEmail(Notification notification) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            mailSender.send(message);
            
            meterRegistry.counter("email.sent.success",
                "type", notification.getType().name(),
                "priority", notification.getPriority().name()
            ).increment();
            
        } catch (Exception e) {
            meterRegistry.counter("email.sent.failure",
                "type", notification.getType().name(),
                "error", e.getClass().getSimpleName()
            ).increment();
            
            throw e;
        } finally {
            sample.stop(Timer.builder("email.send.duration")
                .tags("type", notification.getType().name())
                .register(meterRegistry));
        }
    }
}
```

### Prometheus Metrics

**Expose via `/actuator/prometheus`:**
```
# Email sent successfully
email_sent_success_total{type="TASK_ASSIGNED",priority="HIGH"} 1250

# Email send failures
email_sent_failure_total{type="TASK_ASSIGNED",error="AuthenticationFailedException"} 5

# Email send duration (seconds)
email_send_duration_seconds{type="TASK_ASSIGNED",quantile="0.5"} 0.125
email_send_duration_seconds{type="TASK_ASSIGNED",quantile="0.95"} 0.450
```

### Grafana Dashboard

**Email Performance Dashboard:**
- Email send rate (emails/minute)
- Success/failure ratio
- Average send duration
- Errors by type
- Queue depth

---

## Security Best Practices

1. **Never commit credentials:**
   ```gitignore
   # .gitignore
   application-prod.yml
   .env
   *.credentials
   ```

2. **Use environment variables:**
   ```bash
   export MAIL_PASSWORD=$(vault read -field=password secret/email)
   ```

3. **Rotate credentials regularly:**
   - Every 90 days minimum
   - After team member leaves
   - After security incident

4. **Limit permissions:**
   - Use send-only credentials
   - No access to inbox
   - Minimal scope API keys

5. **Monitor for abuse:**
   - Track send rates
   - Alert on anomalies
   - Log all email sends
   - Implement rate limiting

---

## Checklist

### Development Setup
- [ ] MailHog running: `docker-compose up -d mailhog`
- [ ] MailHog UI accessible: http://localhost:8025
- [ ] Application configured for MailHog
- [ ] Test email sent successfully
- [ ] Template rendering verified

### Production Setup
- [ ] Email provider account created
- [ ] Domain verified (if required)
- [ ] SPF/DKIM/DMARC configured
- [ ] SMTP credentials generated
- [ ] Environment variables set
- [ ] Application deployed with prod config
- [ ] Test email sent successfully
- [ ] Monitoring configured
- [ ] Alerts configured

### Testing
- [ ] SMTP connection test passed
- [ ] Template rendering test passed
- [ ] Notification email test passed
- [ ] Digest email test passed
- [ ] Quiet hours test passed
- [ ] Rate limiting test passed

---

## References

- [Spring Boot Mail Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [MailHog Documentation](https://github.com/mailhog/MailHog)
- [Gmail SMTP Guide](https://support.google.com/mail/answer/7126229)
- [SendGrid SMTP Guide](https://docs.sendgrid.com/for-developers/sending-email/integrating-with-the-smtp-api)
- [AWS SES Documentation](https://docs.aws.amazon.com/ses/)

---

## Support

For issues or questions:
1. Check [Troubleshooting](#troubleshooting) section
2. Review application logs: `logs/spring.log`
3. Check MailHog UI for captured emails
4. Verify environment variables are set correctly
5. Test SMTP connection manually

---

**Last Updated:** November 23, 2025  
**Maintainer:** Development Team  
**Next Review:** December 23, 2025
