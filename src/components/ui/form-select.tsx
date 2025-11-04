/**
 * ✅ PHASE 1 WEEK 3: Reusable Form Components
 * 
 * Form select component with React Hook Form integration
 */

import React from 'react'
import { UseFormRegister, FieldValues, Path } from 'react-hook-form'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { FormField } from './form-field'
import { cn } from '@/lib/utils'

export interface FormSelectOption {
  value: string
  label: string
  disabled?: boolean
}

export interface FormSelectProps<T extends FieldValues>
  extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'name'> {
  name: Path<T>
  label?: string
  description?: string
  error?: string
  register: UseFormRegister<T>
  required?: boolean
  options: FormSelectOption[]
  placeholder?: string
  containerClassName?: string
  selectClassName?: string
}

export function FormSelect<T extends FieldValues>({
  name,
  label,
  description,
  error,
  register,
  required,
  options,
  placeholder = 'Select an option',
  containerClassName,
  selectClassName,
  ...props
}: FormSelectProps<T>) {
  return (
    <FormField
      label={label}
      error={error}
      description={description}
      required={required}
      className={containerClassName}
    >
      <select
        {...register(name)}
        {...props}
        className={cn(
          'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background',
          'file:border-0 file:bg-transparent file:text-sm file:font-medium',
          'placeholder:text-muted-foreground',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
          'disabled:cursor-not-allowed disabled:opacity-50',
          error && 'border-destructive focus-visible:ring-destructive',
          selectClassName
        )}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={error ? `${name}-error` : undefined}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map((option) => (
          <option
            key={option.value}
            value={option.value}
            disabled={option.disabled}
          >
            {option.label}
          </option>
        ))}
      </select>
    </FormField>
  )
}
