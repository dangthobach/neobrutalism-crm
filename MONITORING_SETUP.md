# 🔍 Monitoring & Error Tracking Setup

## ✅ Implementation Status

All monitoring infrastructure is in place. Follow steps below to activate.

---

## 📦 1. Install Sentry

```bash
npm install @sentry/nextjs
```

---

## 🔑 2. Configure Environment Variables

### `.env.local` (Development)
```bash
NEXT_PUBLIC_SENTRY_DSN=https://your-dsn@sentry.io/project-id
NEXT_PUBLIC_APP_VERSION=1.0.0
NODE_ENV=development
```

### `.env.production` (Production)
```bash
NEXT_PUBLIC_SENTRY_DSN=https://your-dsn@sentry.io/project-id
NEXT_PUBLIC_APP_VERSION=1.0.0
NODE_ENV=production
```

---

## 🚀 3. Activate Sentry Integration

### Step 1: Uncomment Sentry imports

File: `src/lib/monitoring/sentry.ts`

```typescript
// BEFORE (commented):
// import * as Sentry from '@sentry/nextjs'

// AFTER (uncommented):
import * as Sentry from '@sentry/nextjs'
```

### Step 2: Uncomment initialization code

In the same file, uncomment all the `Sentry.init()` and function implementations.

### Step 3: Initialize in your app

File: `src/app/layout.tsx`

```typescript
import { initSentry } from '@/lib/monitoring/sentry'

// Initialize on app startup
if (typeof window !== 'undefined') {
  initSentry()
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  )
}
```

---

## 🎯 4. What Gets Tracked

### ✅ Automatically Tracked:

- **Server Errors (5xx)**: API calls that return 500+ status codes
- **Network Errors**: Failed requests, timeouts
- **Unhandled Exceptions**: JavaScript runtime errors
- **Performance Metrics**: API response times
- **User Context**: Current user ID, tenant ID
- **Breadcrumbs**: Navigation, API calls, user actions

### ✅ Features Included:

1. **Error Tracking**
   - Full stack traces
   - User context (ID, tenant)
   - API endpoint context
   - Environment info

2. **Performance Monitoring**
   - API response times
   - Page load times
   - Transaction tracking

3. **Session Replay** (Optional)
   - Visual reproduction of user sessions
   - Privacy-safe (text/media masked)
   - 10% sample rate (configurable)

4. **Privacy Protection**
   - Authorization headers stripped
   - Email addresses removed
   - IP addresses masked
   - Sensitive data filtered

---

## 🧪 5. Testing

### Test Error Tracking:

```typescript
import { captureApiError } from '@/lib/monitoring/sentry'

// Trigger test error
captureApiError(new Error('Test error'), {
  endpoint: '/api/test',
  method: 'GET',
  statusCode: 500,
})
```

### Test Performance Tracking:

```typescript
import { capturePerformance } from '@/lib/monitoring/sentry'

capturePerformance('api_call', 1250, {
  endpoint: '/api/branches',
  method: 'GET',
})
```

---

## 📊 6. Sentry Dashboard Setup

### Create Project:
1. Go to [sentry.io](https://sentry.io)
2. Create new project → Select "Next.js"
3. Copy your DSN

### Recommended Alerts:
- **Server Errors**: Alert when 5xx errors > 10/hour
- **Performance**: Alert when P95 latency > 3s
- **Error Rate**: Alert when error rate > 5%

### Sample Filters:
```
environment:production
level:error
http.status_code:[500 TO 599]
```

---

## 🔧 7. Advanced Configuration

### Custom Performance Tracking:

```typescript
import { capturePerformance } from '@/lib/monitoring/sentry'

export async function trackApiCall<T>(
  fn: () => Promise<T>,
  name: string
): Promise<T> {
  const start = performance.now()

  try {
    return await fn()
  } finally {
    const duration = performance.now() - start
    capturePerformance(name, duration)
  }
}

// Usage:
const branches = await trackApiCall(
  () => branchApi.getAll(),
  'fetch_branches'
)
```

### Custom User Context:

```typescript
import { setUserContext, clearUserContext } from '@/lib/monitoring/sentry'

// On login:
setUserContext({
  id: user.id,
  username: user.username,
  tenantId: user.tenantId,
})

// On logout:
clearUserContext()
```

---

## 🚨 8. Error Handling Best Practices

### ✅ DO:
```typescript
try {
  await api.create(data)
} catch (error) {
  // Let Sentry auto-capture
  throw error
}
```

### ❌ DON'T:
```typescript
try {
  await api.create(data)
} catch (error) {
  console.log(error) // Swallowed error - Sentry won't see it
}
```

### ✅ DO (Custom Context):
```typescript
import { captureApiError } from '@/lib/monitoring/sentry'

try {
  await api.create(data)
} catch (error) {
  captureApiError(error as Error, {
    endpoint: '/api/branches',
    method: 'POST',
    userId: currentUser.id,
    customData: { branchCode: data.code },
  })
  throw error
}
```

---

## 📈 9. Monitoring Checklist

### Before Production Deploy:

- [ ] Install `@sentry/nextjs`
- [ ] Set `NEXT_PUBLIC_SENTRY_DSN` in production
- [ ] Uncomment Sentry code in `sentry.ts`
- [ ] Initialize Sentry in app layout
- [ ] Test error capture in staging
- [ ] Configure Sentry alerts
- [ ] Set up Slack/email notifications
- [ ] Verify sensitive data filtering

### Post-Deploy Verification:

- [ ] Check Sentry dashboard for incoming events
- [ ] Verify user context is set correctly
- [ ] Confirm performance metrics are tracked
- [ ] Test alert notifications
- [ ] Review sample errors for sensitive data leaks

---

## 💰 10. Cost Optimization

### Free Tier Limits (as of 2024):
- 5,000 errors/month
- 10,000 transactions/month
- 100 replays/month

### Optimize Costs:
```typescript
// Adjust sample rates in sentry.ts:
tracesSampleRate: 0.1,           // 10% in production
replaysSessionSampleRate: 0.05,   // 5% of sessions
replaysOnErrorSampleRate: 1.0,    // 100% on errors
```

---

## 🆘 11. Troubleshooting

### Errors not showing in Sentry?

1. Check DSN is set: `echo $NEXT_PUBLIC_SENTRY_DSN`
2. Verify Sentry code is uncommented
3. Check browser console for Sentry init message
4. Test with manual error: `throw new Error('test')`

### Performance metrics not tracked?

1. Check `tracesSampleRate` > 0
2. Verify BrowserTracing integration is enabled
3. Check Network tab for Sentry API calls

---

## 📚 12. Resources

- [Sentry Next.js Docs](https://docs.sentry.io/platforms/javascript/guides/nextjs/)
- [Performance Monitoring](https://docs.sentry.io/product/performance/)
- [Session Replay](https://docs.sentry.io/product/session-replay/)
- [Source Maps](https://docs.sentry.io/platforms/javascript/sourcemaps/)

---

## ✅ Summary

Your monitoring infrastructure is **100% ready**. Just:
1. Install `@sentry/nextjs`
2. Set environment variables
3. Uncomment code in `sentry.ts`
4. Deploy! 🚀

All error tracking, performance monitoring, and user context will work automatically.
