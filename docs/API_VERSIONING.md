# API Versioning Guide

## Overview

Neobrutalism CRM uses **URL-based versioning** for its REST API. This ensures clear, explicit versioning and allows multiple API versions to coexist during migration periods.

---

## 🎯 Versioning Strategy

### URL Structure

```
https://api.yourdomain.com/api/{version}/{resource}
```

**Examples:**
```
GET https://api.yourdomain.com/api/v1/users
GET https://api.yourdomain.com/api/v1/customers
POST https://api.yourdomain.com/api/v1/tasks
```

### Current Versions

| Version | Status | Release Date | Sunset Date | Documentation |
|---------|--------|--------------|-------------|---------------|
| **v1** | ✅ Current | 2025-01-01 | N/A | [API v1 Docs](#) |

---

## 📋 Versioning Rules

### When to Create a New Version

Create a new major version when making **breaking changes**:

❌ **Breaking Changes** (require new version):
- Removing an endpoint
- Renaming a field in request/response
- Changing field data type
- Changing endpoint URL structure
- Removing required authentication
- Changing error response format

✅ **Non-Breaking Changes** (add to current version):
- Adding new endpoints
- Adding optional fields to requests
- Adding new fields to responses
- Adding new query parameters (optional)
- Bug fixes
- Performance improvements

### Backward Compatibility

Within a major version (e.g., v1.x):
- All changes MUST be backward compatible
- New optional fields can be added
- Existing fields cannot be removed or renamed
- Field types cannot change

---

## 🔧 Implementation Details

### Request Headers

**Required Headers:**
```http
Content-Type: application/json
Authorization: Bearer <jwt-token>
X-Tenant-ID: <tenant-id>
```

**Optional Headers:**
```http
Accept: application/json
X-Request-ID: <unique-request-id>
```

### Response Headers

**Standard Response Headers:**
```http
X-API-Version: v1
Content-Type: application/json
```

**Deprecation Warning Headers** (for deprecated versions):
```http
X-API-Deprecated-Warning: API version v0 is deprecated and will be removed on 2025-06-01
X-API-Sunset-Date: 2025-06-01
X-API-Migration-Guide: https://docs.yourdomain.com/migration/v0-to-v1
```

---

## 🚨 Deprecation Policy

### Timeline

1. **Announcement** (T+0): New version released, old version marked as deprecated
2. **Deprecation Period** (T+0 to T+12 months): Both versions supported
3. **Sunset Warning** (T+9 months): Intensify migration communications
4. **Sunset** (T+12 months): Old version removed, returns 410 Gone

### Deprecation Process

**Step 1: Announcement**
- Release notes published
- Email notifications to API consumers
- Documentation updated
- Deprecation headers added to responses

**Step 2: Migration Support**
- Migration guide published
- Sample code provided
- Support team available for questions
- Monitoring of old version usage

**Step 3: Sunset**
- Old version disabled
- Returns 410 Gone status
- Redirect to migration guide

---

## 📦 Response Format

### Success Response

```json
{
  "success": true,
  "message": "Resource retrieved successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Example Resource"
  },
  "timestamp": "2025-12-11T10:00:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "The requested resource was not found",
    "details": "User with ID 123 does not exist",
    "timestamp": "2025-12-11T10:00:00Z",
    "path": "/api/v1/users/123",
    "traceId": "abc123-def456"
  }
}
```

### Pagination Response

```json
{
  "success": true,
  "message": "Resources retrieved successfully",
  "data": {
    "content": [...],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 100,
      "totalPages": 5
    }
  }
}
```

---

## 🔄 Migration Between Versions

### Example: Migrating from v0 to v1

#### v0 Endpoint (Deprecated)
```http
GET /api/customers?page=1&size=20
```

**Response:**
```json
{
  "customers": [...],
  "total": 100
}
```

#### v1 Endpoint (Current)
```http
GET /api/v1/customers?page=0&size=20
```

**Response:**
```json
{
  "success": true,
  "message": "Customers retrieved successfully",
  "data": {
    "content": [...],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 100,
      "totalPages": 5
    }
  }
}
```

**Changes:**
- ✅ Explicit `/v1/` in URL
- ✅ Standardized response format with `success`, `message`, `data`
- ✅ Enhanced pagination metadata
- ✅ Page numbering starts at 0 (was 1 in v0)

---

## 🛠️ Client Implementation

### JavaScript/TypeScript Example

```typescript
const API_BASE_URL = 'https://api.yourdomain.com/api/v1';

class ApiClient {
  private baseUrl: string;
  private token: string;

  constructor(token: string) {
    this.baseUrl = API_BASE_URL;
    this.token = token;
  }

  async request<T>(
    endpoint: string,
    method: string = 'GET',
    body?: any
  ): Promise<T> {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.token}`,
        'X-Tenant-ID': 'default',
      },
      body: body ? JSON.stringify(body) : undefined,
    });

    // Check for deprecation warnings
    const deprecationWarning = response.headers.get('X-API-Deprecated-Warning');
    if (deprecationWarning) {
      console.warn('API Deprecation Warning:', deprecationWarning);
    }

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error?.message || 'API request failed');
    }

    return data.data;
  }

  // Example usage
  async getUsers(page: number = 0, size: number = 20) {
    return this.request(`/users?page=${page}&size=${size}`);
  }
}
```

### Java Example

```java
public class ApiClient {
    private static final String API_BASE_URL = "https://api.yourdomain.com/api/v1";
    private final RestTemplate restTemplate;
    private final String token;

    public <T> T request(String endpoint, HttpMethod method, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-Tenant-ID", "default");

        HttpEntity<?> entity = new HttpEntity<>(body, headers);

        ResponseEntity<ApiResponse<T>> response = restTemplate.exchange(
            API_BASE_URL + endpoint,
            method,
            entity,
            new ParameterizedTypeReference<ApiResponse<T>>() {}
        );

        // Check for deprecation warnings
        String deprecationWarning = response.getHeaders().getFirst("X-API-Deprecated-Warning");
        if (deprecationWarning != null) {
            log.warn("API Deprecation Warning: {}", deprecationWarning);
        }

        return response.getBody().getData();
    }
}
```

---

## 📊 Version Usage Monitoring

### Metrics

Track the following metrics for each API version:

- **Request Count**: Total requests per version
- **Error Rate**: 4xx/5xx errors per version
- **Response Time**: P50, P95, P99 latency per version
- **Unique Clients**: Number of distinct API consumers
- **Endpoint Usage**: Most/least used endpoints

### Dashboards

Monitor version adoption in Grafana:
```
sum(rate(http_requests_total{version="v1"}[5m])) by (version)
```

---

## 🔐 Security Considerations

### Version-Specific Security

- Each version maintains its own security policies
- Authentication/authorization rules can differ between versions
- Rate limits may vary by version
- Deprecated versions may have stricter rate limits

### Best Practices

1. **Always use HTTPS** for API requests
2. **Include X-Request-ID** header for request tracing
3. **Handle deprecation warnings** proactively
4. **Implement retry logic** with exponential backoff
5. **Cache responses** where appropriate

---

## 📝 Changelog

### v1.0.0 (2025-01-01)

**Initial stable release**

- Complete CRUD operations for all resources
- JWT authentication
- Role-based access control
- Multi-tenancy support
- Rate limiting
- Comprehensive error handling

---

## 📞 Support

For API questions or issues:

- **Documentation**: https://docs.yourdomain.com/api
- **Email**: api-support@yourcompany.com
- **GitHub Issues**: https://github.com/yourorg/crm/issues
- **Status Page**: https://status.yourdomain.com

---

## 🎓 Best Practices for API Consumers

1. **Always specify version** in URL (don't rely on default)
2. **Monitor deprecation headers** and plan migrations early
3. **Use SDK/client libraries** when available
4. **Implement proper error handling** for all status codes
5. **Respect rate limits** to avoid throttling
6. **Cache responses** where appropriate (check Cache-Control headers)
7. **Use pagination** for list endpoints
8. **Include X-Request-ID** for request tracing and debugging

---

**Last Updated**: December 2025
**Current Version**: v1
**API Status**: Stable ✅
