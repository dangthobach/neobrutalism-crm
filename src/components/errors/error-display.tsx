/**
 * ✅ PHASE 1: Error Display Component
 * Shows user-friendly error messages with retry actions
 */

import { AlertCircle, Info, AlertTriangle, XCircle, RefreshCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { type UserFriendlyError } from '@/lib/errors/error-handler'

interface ErrorDisplayProps {
  error: UserFriendlyError
  onRetry?: () => void
  className?: string
}

export function ErrorDisplay({ error, onRetry, className }: ErrorDisplayProps) {
  // Map error type to icon and color
  const getIcon = () => {
    switch (error.type) {
      case 'validation':
        return <AlertCircle className="h-4 w-4" />
      case 'business':
        return <Info className="h-4 w-4" />
      case 'auth':
        return <XCircle className="h-4 w-4" />
      case 'network':
        return <AlertTriangle className="h-4 w-4" />
      case 'server':
        return <XCircle className="h-4 w-4" />
      default:
        return <AlertCircle className="h-4 w-4" />
    }
  }

  const getVariant = () => {
    switch (error.type) {
      case 'validation':
        return 'default'
      case 'business':
        return 'default'
      case 'auth':
        return 'destructive'
      case 'network':
        return 'destructive'
      case 'server':
        return 'destructive'
      default:
        return 'default'
    }
  }

  return (
    <Alert variant={getVariant()} className={className}>
      {getIcon()}
      <AlertTitle>{error.title}</AlertTitle>
      <AlertDescription>
        <div className="mt-2 space-y-2">
          <p>{error.message}</p>

          {/* Show field-level errors if present */}
          {error.details && Object.keys(error.details).length > 0 && (
            <ul className="mt-2 list-disc list-inside space-y-1">
              {Object.entries(error.details).map(([field, message]) => (
                <li key={field} className="text-sm">
                  <span className="font-medium">{field}:</span> {message}
                </li>
              ))}
            </ul>
          )}

          {/* Show retry button if error is retryable */}
          {error.canRetry && onRetry && (
            <Button
              variant="default"
              size="sm"
              onClick={onRetry}
              className="mt-3"
            >
              <RefreshCw className="mr-2 h-4 w-4" />
              Try Again
            </Button>
          )}

          {/* Show custom action if present */}
          {error.action && (
            <Button
              variant="default"
              size="sm"
              onClick={error.action.handler}
              className="mt-3"
            >
              {error.action.label}
            </Button>
          )}
        </div>
      </AlertDescription>
    </Alert>
  )
}

/**
 * Compact error display for inline form errors
 */
export function InlineError({ message }: { message: string }) {
  return (
    <div className="flex items-start gap-2 text-sm text-red-600">
      <AlertCircle className="h-4 w-4 mt-0.5" />
      <span>{message}</span>
    </div>
  )
}
