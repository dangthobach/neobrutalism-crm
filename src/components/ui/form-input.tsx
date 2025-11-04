/**
 * ✅ PHASE 1 WEEK 3: Reusable Form Components
 * 
 * Form input component with React Hook Form integration
 */

import React from 'react'
import { UseFormRegister, FieldValues, Path } from 'react-hook-form'
import { Input } from '@/components/ui/input'
import { FormField } from './form-field'
import { cn } from '@/lib/utils'

export interface FormInputProps<T extends FieldValues>
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'name'> {
  name: Path<T>
  label?: string
  description?: string
  error?: string
  register: UseFormRegister<T>
  required?: boolean
  containerClassName?: string
}

export function FormInput<T extends FieldValues>({
  name,
  label,
  description,
  error,
  register,
  required,
  containerClassName,
  className,
  ...props
}: FormInputProps<T>) {
  return (
    <FormField
      label={label}
      error={error}
      description={description}
      required={required}
      className={containerClassName}
    >
      <Input
        {...register(name)}
        {...props}
        className={cn(error && 'border-destructive focus-visible:ring-destructive', className)}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={error ? `${name}-error` : undefined}
      />
    </FormField>
  )
}
