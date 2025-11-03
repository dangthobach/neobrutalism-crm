/**
 * Advanced Search Dialog Component
 * Reusable search component for Users, Roles, Groups, and Organizations
 */

import { useState } from "react"
import { Search, X, Filter } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

export interface SearchFilter {
  field: string
  label: string
  type: 'text' | 'select' | 'date'
  options?: { label: string; value: string }[]
  placeholder?: string
}

export interface SearchValues {
  [key: string]: string
}

interface AdvancedSearchDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  filters: SearchFilter[]
  onSearch: (values: SearchValues) => void
  title?: string
}

export function AdvancedSearchDialog({
  open,
  onOpenChange,
  filters,
  onSearch,
  title = "Advanced Search"
}: AdvancedSearchDialogProps) {
  const [searchValues, setSearchValues] = useState<SearchValues>({})

  const handleSearch = () => {
    // Filter out empty values
    const activeFilters = Object.entries(searchValues)
      .filter(([_, value]) => value && value.trim() !== '')
      .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {})
    
    onSearch(activeFilters)
    onOpenChange(false)
  }

  const handleReset = () => {
    setSearchValues({})
    onSearch({})
  }

  const handleValueChange = (field: string, value: string) => {
    setSearchValues(prev => ({ ...prev, [field]: value }))
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="border-4 border-black shadow-[8px_8px_0_#000] max-w-2xl">
        <DialogHeader>
          <DialogTitle className="font-heading text-2xl flex items-center gap-2">
            <Filter className="h-6 w-6" />
            {title}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="grid gap-4 sm:grid-cols-2">
            {filters.map((filter) => (
              <div key={filter.field} className="space-y-2">
                <Label htmlFor={filter.field} className="font-base">
                  {filter.label}
                </Label>
                
                {filter.type === 'text' && (
                  <Input
                    id={filter.field}
                    placeholder={filter.placeholder || filter.label}
                    value={searchValues[filter.field] || ''}
                    onChange={(e) => handleValueChange(filter.field, e.target.value)}
                    className="border-2 border-black font-base"
                  />
                )}

                {filter.type === 'select' && filter.options && (
                  <Select
                    value={searchValues[filter.field] || ''}
                    onValueChange={(value) => handleValueChange(filter.field, value)}
                  >
                    <SelectTrigger className="border-2 border-black font-base">
                      <SelectValue placeholder={filter.placeholder || `Select ${filter.label}`} />
                    </SelectTrigger>
                    <SelectContent className="border-2 border-black">
                      <SelectItem value="">All</SelectItem>
                      {filter.options.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}

                {filter.type === 'date' && (
                  <Input
                    id={filter.field}
                    type="date"
                    value={searchValues[filter.field] || ''}
                    onChange={(e) => handleValueChange(filter.field, e.target.value)}
                    className="border-2 border-black font-base"
                  />
                )}
              </div>
            ))}
          </div>

          {/* Active Filters Display */}
          {Object.entries(searchValues).filter(([_, value]) => value && value.trim() !== '').length > 0 && (
            <div className="pt-4 border-t-2 border-black">
              <p className="text-sm font-base text-foreground/70 mb-2">Active Filters:</p>
              <div className="flex flex-wrap gap-2">
                {Object.entries(searchValues)
                  .filter(([_, value]) => value && value.trim() !== '')
                  .map(([key, value]) => {
                    const filter = filters.find(f => f.field === key)
                    return (
                      <div
                        key={key}
                        className="inline-flex items-center gap-2 px-3 py-1 border-2 border-black bg-main text-main-foreground text-sm font-base"
                      >
                        <span className="font-heading">{filter?.label}:</span>
                        <span>{value}</span>
                        <button
                          onClick={() => handleValueChange(key, '')}
                          className="hover:bg-black/10 rounded"
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </div>
                    )
                  })}
              </div>
            </div>
          )}
        </div>

        <DialogFooter className="gap-2">
          <Button
            variant="noShadow"
            onClick={handleReset}
            className="border-2 border-black"
          >
            Reset
          </Button>
          <Button
            variant="noShadow"
            onClick={handleSearch}
            className="border-2 border-black bg-main text-main-foreground"
          >
            <Search className="h-4 w-4 mr-2" />
            Search
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// Example usage configurations for different entities

export const userSearchFilters: SearchFilter[] = [
  { field: 'username', label: 'Username', type: 'text', placeholder: 'Enter username' },
  { field: 'email', label: 'Email', type: 'text', placeholder: 'Enter email' },
  { field: 'firstName', label: 'First Name', type: 'text', placeholder: 'Enter first name' },
  { field: 'lastName', label: 'Last Name', type: 'text', placeholder: 'Enter last name' },
  {
    field: 'status',
    label: 'Status',
    type: 'select',
    options: [
      { label: 'Active', value: 'ACTIVE' },
      { label: 'Inactive', value: 'INACTIVE' },
      { label: 'Suspended', value: 'SUSPENDED' },
    ]
  },
  { field: 'organizationId', label: 'Organization ID', type: 'text', placeholder: 'Enter organization ID' },
  { field: 'createdAfter', label: 'Created After', type: 'date' },
  { field: 'createdBefore', label: 'Created Before', type: 'date' },
]

export const roleSearchFilters: SearchFilter[] = [
  { field: 'code', label: 'Code', type: 'text', placeholder: 'Enter role code' },
  { field: 'name', label: 'Name', type: 'text', placeholder: 'Enter role name' },
  { field: 'description', label: 'Description', type: 'text', placeholder: 'Enter description' },
  {
    field: 'status',
    label: 'Status',
    type: 'select',
    options: [
      { label: 'Active', value: 'ACTIVE' },
      { label: 'Inactive', value: 'INACTIVE' },
      { label: 'Suspended', value: 'SUSPENDED' },
    ]
  },
  {
    field: 'isSystem',
    label: 'System Role',
    type: 'select',
    options: [
      { label: 'Yes', value: 'true' },
      { label: 'No', value: 'false' },
    ]
  },
  { field: 'minPriority', label: 'Min Priority', type: 'text', placeholder: 'Enter minimum priority' },
  { field: 'maxPriority', label: 'Max Priority', type: 'text', placeholder: 'Enter maximum priority' },
]

export const groupSearchFilters: SearchFilter[] = [
  { field: 'code', label: 'Code', type: 'text', placeholder: 'Enter group code' },
  { field: 'name', label: 'Name', type: 'text', placeholder: 'Enter group name' },
  { field: 'description', label: 'Description', type: 'text', placeholder: 'Enter description' },
  {
    field: 'status',
    label: 'Status',
    type: 'select',
    options: [
      { label: 'Active', value: 'ACTIVE' },
      { label: 'Inactive', value: 'INACTIVE' },
      { label: 'Suspended', value: 'SUSPENDED' },
    ]
  },
  { field: 'organizationId', label: 'Organization ID', type: 'text', placeholder: 'Enter organization ID' },
  { field: 'parentId', label: 'Parent Group ID', type: 'text', placeholder: 'Enter parent group ID' },
  {
    field: 'level',
    label: 'Level',
    type: 'select',
    options: [
      { label: 'Root (0)', value: '0' },
      { label: 'Level 1', value: '1' },
      { label: 'Level 2', value: '2' },
      { label: 'Level 3', value: '3' },
    ]
  },
]

export const organizationSearchFilters: SearchFilter[] = [
  { field: 'code', label: 'Code', type: 'text', placeholder: 'Enter organization code' },
  { field: 'name', label: 'Name', type: 'text', placeholder: 'Enter organization name' },
  { field: 'description', label: 'Description', type: 'text', placeholder: 'Enter description' },
  { field: 'email', label: 'Email', type: 'text', placeholder: 'Enter email' },
  { field: 'phone', label: 'Phone', type: 'text', placeholder: 'Enter phone' },
  { field: 'website', label: 'Website', type: 'text', placeholder: 'Enter website' },
  {
    field: 'status',
    label: 'Status',
    type: 'select',
    options: [
      { label: 'Draft', value: 'DRAFT' },
      { label: 'Active', value: 'ACTIVE' },
      { label: 'Inactive', value: 'INACTIVE' },
      { label: 'Suspended', value: 'SUSPENDED' },
      { label: 'Archived', value: 'ARCHIVED' },
    ]
  },
  { field: 'createdAfter', label: 'Created After', type: 'date' },
  { field: 'createdBefore', label: 'Created Before', type: 'date' },
]
