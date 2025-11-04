/**
 * ✅ PHASE 1: Error Handler Utility
 * Standardizes error handling across frontend application
 * Maps backend error codes to user-friendly messages
 */

export interface ApiError {
  status: number
  message: string
  errorCode?: string
  data?: any
  timestamp?: string
}

export interface UserFriendlyError {
  title: string
  message: string
  type: 'validation' | 'business' | 'server' | 'network' | 'auth'
  details?: Record<string, string>
  canRetry: boolean
  action?: {
    label: string
    handler: () => void
  }
}

export class ErrorHandler {
  /**
   * Map backend error codes to user-friendly messages
   */
  private static readonly ERROR_MESSAGES: Record<string, UserFriendlyError> = {
    // Validation Errors (4xx)
    VALIDATION_ERROR: {
      title: 'Validation Error',
      message: 'Please check your input and try again',
      type: 'validation',
      canRetry: true,
    },
    DUPLICATE_RESOURCE: {
      title: 'Duplicate Entry',
      message: 'This record already exists. Please use a different value.',
      type: 'validation',
      canRetry: true,
    },
    RESOURCE_NOT_FOUND: {
      title: 'Not Found',
      message: 'The requested resource could not be found',
      type: 'business',
      canRetry: false,
    },
    INVALID_STATE: {
      title: 'Invalid Operation',
      message: 'This operation is not allowed in the current state',
      type: 'business',
      canRetry: false,
    },
    TENANT_VIOLATION: {
      title: 'Access Denied',
      message:
        'You do not have permission to access this resource from another organization',
      type: 'auth',
      canRetry: false,
    },

    // Business Logic Errors (4xx)
    INVALID_STATE_TRANSITION: {
      title: 'Invalid Status Change',
      message: 'Cannot change status from current state',
      type: 'business',
      canRetry: false,
    },
    OPTIMISTIC_LOCK_ERROR: {
      title: 'Conflict Detected',
      message:
        'This record has been modified by another user. Please refresh and try again.',
      type: 'business',
      canRetry: true,
    },

    // Server Errors (5xx)
    INTERNAL_ERROR: {
      title: 'Server Error',
      message: 'An unexpected error occurred. Please try again later.',
      type: 'server',
      canRetry: true,
    },
    TRANSACTION_ERROR: {
      title: 'Transaction Failed',
      message: 'The operation could not be completed. Please try again.',
      type: 'server',
      canRetry: true,
    },
  }

  /**
   * Convert ApiError to UserFriendlyError
   */
  static handle(error: ApiError): UserFriendlyError {
    // Network errors (no response from server)
    if (!error.status) {
      return {
        title: 'Network Error',
        message:
          'Unable to connect to the server. Please check your internet connection.',
        type: 'network',
        canRetry: true,
      }
    }

    // Use mapped error message if available
    if (error.errorCode && this.ERROR_MESSAGES[error.errorCode]) {
      const mapped = this.ERROR_MESSAGES[error.errorCode]
      return {
        ...mapped,
        details: error.data, // Include field-level errors if present
      }
    }

    // Fallback based on HTTP status
    if (error.status >= 500) {
      return {
        title: 'Server Error',
        message:
          error.message || 'An unexpected server error occurred. Please try again later.',
        type: 'server',
        canRetry: true,
      }
    }

    if (error.status === 401) {
      return {
        title: 'Authentication Required',
        message: 'Please log in to continue',
        type: 'auth',
        canRetry: false,
        action: {
          label: 'Log In',
          handler: () => {
            window.location.href = '/login'
          },
        },
      }
    }

    if (error.status === 403) {
      return {
        title: 'Access Denied',
        message: 'You do not have permission to perform this action',
        type: 'auth',
        canRetry: false,
      }
    }

    if (error.status === 404) {
      return {
        title: 'Not Found',
        message: error.message || 'The requested resource could not be found',
        type: 'business',
        canRetry: false,
      }
    }

    if (error.status === 409) {
      return {
        title: 'Conflict',
        message:
          error.message ||
          'This operation conflicts with existing data. Please refresh and try again.',
        type: 'business',
        canRetry: true,
      }
    }

    // Generic 4xx error
    if (error.status >= 400 && error.status < 500) {
      return {
        title: 'Request Error',
        message: error.message || 'Invalid request. Please check your input.',
        type: 'validation',
        canRetry: true,
        details: error.data,
      }
    }

    // Unknown error
    return {
      title: 'Error',
      message: error.message || 'An unexpected error occurred',
      type: 'server',
      canRetry: true,
    }
  }

  /**
   * Show toast notification for error
   * Uses sonner toast library
   */
  static toast(error: ApiError) {
    const friendly = this.handle(error)

    // Import dynamically to avoid circular dependency
    import('sonner').then(({ toast }) => {
      toast(friendly.title, {
        description: friendly.message,
        duration: friendly.canRetry ? 5000 : 7000,
      })

      // Log to console for debugging
      if (friendly.type === 'server') {
        console.error('Server error:', error)
      }
    })
  }

  /**
   * Get field-level validation errors
   * Returns Map of field name -> error message
   */
  static getFieldErrors(error: ApiError): Record<string, string> {
    if (error.data && typeof error.data === 'object') {
      return error.data as Record<string, string>
    }
    return {}
  }

  /**
   * Check if error is retryable
   */
  static canRetry(error: ApiError): boolean {
    const friendly = this.handle(error)
    return friendly.canRetry
  }

  /**
   * Get suggested action for error
   */
  static getAction(error: ApiError) {
    const friendly = this.handle(error)
    return friendly.action
  }

  /**
   * Format error for logging to external service (e.g., Sentry)
   */
  static formatForLogging(error: ApiError): object {
    return {
      status: error.status,
      code: error.errorCode,
      message: error.message,
      data: error.data,
      timestamp: error.timestamp || new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
    }
  }
}

/**
 * React hook for error handling
 * Usage: const { handleError } = useErrorHandler()
 */
export function useErrorHandler() {
  const handleError = (error: ApiError) => {
    // Show toast
    ErrorHandler.toast(error)

    // Log to external service if server error
    if (error.status >= 500) {
      // TODO: Send to Sentry/DataDog/etc
      console.error('Server error logged:', ErrorHandler.formatForLogging(error))
    }

    return ErrorHandler.handle(error)
  }

  return { handleError, getFieldErrors: ErrorHandler.getFieldErrors }
}
