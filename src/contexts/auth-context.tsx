"use client"

import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react'
import { authApi, LoginRequest, LoginResponse } from '@/lib/api/auth'
import { apiClient } from '@/lib/api/client'

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

  const isAuthenticated = !!user

  // Initialize auth state from localStorage
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const token = apiClient.getAccessToken()
        if (token) {
          // Verify token is still valid by getting current user
          const userId = await authApi.getCurrentUser()
          if (userId) {
            // For now, we'll create a minimal user object
            // In a real app, you'd fetch full user details
            setUser({
              id: userId,
              username: 'admin', // This should come from the API
              email: 'admin@example.com',
              firstName: 'Admin',
              lastName: 'User',
              fullName: 'Admin User',
              organizationId: '018e0010-0000-0000-0000-000000000001',
              roles: ['ADMIN'],
              permissions: ['*']
            })
          }
        }
      } catch (error) {
        console.error('Failed to initialize auth:', error)
        // Clear invalid token
        apiClient.setAccessToken(null)
      } finally {
        setIsLoading(false)
      }
    }

    initializeAuth()
  }, [])

  const login = async (credentials: LoginRequest) => {
    try {
      setIsLoading(true)
      const response: LoginResponse = await authApi.login(credentials)
      
      // Store tokens
      apiClient.setAccessToken(response.accessToken)
      
      // Store refresh token
      if (typeof window !== 'undefined') {
        localStorage.setItem('refresh_token', response.refreshToken)
      }
      
      // Set user data
      setUser(response.user)
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
      }
      
      // Update user data
      setUser(response.user)
    } catch (error) {
      console.error('Token refresh failed:', error)
      // If refresh fails, logout user
      logout()
      throw error
    }
  }

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
