/**
 * API Client for communicating with backend
 * Handles authentication, error handling, and retries
 * 
 * CRITICAL: This client unwraps the ApiResponse wrapper from backend
 * Backend returns: { success: true, message: "...", data: T }
 * This client returns: T (the actual data)
 */

export interface ApiResponse<T> {
  success: boolean
  message?: string
  code?: string
  data?: T
  timestamp?: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
  first: boolean
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public data?: any
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

/**
 * Type guard to check if response is wrapped in ApiResponse
 */
function isApiResponse<T>(data: any): data is ApiResponse<T> {
  return data && typeof data === 'object' && 'success' in data && 'data' in data
}

class ApiClient {
  private baseUrl: string
  private activeRequests = new Map<string, AbortController>()
  private cache = new Map<string, { data: any; timestamp: number }>()
  private readonly DEFAULT_TIMEOUT = 30000 // 30 seconds
  private readonly CACHE_TTL = 60000 // 1 minute

  constructor(baseUrl?: string) {
    // ⭐ NEW: Use Gateway URL (not direct /api) to leverage OAuth2 session
    this.baseUrl = baseUrl || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
  }

  /**
   * ⭐ DEPRECATED: OAuth2 session-based auth doesn't use access tokens
   * Session cookies are managed automatically by the browser
   * Keeping for backward compatibility during migration
   */
  setAccessToken(_token: string | null) {
    console.warn('[ApiClient] setAccessToken is deprecated - using OAuth2 session cookies instead')
  }

  /**
   * ⭐ DEPRECATED: No refresh token needed with OAuth2
   */
  clearRefreshToken() {
    console.warn('[ApiClient] clearRefreshToken is deprecated - OAuth2 handles token refresh automatically')
  }

  /**
   * ⭐ DEPRECATED: Session cookies replace access tokens
   */
  getAccessToken(): string | null {
    return null
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
    retryCount = 0,
    maxRetries = 3
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`

    // Create abort controller for timeout and cancellation
    const controller = new AbortController()
    const requestId = `${options.method || 'GET'}_${endpoint}`
    this.activeRequests.set(requestId, controller)

    // Set timeout
    const timeoutId = setTimeout(() => {
      controller.abort()
      console.error('⏱️ Request timeout:', { endpoint, timeout: this.DEFAULT_TIMEOUT })
    }, this.DEFAULT_TIMEOUT)

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    }

    if (options.headers) {
      Object.assign(headers, options.headers)
    }

    // ⭐ REMOVED: No Authorization header needed - using OAuth2 session cookies

    // Log request in development
    if (process.env.NODE_ENV === 'development') {
      console.log('🚀 API Request:', {
        method: options.method || 'GET',
        url: endpoint,
        baseUrl: this.baseUrl,
        retryCount,
      })
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers,
        signal: controller.signal,
        // ⭐ CRITICAL: Include credentials (cookies) for OAuth2 session
        credentials: 'include',
      })

      // Parse JSON response
      const data: any = await response.json()

      // Log response in development
      if (process.env.NODE_ENV === 'development') {
        console.log('📥 API Response:', {
          status: response.status,
          ok: response.ok,
          url: endpoint,
          data,
        })
      }

      // Handle HTTP errors
      if (!response.ok) {
        // ⭐ Handle 401 Unauthorized - OAuth2 session expired
        if (response.status === 401 && retryCount === 0) {
          console.error('❌ Session expired (401) - redirecting to OAuth2 login')
          if (typeof window !== 'undefined') {
            // Redirect to Gateway OAuth2 login
            window.location.href = '/login/oauth2/authorization/keycloak'
          }
          throw new ApiError(401, 'UNAUTHORIZED', 'Session expired')
        }

        // Extract error message from ApiResponse wrapper or raw response
        const errorMessage = isApiResponse(data) 
          ? data.message || 'An error occurred'
          : data.message || 'An error occurred'
        
        const errorCode = isApiResponse(data)
          ? data.code || 'UNKNOWN_ERROR'
          : 'UNKNOWN_ERROR'

        throw new ApiError(
          response.status,
          errorCode,
          errorMessage,
          data
        )
      }

      // CRITICAL: Unwrap ApiResponse wrapper from backend
      // Backend returns: { success: true, message: "...", data: T }
      // We return: T (the actual data)
      if (isApiResponse<T>(data)) {
        if (!data.success) {
          throw new ApiError(
            response.status,
            data.code || 'API_ERROR',
            data.message || 'API returned success: false',
            data.data
          )
        }
        // Return the unwrapped data
        return data.data as T
      }

      // If not wrapped, return as-is (for backwards compatibility)
      return data as T
    } catch (error: any) {
      // Handle abort/timeout
      if (error.name === 'AbortError') {
        console.error('⏱️ Request aborted/timeout:', { endpoint })
        const timeoutError = new ApiError(0, 'TIMEOUT', 'Request timeout or cancelled')
        
        // ✅ PHASE 1: Log to monitoring service for server errors
        if (typeof window !== 'undefined' && window.console) {
          console.error('[API Client] Timeout error:', {
            endpoint,
            method: options.method || 'GET',
            timestamp: new Date().toISOString()
          })
        }
        
        throw timeoutError
      }

      if (error instanceof ApiError) {
        console.error('❌ API Error:', {
          status: error.status,
          code: error.code,
          message: error.message,
        })
        
        // ✅ FIX #3: Log server errors to Sentry monitoring
        if (error.status >= 500) {
          if (typeof window !== 'undefined' && window.console) {
            console.error('[API Client] Server error:', {
              status: error.status,
              code: error.code,
              message: error.message,
              endpoint,
              method: options.method || 'GET',
              timestamp: new Date().toISOString(),
              data: error.data
            })

            // ✅ Send to Sentry (when installed)
            import('@/lib/monitoring/sentry').then(({ captureApiError }) => {
              captureApiError(new Error(error.message), {
                endpoint,
                method: options.method || 'GET',
                statusCode: error.status,
              })
            }).catch(() => {
              // Sentry not available, ignore
            })
          }
        }
        
        // Retry logic for server errors (5xx) and network errors
        if (retryCount < maxRetries) {
          const shouldRetry = 
            error.status === 0 || // Network error
            (error.status >= 500 && error.status < 600) // Server error
          
          if (shouldRetry) {
            // Exponential backoff: 1s, 2s, 4s
            const delay = 1000 * Math.pow(2, retryCount)
            console.log(`🔄 Retrying request (${retryCount + 1}/${maxRetries}) after ${delay}ms...`)
            
            await new Promise(resolve => setTimeout(resolve, delay))
            
            // Retry the request
            return this.request<T>(endpoint, options, retryCount + 1, maxRetries)
          }
        }
        
        throw error
      }

      // Network or parsing errors
      console.error('❌ Network Error:', error)
      
      // Retry for network errors
      if (retryCount < maxRetries) {
        const delay = 1000 * Math.pow(2, retryCount)
        console.log(`🔄 Retrying request (${retryCount + 1}/${maxRetries}) after ${delay}ms...`)
        
        await new Promise(resolve => setTimeout(resolve, delay))
        return this.request<T>(endpoint, options, retryCount + 1, maxRetries)
      }
      
      throw new ApiError(
        0,
        'NETWORK_ERROR',
        error instanceof Error ? error.message : 'Network error occurred'
      )
    } finally {
      // Cleanup
      clearTimeout(timeoutId)
      this.activeRequests.delete(requestId)
    }
  }

  /**
   * ⭐ REMOVED: OAuth2 Gateway handles token refresh automatically
   * No manual refresh needed - Spring Security OAuth2 Client manages this
   */

  /**
   * GET request with optional caching
   * @param endpoint - API endpoint
   * @param params - Query parameters
   * @param useCache - Whether to use cache (default: true)
   * @returns Response data
   */
  async get<T>(endpoint: string, params?: Record<string, any>, useCache = true): Promise<T> {
    const queryString = params
      ? '?' + new URLSearchParams(
          Object.entries(params).reduce((acc, [key, value]) => {
            if (value !== undefined && value !== null) {
              acc[key] = String(value)
            }
            return acc
          }, {} as Record<string, string>)
        ).toString()
      : ''

    const fullEndpoint = `${endpoint}${queryString}`
    
    // Check cache
    if (useCache) {
      const cacheKey = `GET_${fullEndpoint}`
      const cached = this.cache.get(cacheKey)
      
      if (cached && Date.now() - cached.timestamp < this.CACHE_TTL) {
        if (process.env.NODE_ENV === 'development') {
          console.log('💾 Cache hit:', { endpoint: fullEndpoint })
        }
        return cached.data
      }
    }

    // Make request
    const data = await this.request<T>(fullEndpoint, {
      method: 'GET',
    })
    
    // Store in cache
    if (useCache) {
      const cacheKey = `GET_${fullEndpoint}`
      this.cache.set(cacheKey, { data, timestamp: Date.now() })
    }
    
    return data
  }

  /**
   * Cancel a specific request
   * @param requestId - Request ID (method_endpoint)
   */
  cancel(requestId: string): void {
    const controller = this.activeRequests.get(requestId)
    if (controller) {
      controller.abort()
      this.activeRequests.delete(requestId)
      console.log('🚫 Request cancelled:', { requestId })
    }
  }

  /**
   * Cancel all active requests
   */
  cancelAll(): void {
    console.log('🚫 Cancelling all requests:', { count: this.activeRequests.size })
    this.activeRequests.forEach(controller => controller.abort())
    this.activeRequests.clear()
  }

  /**
   * Clear cache
   * @param pattern - Optional pattern to match cache keys (regex string)
   */
  clearCache(pattern?: string): void {
    if (pattern) {
      const regex = new RegExp(pattern)
      const keysToDelete: string[] = []
      
      this.cache.forEach((_, key) => {
        if (regex.test(key)) {
          keysToDelete.push(key)
        }
      })
      
      keysToDelete.forEach(key => this.cache.delete(key))
      console.log('🗑️ Cache cleared:', { pattern, count: keysToDelete.length })
    } else {
      this.cache.clear()
      console.log('🗑️ All cache cleared')
    }
  }

  async post<T>(endpoint: string, body?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  }

  async put<T>(endpoint: string, body?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body),
    })
  }

  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'DELETE',
    })
  }

  async patch<T>(endpoint: string, body?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: JSON.stringify(body),
    })
  }
}

// Export singleton instance
export const apiClient = new ApiClient()
