"use client"

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/contexts/auth-context'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Loader2, Eye, EyeOff } from 'lucide-react'
import { toast } from 'sonner'

export default function LoginPage() {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    rememberMe: false
  })
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  
  const { login } = useAuth()
  const router = useRouter()
  const searchParams = typeof window !== 'undefined' ? new URLSearchParams(window.location.search) : null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!formData.username || !formData.password) {
      toast.error('Please fill in all fields')
      return
    }

    try {
      setIsLoading(true)
      console.log('Attempting login with:', { username: formData.username })

      await login({
        username: formData.username,
        password: formData.password,
        rememberMe: formData.rememberMe
      })

      console.log('Login successful, redirecting...')
      toast.success('Login successful!')

      // Check for returnUrl from query params (set by middleware)
      const returnUrl = searchParams?.get('returnUrl')
      const redirectPath = returnUrl || '/admin'

      console.log('Redirecting to:', redirectPath)

      // Use window.location for full page reload to ensure auth state is fresh
      if (typeof window !== 'undefined') {
        window.location.href = redirectPath
      } else {
        router.push(redirectPath)
      }
    } catch (error) {
      console.error('Login error:', error)
      toast.error('Login failed', {
        description: error instanceof Error ? error.message : 'Invalid credentials'
      })
    } finally {
      setIsLoading(false)
    }
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }))
  }

  return (
    <div className="min-h-screen bg-secondary-background flex items-center justify-center p-4">
      <Card className="w-full max-w-md border-4 border-black shadow-[8px_8px_0_#000]">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-heading">Welcome Back</CardTitle>
          <CardDescription className="font-base">
            Sign in to your account to continue
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="username" className="font-base">Username</Label>
              <Input
                id="username"
                name="username"
                type="text"
                placeholder="Enter your username"
                value={formData.username}
                onChange={handleInputChange}
                className="border-2 border-black font-base"
                disabled={isLoading}
                required
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="password" className="font-base">Password</Label>
              <div className="relative">
                <Input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Enter your password"
                  value={formData.password}
                  onChange={handleInputChange}
                  className="border-2 border-black font-base pr-10"
                  disabled={isLoading}
                  required
                />
                <Button
                  type="button"
                  variant="noShadow"
                  size="sm"
                  className="absolute right-2 top-1/2 -translate-y-1/2 h-6 w-6 p-0"
                  onClick={() => setShowPassword(!showPassword)}
                  disabled={isLoading}
                >
                  {showPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </Button>
              </div>
            </div>

            <div className="flex items-center space-x-2">
              <input
                id="rememberMe"
                name="rememberMe"
                type="checkbox"
                checked={formData.rememberMe}
                onChange={handleInputChange}
                disabled={isLoading}
                className="border-2 border-black"
              />
              <Label htmlFor="rememberMe" className="font-base text-sm">
                Remember me
              </Label>
            </div>

            <Button
              type="submit"
              className="w-full border-2 border-black bg-main text-main-foreground hover:translate-x-1 hover:translate-y-1 transition-all shadow-[4px_4px_0_#000]"
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Signing in...
                </>
              ) : (
                'Sign In'
              )}
            </Button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm font-base text-foreground/70">
              Demo credentials:
            </p>
            <p className="text-sm font-base text-foreground/70">
              Username: <span className="font-mono">admin</span>
            </p>
            <p className="text-sm font-base text-foreground/70">
              Password: <span className="font-mono">admin123</span>
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
