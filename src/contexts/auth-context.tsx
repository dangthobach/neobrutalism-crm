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

  /**
   * ⭐ CHANGED: Initialize auth state from OAuth2 session (not localStorage)
   * Checks if user has active OAuth2 session by fetching profile from backend
   */
  useEffect(() => {
    const initializeAuth = async () => {
      console.log('[AuthContext] Initializing OAuth2 auth...')
      try {
        // Try to fetch current user profile (will use session cookie automatically)
        console.log('[AuthContext] Checking OAuth2 session...')
        const profile = await userApi.getCurrentUserProfile()
        console.log('[AuthContext] OAuth2 session active, profile fetched:', profile)

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
          console.log('[AuthContext] User authenticated via OAuth2 session')
        }
      } catch (err: any) {
        // No session or session expired
        if (err?.status === 401) {
          console.log('[AuthContext] No active OAuth2 session (401)')
        } else {
          console.error('[AuthContext] Failed to check OAuth2 session:', err)
        }
      } finally {
        console.log('[AuthContext] Initialization complete, isLoading = false')
        setIsLoading(false)
      }
    }

    initializeAuth()
  }, [])

  /**
   * ⭐ CHANGED: OAuth2 login via redirect (not POST /auth/login)
   * Redirects to Gateway OAuth2 authorization endpoint (Keycloak)
   */
  const login = async (_credentials: LoginRequest) => {
    try {
      setIsLoading(true)
      console.log('[AuthContext] Redirecting to OAuth2 login (Keycloak)...')

      // Redirect to Gateway OAuth2 authorization endpoint
      if (typeof window !== 'undefined') {
        window.location.href = '/login/oauth2/authorization/keycloak'
      }
    } catch (error) {
      console.error('OAuth2 redirect failed:', error)
      throw error
    } finally {
      setIsLoading(false)
    }
  }

  /**
   * ⭐ CHANGED: OAuth2 logout via Gateway (clears session + redirects to Keycloak logout)
   */
  const logout = () => {
    try {
      console.log('[AuthContext] Logging out via OAuth2...')

      // Clear any scheduled refresh timer
      if (refreshTimer) {
        clearTimeout(refreshTimer)
        setRefreshTimer(null)
      }

      // Clear user data
      setUser(null)

      // Redirect to Gateway logout endpoint (POST /logout)
      // Gateway will clear session and redirect to Keycloak logout
      if (typeof window !== 'undefined') {
        window.location.href = '/logout'
      }
    } catch (error) {
      console.error('Logout failed:', error)
    }
  }

  /**
   * ⭐ REMOVED: OAuth2 Gateway handles token refresh automatically
   * No manual refresh needed - Spring Security manages this transparently
   */
  const refreshToken = async () => {
    console.warn('[AuthContext] refreshToken is deprecated - OAuth2 handles token refresh automatically')
    // No-op for backward compatibility
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
