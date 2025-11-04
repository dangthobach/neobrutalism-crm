/**
 * ✅ PHASE 1 WEEK 3: Reusable Form Components
 * 
 * Base form field wrapper with label, error message, and description
 */

import React from 'react'
import { cn } from '@/lib/utils'

export interface FormFieldProps {
  label?: string
  error?: string
  description?: string
  required?: boolean
  children: React.ReactNode
  className?: string
  labelClassName?: string
  errorClassName?: string
  descriptionClassName?: string
}

export const FormField = React.forwardRef<HTMLDivElement, FormFieldProps>(
  (
    {
      label,
      error,
      description,
      required,
      children,
      className,
      labelClassName,
      errorClassName,
      descriptionClassName,
    },
    ref
  ) => {
    return (
      <div ref={ref} className={cn('space-y-2', className)}>
        {label && (
          <label
            className={cn(
              'text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70',
              error && 'text-destructive',
              labelClassName
            )}
          >
            {label}
            {required && <span className="ml-1 text-destructive">*</span>}
          </label>
        )}
        
        {children}
        
        {description && !error && (
          <p
            className={cn(
              'text-xs text-muted-foreground',
              descriptionClassName
            )}
          >
            {description}
          </p>
        )}
        
        {error && (
          <p
            className={cn(
              'text-xs font-medium text-destructive',
              errorClassName
            )}
          >
            {error}
          </p>
        )}
      </div>
    )
  }
)

FormField.displayName = 'FormField'
