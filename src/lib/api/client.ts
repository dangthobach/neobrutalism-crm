/**
 * API Client for communicating with backend
 * Handles authentication, error handling, and retries
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

class ApiClient {
  private baseUrl: string
  private accessToken: string | null = null
  private refreshInProgress: Promise<void> | null = null

  constructor(baseUrl?: string) {
    this.baseUrl = baseUrl || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
  }

  setAccessToken(token: string | null) {
    this.accessToken = token
    if (token) {
      localStorage.setItem('access_token', token)
    } else {
      localStorage.removeItem('access_token')
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
    retryCount = 0
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint}`
    const token = this.getAccessToken()

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    }

    if (options.headers) {
      Object.assign(headers, options.headers)
    }

    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers,
      })

      // Parse JSON response
      const data: ApiResponse<T> = await response.json()

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
          }
        }

        throw new ApiError(
          response.status,
          data.code || 'UNKNOWN_ERROR',
          data.message || 'An error occurred',
          data.data
        )
      }

      return data
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }

      // Network or parsing errors
      throw new ApiError(
        0,
        'NETWORK_ERROR',
        error instanceof Error ? error.message : 'Network error occurred'
      )
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

  async get<T>(endpoint: string, params?: Record<string, any>): Promise<ApiResponse<T>> {
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

    return this.request<T>(`${endpoint}${queryString}`, {
      method: 'GET',
    })
  }

  async post<T>(endpoint: string, body?: any): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  }

  async put<T>(endpoint: string, body?: any): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body),
    })
  }

  async delete<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'DELETE',
    })
  }
}

// Export singleton instance
export const apiClient = new ApiClient()
