/**
 * ✅ PHASE 1 WEEK 3: Reusable Form Components
 * 
 * Form textarea component with React Hook Form integration
 */

import React from 'react'
import { UseFormRegister, FieldValues, Path } from 'react-hook-form'
import { Textarea } from '@/components/ui/textarea'
import { FormField } from './form-field'
import { cn } from '@/lib/utils'

export interface FormTextareaProps<T extends FieldValues>
  extends Omit<React.TextareaHTMLAttributes<HTMLTextAreaElement>, 'name'> {
  name: Path<T>
  label?: string
  description?: string
  error?: string
  register: UseFormRegister<T>
  required?: boolean
  containerClassName?: string
}

export function FormTextarea<T extends FieldValues>({
  name,
  label,
  description,
  error,
  register,
  required,
  containerClassName,
  className,
  ...props
}: FormTextareaProps<T>) {
  return (
    <FormField
      label={label}
      error={error}
      description={description}
      required={required}
      className={containerClassName}
    >
      <Textarea
        {...register(name)}
        {...props}
        className={cn(error && 'border-destructive focus-visible:ring-destructive', className)}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={error ? `${name}-error` : undefined}
      />
    </FormField>
  )
}
