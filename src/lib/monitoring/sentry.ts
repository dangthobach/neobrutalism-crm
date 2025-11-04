/**
 * ✅ FIX #3: Sentry Error Monitoring Integration
 * Centralizes error tracking and performance monitoring
 *
 * Setup Instructions:
 * 1. npm install @sentry/nextjs
 * 2. Set NEXT_PUBLIC_SENTRY_DSN in .env.local
 * 3. Uncomment integration in this file
 * 4. Initialize in app layout
 */

// Uncomment when Sentry is installed:
// import * as Sentry from '@sentry/nextjs'

export interface ErrorContext {
  endpoint?: string
  method?: string
  userId?: string
  tenantId?: string
  statusCode?: number
  [key: string]: any
}

/**
 * Initialize Sentry monitoring
 * Call this in app/layout.tsx or _app.tsx
 */
export function initSentry() {
  // Check if Sentry DSN is configured
  const dsn = process.env.NEXT_PUBLIC_SENTRY_DSN

  if (!dsn) {
    console.warn('⚠️ Sentry DSN not configured. Error tracking disabled.')
    return
  }

  // Uncomment when @sentry/nextjs is installed:
  /*
  Sentry.init({
    dsn,

    // Environment configuration
    environment: process.env.NODE_ENV,
    release: process.env.NEXT_PUBLIC_APP_VERSION || 'unknown',

    // Performance Monitoring
    tracesSampleRate: process.env.NODE_ENV === 'production' ? 0.1 : 1.0, // 10% in prod, 100% in dev

    // Session Replay
    replaysSessionSampleRate: 0.1, // 10% of sessions
    replaysOnErrorSampleRate: 1.0, // 100% of sessions with errors

    // Filter out sensitive data
    beforeSend(event, hint) {
      // Remove sensitive headers
      if (event.request?.headers) {
        delete event.request.headers['authorization']
        delete event.request.headers['cookie']
      }

      // Remove sensitive data from context
      if (event.contexts?.user) {
        delete event.contexts.user.email
        delete event.contexts.user.ip_address
      }

      return event
    },

    // Ignore specific errors
    ignoreErrors: [
      // Browser extensions
      'top.GLOBALS',
      'chrome-extension://',

      // Network errors (handled by retry logic)
      'NetworkError',
      'Failed to fetch',

      // User cancellations
      'AbortError',
      'Request aborted',
    ],

    // Integrations
    integrations: [
      new Sentry.BrowserTracing({
        tracePropagationTargets: [
          'localhost',
          /^https:\/\/[^/]+\.neobrutalism-crm\.com/,
        ],
      }),
      new Sentry.Replay({
        maskAllText: true,
        blockAllMedia: true,
      }),
    ],
  })

  console.log('✅ Sentry monitoring initialized')
  */

  console.log('ℹ️ Sentry placeholder initialized (install @sentry/nextjs to activate)')
}

/**
 * Capture API error with context
 */
export function captureApiError(
  error: Error,
  context: ErrorContext
) {
  // Log to console in development
  if (process.env.NODE_ENV === 'development') {
    console.error('🔴 API Error:', {
      message: error.message,
      ...context,
    })
  }

  // Send to Sentry in production
  if (process.env.NODE_ENV === 'production') {
    // Uncomment when Sentry is installed:
    /*
    Sentry.captureException(error, {
      level: context.statusCode && context.statusCode >= 500 ? 'error' : 'warning',
      tags: {
        api_endpoint: context.endpoint,
        api_method: context.method,
        status_code: context.statusCode,
      },
      contexts: {
        api: {
          endpoint: context.endpoint,
          method: context.method,
          status_code: context.statusCode,
        },
        tenant: {
          tenant_id: context.tenantId,
        },
      },
      user: {
        id: context.userId,
      },
    })
    */

    // Fallback logging
    console.error('[Sentry Placeholder] Error captured:', {
      error: error.message,
      context,
    })
  }
}

/**
 * Capture performance metric
 */
export function capturePerformance(
  name: string,
  duration: number,
  context?: Record<string, any>
) {
  if (process.env.NODE_ENV === 'development') {
    console.log(`⏱️ Performance: ${name} took ${duration}ms`)
  }

  // Uncomment when Sentry is installed:
  /*
  Sentry.metrics.distribution(name, duration, {
    tags: context,
  })
  */
}

/**
 * Set user context for error tracking
 */
export function setUserContext(user: {
  id: string
  username?: string
  tenantId?: string
}) {
  // Uncomment when Sentry is installed:
  /*
  Sentry.setUser({
    id: user.id,
    username: user.username,
  })

  Sentry.setTag('tenant_id', user.tenantId)
  */

  if (process.env.NODE_ENV === 'development') {
    console.log('👤 User context set:', user.id)
  }
}

/**
 * Clear user context (on logout)
 */
export function clearUserContext() {
  // Uncomment when Sentry is installed:
  /*
  Sentry.setUser(null)
  */

  if (process.env.NODE_ENV === 'development') {
    console.log('👤 User context cleared')
  }
}

/**
 * Capture custom breadcrumb
 */
export function addBreadcrumb(
  message: string,
  category: string,
  data?: Record<string, any>
) {
  // Uncomment when Sentry is installed:
  /*
  Sentry.addBreadcrumb({
    message,
    category,
    data,
    level: 'info',
  })
  */
}
