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
  private accessToken: string | null = null
  private refreshInProgress: Promise<void> | null = null
  private activeRequests = new Map<string, AbortController>()
  private cache = new Map<string, { data: any; timestamp: number }>()
  private readonly DEFAULT_TIMEOUT = 30000 // 30 seconds
  private readonly CACHE_TTL = 60000 // 1 minute

  constructor(baseUrl?: string) {
    this.baseUrl = baseUrl || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
  }

  setAccessToken(token: string | null) {
    this.accessToken = token

    // Only access localStorage in browser environment
    if (typeof window === 'undefined') {
      console.warn('[ApiClient] Cannot set token in SSR environment')
      return
    }

    if (token) {
      localStorage.setItem('access_token', token)
      console.log('[ApiClient] Token saved to localStorage')

      // Also set as cookie for middleware to check
      if (typeof document !== 'undefined') {
        document.cookie = `access_token=${token}; path=/; max-age=${7 * 24 * 60 * 60}; SameSite=Lax`
        console.log('[ApiClient] Token saved to cookie')
      }
    } else {
      localStorage.removeItem('access_token')
      console.log('[ApiClient] Token removed from localStorage')

      // Clear cookie
      if (typeof document !== 'undefined') {
        document.cookie = 'access_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT'
        console.log('[ApiClient] Token removed from cookie')
      }
    }
  }

  clearRefreshToken() {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('refresh_token')
    }
  }

  getAccessToken(): string | null {
    if (!this.accessToken && typeof window !== 'undefined') {
      this.accessToken = localStorage.getItem('access_token')
    }
    return this.accessToken
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
    retryCount = 0,
    maxRetries = 3
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`
    const token = this.getAccessToken()
    
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

    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

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
        // Handle 401 Unauthorized - try to refresh token
        if (response.status === 401 && retryCount === 0 && token) {
          // If a refresh is already in progress, wait for it instead of starting a new one
          if (this.refreshInProgress) {
            try {
              await this.refreshInProgress
              return this.request(endpoint, options, retryCount + 1)
            } catch (e) {
              // refresh failed
              this.setAccessToken(null)
              this.clearRefreshToken()
              if (typeof window !== 'undefined') window.location.href = '/login'
              throw new ApiError(401, 'UNAUTHORIZED', 'Session expired')
            }
          }

          // Start refresh flow
          this.refreshInProgress = this.refreshToken()
          try {
            await this.refreshInProgress
            // refresh succeeded, clear the marker and retry
            this.refreshInProgress = null
            return this.request(endpoint, options, retryCount + 1)
          } catch (refreshError) {
            // If refresh fails, clear tokens and redirect to login
            this.refreshInProgress = null
            this.setAccessToken(null)
            this.clearRefreshToken()
            if (typeof window !== 'undefined') window.location.href = '/login'
            throw new ApiError(401, 'UNAUTHORIZED', 'Session expired')
          }
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
        throw new ApiError(0, 'TIMEOUT', 'Request timeout or cancelled')
      }

      if (error instanceof ApiError) {
        console.error('❌ API Error:', {
          status: error.status,
          code: error.code,
          message: error.message,
        })
        
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

  private async refreshToken(): Promise<void> {
    const refreshTokenValue = typeof window !== 'undefined' 
      ? localStorage.getItem('refresh_token') 
      : null
    
    if (!refreshTokenValue) {
      throw new Error('No refresh token available')
    }

    try {
      const response = await fetch(`${this.baseUrl}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refreshToken: refreshTokenValue }),
      })

      if (!response.ok) {
        throw new Error('Token refresh failed')
      }

      const data = await response.json()
      
      if (data.data) {
        this.setAccessToken(data.data.accessToken)
        if (typeof window !== 'undefined') {
          localStorage.setItem('refresh_token', data.data.refreshToken)
        }
      }
    } catch (error) {
      console.error('Token refresh failed:', error)
      throw error
    }
  }

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
