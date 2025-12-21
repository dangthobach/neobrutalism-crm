# Security Guide

## 🔒 Production Security Checklist

This document provides a comprehensive security checklist for deploying Neobrutalism CRM to production.

---

## ✅ Pre-Deployment Checklist

### 1. Environment Variables & Secrets

**Critical Configuration (MUST DO)**

- [ ] Generate strong JWT secret: `openssl rand -base64 32`
- [ ] Set `JWT_SECRET` environment variable (min 32 characters)
- [ ] Configure strong database password (min 16 characters, alphanumeric + special chars)
- [ ] Configure Redis password (if using Redis authentication)
- [ ] Update MinIO access key and secret key
- [ ] Configure production SMTP credentials
- [ ] Update CORS_ALLOWED_ORIGINS to production URLs only

**Verification:**
```bash
# Run application with prod profile - it will validate security config
java -jar -Dspring.profiles.active=prod target/*.jar

# Expected output:
# ✅ JWT Secret: Strong (44 characters)
# ✅ Database Password: Configured
# ✅ CORS Origins: Configured for production
```

---

### 2. Database Security

**Configuration**

- [ ] Use SSL/TLS for database connections
- [ ] Enable database encryption at rest
- [ ] Use dedicated database user with minimal privileges
- [ ] Disable public database access
- [ ] Enable database audit logging
- [ ] Configure automated backups

**Connection String (Production):**
```properties
DB_HOST=your-db-host.com
DB_PORT=5432
DB_NAME=neobrutalism_crm_prod
DB_USERNAME=crm_prod_user
DB_PASSWORD=<strong-password>

# Enable SSL
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=require
```

**Default Admin Password:**
```sql
-- CRITICAL: Change default admin password after first deployment!
-- Default credentials:
-- Username: admin
-- Password: admin123

-- Update password via API or database:
UPDATE users
SET password_hash = '$2a$12$<bcrypt-hash-of-new-password>'
WHERE username = 'admin';
```

---

### 3. HTTPS & TLS Configuration

**Enable HSTS (HTTP Strict Transport Security)**

```bash
# Set environment variables
SECURITY_HSTS_ENABLED=true
SECURITY_HSTS_MAX_AGE=31536000
SECURITY_HSTS_INCLUDE_SUBDOMAINS=true
```

**SSL Certificate:**
- Use Let's Encrypt (free) or commercial CA
- Configure reverse proxy (Nginx/Apache) with SSL
- Enable HTTP/2
- Use TLS 1.2+ only (disable TLS 1.0/1.1)

**Nginx Example:**
```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

---

### 4. CORS Configuration

**Production CORS:**
```properties
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

**Important:**
- Remove all `localhost` origins
- Use exact domain matches (no wildcards)
- Enable credentials only if needed
- Limit exposed headers

---

### 5. Rate Limiting

**Enable Rate Limiting:**
```properties
RATE_LIMIT_ENABLED=true
```

**Default Limits:**
- Admin: 1000 requests/minute
- Authenticated users: 100 requests/minute
- Public/unauthenticated: 20 requests/minute

**Adjust for your needs:**
```yaml
rate-limit:
  enabled: true
  admin-limit: 1000
  user-limit: 100
  public-limit: 20
```

---

### 6. Input Validation & Sanitization

**Use InputSanitizer for all user input:**

```java
@Autowired
private InputSanitizer inputSanitizer;

public void saveContent(ContentRequest request) {
    // Sanitize HTML content
    String safeContent = inputSanitizer.sanitizeHtml(request.getContent());

    // Sanitize file names
    String safeFileName = inputSanitizer.sanitizeFileName(request.getFileName());

    // Validate URLs
    String safeUrl = inputSanitizer.sanitizeUrl(request.getWebsiteUrl());

    // Escape for display
    String safeTitle = inputSanitizer.escapeHtml(request.getTitle());
}
```

**Always validate:**
- HTML content (remove XSS)
- File names (prevent path traversal)
- URLs (prevent open redirect)
- Email addresses (prevent header injection)
- SQL inputs (use parameterized queries)

---

### 7. Authentication & Authorization

**JWT Configuration:**
```properties
JWT_ACCESS_TOKEN_VALIDITY=3600000    # 1 hour
JWT_REFRESH_TOKEN_VALIDITY=604800000 # 7 days
```

**Password Policy:**
- Minimum 8 characters
- BCrypt with cost factor 12
- Require password change on first login
- Implement password expiry (optional)
- Lock account after 5 failed login attempts

**Multi-Factor Authentication (Future):**
- TOTP (Time-based One-Time Password)
- SMS verification
- Email verification

---

### 8. Logging & Monitoring

**Enable Audit Logging:**
```properties
app.audit.enabled=true
audit.scheduled.enabled=true
```

**Monitor for:**
- Failed login attempts
- Permission changes
- Suspicious activity
- Critical security events

**Log Retention:**
- Audit logs: 1 year (default)
- Application logs: 30 days
- Access logs: 90 days

**Alerting:**
Configure alerts for:
- Multiple failed login attempts (5+ in 1 hour)
- Permission changes
- Unusual data access patterns
- Rate limit violations

---

### 9. Dependency Security

**Scan for vulnerabilities:**

```bash
# Maven dependency check
mvn org.owasp:dependency-check-maven:check

# Snyk scan (requires account)
snyk test

# OWASP ZAP (dynamic scanning)
# Use ZAP proxy to scan running application
```

**Keep dependencies updated:**
```bash
# Check for updates
mvn versions:display-dependency-updates

# Update Spring Boot
# Update all dependencies regularly
```

---

### 10. Container Security (Docker)

**Dockerfile best practices:**
- Use specific version tags (not `latest`)
- Run as non-root user
- Multi-stage builds
- Minimal base image (Alpine/Distroless)
- Scan images for vulnerabilities

**Example secure Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

WORKDIR /app
COPY --chown=appuser:appuser target/*.jar app.jar

# Run as non-root
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Scan images:**
```bash
# Trivy scan
trivy image your-image:tag

# Docker Scout (Docker Desktop)
docker scout cves your-image:tag
```

---

### 11. Network Security

**Firewall Rules:**
- Allow HTTPS (443) from internet
- Allow HTTP (80) only for redirect
- Restrict database port (5432) to app servers only
- Restrict Redis port (6379) to app servers only
- Block all other ports

**AWS Security Groups Example:**
```
Inbound Rules:
- Port 443 (HTTPS) from 0.0.0.0/0
- Port 80 (HTTP) from 0.0.0.0/0 (redirect to HTTPS)
- Port 8080 (App) from Load Balancer only
- Port 5432 (PostgreSQL) from App Security Group only
- Port 6379 (Redis) from App Security Group only

Outbound Rules:
- All traffic allowed (or restrict to specific IPs)
```

---

### 12. Backup & Disaster Recovery

**Database Backups:**
- Automated daily backups
- Backup retention: 30 days
- Test restore procedure monthly
- Encrypt backups
- Store in different region/zone

**Application Backup:**
- Configuration files
- MinIO object storage
- Redis data (if critical)

---

### 13. Compliance

**GDPR Compliance:**
- [ ] Data encryption at rest and in transit
- [ ] Right to be forgotten (user deletion)
- [ ] Data export functionality
- [ ] Privacy policy
- [ ] Terms of service
- [ ] Cookie consent

**Audit Trail:**
- [ ] Track all data access
- [ ] Log all modifications
- [ ] Retain logs per compliance requirements

---

## 🛡️ Security Headers

The application automatically sets the following security headers:

```
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; ...
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload (if enabled)
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(), camera=(), ...
Cache-Control: no-store, no-cache, must-revalidate, private
```

---

## 🚨 Incident Response

**Security Incident Procedure:**

1. **Detect**: Monitor logs and alerts
2. **Contain**: Disable affected accounts/endpoints
3. **Investigate**: Analyze logs and audit trail
4. **Remediate**: Fix vulnerability, patch system
5. **Recover**: Restore services
6. **Report**: Document incident and lessons learned

**Emergency Contacts:**
- Security team: security@yourcompany.com
- DevOps on-call: +1-xxx-xxx-xxxx

---

## 📞 Security Support

For security issues or questions:

- **Email**: security@yourcompany.com
- **Bug Bounty**: (if applicable)
- **Responsible Disclosure**: Follow responsible disclosure practices

**Reporting Security Vulnerabilities:**

Please do NOT create public GitHub issues for security vulnerabilities.

Email security@yourcompany.com with:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (optional)

---

## 📚 Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)
- [CWE Top 25](https://cwe.mitre.org/top25/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)

---

**Last Updated**: December 2025
**Version**: 1.0.0
