"use client"

import React, { createContext, useContext, useEffect, useState, useCallback, ReactNode } from 'react'
import { authApi, LoginRequest, LoginResponse } from '@/lib/api/auth'
import { apiClient } from '@/lib/api/client'
import { userApi } from '@/lib/api/users'

interface User {
  id: string
  username: string
  email: string
  firstName: string
  lastName: string
  fullName?: string
  avatar?: string
  organizationId: string
  roles: string[]
  permissions: string[]
}

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (credentials: LoginRequest) => Promise<void>
  logout: () => void
  refreshToken: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [refreshTimer, setRefreshTimer] = useState<NodeJS.Timeout | null>(null)

  const isAuthenticated = !!user

  // Stable reference to refreshToken function
  const refreshTokenRef = React.useRef<(() => Promise<void>) | null>(null)

  // Initialize auth state from localStorage
  useEffect(() => {
    const initializeAuth = async () => {
      console.log('[AuthContext] Initializing auth...')
      try {
        const token = apiClient.getAccessToken()
        console.log('[AuthContext] Token from localStorage:', token ? 'EXISTS' : 'NULL')

        if (token) {
          // Try to fetch full current user profile from backend
          try {
            console.log('[AuthContext] Fetching user profile...')
            const profile = await userApi.getCurrentUserProfile()
            console.log('[AuthContext] Profile fetched:', profile)

            if (profile) {
              setUser({
                id: profile.id,
                username: profile.username,
                email: profile.email,
                firstName: profile.firstName,
                lastName: profile.lastName,
                fullName: profile.fullName,
                avatar: profile.avatar,
                organizationId: profile.organizationId,
                roles: (profile as any).roles || [],
                permissions: (profile as any).permissions || [],
              })
              console.log('[AuthContext] User set successfully')
              
              // Schedule background refresh if we have an expiry stored
              if (typeof window !== 'undefined') {
                const expiresAt = localStorage.getItem('access_token_expires_at')
                if (expiresAt) {
                  const ms = Number(expiresAt) - Date.now()
                  // schedule only if expiry in future
                  if (ms > 0) {
                    // set to refresh 60s before expiry
                    const timeout = Math.max(ms - 60_000, 5_000)
                    const id = setTimeout(() => {
                      refreshTokenRef.current?.().catch(() => {})
                    }, timeout)
                    setRefreshTimer(id)
                  }
                }
              }
            }
          } catch (err) {
            // Token invalid or profile fetch failed -> clear tokens
            console.error('[AuthContext] Profile fetch failed:', err)
            apiClient.setAccessToken(null)
            if (typeof window !== 'undefined') localStorage.removeItem('refresh_token')
          }
        } else {
          console.log('[AuthContext] No token found, user not authenticated')
        }
      } catch (error) {
        console.error('[AuthContext] Failed to initialize auth:', error)
        // Clear invalid token
        apiClient.setAccessToken(null)
      } finally {
        console.log('[AuthContext] Initialization complete, isLoading = false')
        setIsLoading(false)
      }
    }

    initializeAuth()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []) // refreshToken is handled via ref to avoid dependency

  const login = async (credentials: LoginRequest) => {
    try {
      setIsLoading(true)
      console.log('[AuthContext] Login attempt for:', credentials.username)

      const response: LoginResponse = await authApi.login(credentials)
      console.log('[AuthContext] Login response received:', { userId: response.userId, username: response.username })

      // Store tokens
      apiClient.setAccessToken(response.accessToken)
      console.log('[AuthContext] Access token stored')

      // Store refresh token
      if (typeof window !== 'undefined') {
        localStorage.setItem('refresh_token', response.refreshToken)
        // store expiry timestamp
        const expiresAt = Date.now() + (response.expiresIn * 1000)
        localStorage.setItem('access_token_expires_at', String(expiresAt))
        console.log('[AuthContext] Refresh token and expiry stored')

        // schedule a refresh 60s before expiry
        const timeout = Math.max(response.expiresIn * 1000 - 60_000, 5_000)
        if (refreshTimer) {
          clearTimeout(refreshTimer)
        }
        const id = setTimeout(() => {
          refreshToken().catch(() => {})
        }, timeout)
        setRefreshTimer(id)
      }

      // Set user data - Map backend response to User interface
      const userData = {
        id: response.userId,
        username: response.username,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        fullName: response.fullName,
        avatar: undefined, // Backend doesn't return avatar in login
        organizationId: response.tenantId || '', // Map tenantId to organizationId
        roles: Array.from(response.roles || []),
        permissions: [], // Will be fetched separately or from user profile
      }

      setUser(userData)
      console.log('[AuthContext] User data set:', userData)
    } catch (error) {
      console.error('Login failed:', error)
      throw error
    } finally {
      setIsLoading(false)
    }
  }

  const logout = () => {
    try {
      // Clear tokens
      apiClient.setAccessToken(null)
      if (typeof window !== 'undefined') {
        localStorage.removeItem('refresh_token')
        localStorage.removeItem('access_token_expires_at')
      }
      
      // clear any scheduled refresh
      if (refreshTimer) {
        clearTimeout(refreshTimer)
        setRefreshTimer(null)
      }
      
      // Clear user data
      setUser(null)
      
      // Call logout API (optional)
      authApi.logout().catch(console.error)
    } catch (error) {
      console.error('Logout failed:', error)
    }
  }

  const refreshToken = async () => {
    try {
      const refreshTokenValue = typeof window !== 'undefined' 
        ? localStorage.getItem('refresh_token') 
        : null
      
      if (!refreshTokenValue) {
        throw new Error('No refresh token available')
      }

      const response: LoginResponse = await authApi.refreshToken({
        refreshToken: refreshTokenValue
      })
      
      // Update tokens
      apiClient.setAccessToken(response.accessToken)
      if (typeof window !== 'undefined') {
        localStorage.setItem('refresh_token', response.refreshToken)
        const expiresAt = Date.now() + (response.expiresIn * 1000)
        localStorage.setItem('access_token_expires_at', String(expiresAt))
        // reschedule refresh
        const timeout = Math.max(response.expiresIn * 1000 - 60_000, 5_000)
        if (refreshTimer) {
          clearTimeout(refreshTimer)
        }
        const id = setTimeout(() => {
          refreshTokenRef.current?.().catch(() => {})
        }, timeout)
        setRefreshTimer(id)
      }
      
      // Update user data - Map backend response to User interface
      setUser({
        id: response.userId,
        username: response.username,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        fullName: response.fullName,
        avatar: undefined,
        organizationId: response.tenantId || '',
        roles: Array.from(response.roles || []),
        permissions: [],
      })
    } catch (error) {
      console.error('Token refresh failed:', error)
      // If refresh fails, logout user
      logout()
      throw error
    }
  }

  // Update ref when refreshToken function changes
  refreshTokenRef.current = refreshToken

  const value: AuthContextType = {
    user,
    isAuthenticated,
    isLoading,
    login,
    logout,
    refreshToken
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
