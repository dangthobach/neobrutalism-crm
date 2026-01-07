"use client"

import { useEffect } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Loader2 } from 'lucide-react'

/**
 * ⭐ CHANGED: OAuth2 Login Page
 *
 * BEFORE: Traditional username/password form → POST /auth/login → JWT tokens in localStorage
 * AFTER: Automatic redirect → OAuth2 Authorization Code Flow → Session cookies
 *
 * Flow:
 * 1. User visits /login
 * 2. Automatic redirect to Gateway OAuth2 endpoint
 * 3. Gateway redirects to Keycloak login page
 * 4. User enters credentials in Keycloak
 * 5. Keycloak redirects back to Gateway with authorization code
 * 6. Gateway exchanges code for tokens, creates session in Redis
 * 7. Gateway sets SESSION_ID cookie (HttpOnly, Secure)
 * 8. Gateway redirects to frontend (/admin or returnUrl)
 * 9. Frontend API calls automatically include session cookie
 *
 * Benefits:
 * - No JWT tokens in localStorage (XSS protection)
 * - No manual token refresh (OAuth2 handles it)
 * - HttpOnly session cookies (cannot be accessed by JavaScript)
 * - Centralized authentication via Keycloak SSO
 */
export default function LoginPage() {
  useEffect(() => {
    if (typeof window === 'undefined') return

    const params = new URLSearchParams(window.location.search)
    const returnUrl = params.get('returnUrl')
    const state = returnUrl ? `?returnUrl=${encodeURIComponent(returnUrl)}` : ''

    console.log('[Login] Redirecting to OAuth2 login (Keycloak)...')
    window.location.href = `/login/oauth2/authorization/keycloak${state}`
  }, [])

  return (
    <div className="min-h-screen bg-secondary-background flex items-center justify-center p-4">
      <Card className="w-full max-w-md border-4 border-black shadow-[8px_8px_0_#000]">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-heading">Redirecting to Login</CardTitle>
          <CardDescription className="font-base">
            Please wait while we redirect you to the login page...
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col items-center justify-center py-8">
          <Loader2 className="h-12 w-12 animate-spin text-main mb-4" />
          <p className="text-sm font-base text-foreground/70">
            Redirecting to Keycloak SSO...
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
