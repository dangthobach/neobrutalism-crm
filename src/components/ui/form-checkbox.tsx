/**
 * ✅ PHASE 1 WEEK 3: Reusable Form Components
 * 
 * Form checkbox component with React Hook Form integration
 */

import React from 'react'
import { UseFormRegister, FieldValues, Path } from 'react-hook-form'
import { Checkbox } from '@/components/ui/checkbox'
import { FormField } from './form-field'
import { cn } from '@/lib/utils'

export interface FormCheckboxProps<T extends FieldValues>
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'name' | 'type'> {
  name: Path<T>
  label?: string
  description?: string
  error?: string
  register: UseFormRegister<T>
  required?: boolean
  containerClassName?: string
  checkboxClassName?: string
}

export function FormCheckbox<T extends FieldValues>({
  name,
  label,
  description,
  error,
  register,
  required,
  containerClassName,
  checkboxClassName,
  ...props
}: FormCheckboxProps<T>) {
  return (
    <FormField
      error={error}
      description={description}
      required={required}
      className={containerClassName}
    >
      <div className="flex items-center space-x-2">
        <input
          type="checkbox"
          {...register(name)}
          {...props}
          className={cn(
            'h-4 w-4 rounded border-gray-300 text-primary focus:ring-2 focus:ring-primary focus:ring-offset-2',
            error && 'border-destructive',
            checkboxClassName
          )}
          aria-invalid={error ? 'true' : 'false'}
          aria-describedby={error ? `${name}-error` : undefined}
        />
        {label && (
          <label
            className={cn(
              'text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70',
              error && 'text-destructive'
            )}
          >
            {label}
            {required && <span className="ml-1 text-destructive">*</span>}
          </label>
        )}
      </div>
    </FormField>
  )
}
