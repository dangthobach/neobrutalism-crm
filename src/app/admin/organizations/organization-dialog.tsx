"use client"

import { useState, useEffect } from "react"
import { X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
  organizationsAPI,
  type Organization,
  type OrganizationRequest,
} from "@/lib/api/organizations"

interface OrganizationDialogProps {
  organization?: Organization | null
  onClose: (success: boolean) => void
}

export function OrganizationDialog({ organization, onClose }: OrganizationDialogProps) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [formData, setFormData] = useState<OrganizationRequest>({
    name: "",
    code: "",
    description: "",
    email: "",
    phone: "",
    website: "",
    address: "",
  })

  useEffect(() => {
    if (organization) {
      setFormData({
        name: organization.name,
        code: organization.code,
        description: organization.description || "",
        email: organization.email || "",
        phone: organization.phone || "",
        website: organization.website || "",
        address: organization.address || "",
      })
    }
  }, [organization])

  const handleChange = (field: keyof OrganizationRequest, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
    setError(null)
  }

  const validateForm = (): boolean => {
    if (!formData.name.trim()) {
      setError("Organization name is required")
      return false
    }
    if (formData.name.length < 2 || formData.name.length > 200) {
      setError("Organization name must be between 2 and 200 characters")
      return false
    }
    if (!formData.code.trim()) {
      setError("Organization code is required")
      return false
    }
    if (!/^[A-Z0-9_-]+$/.test(formData.code)) {
      setError("Organization code must contain only uppercase letters, numbers, dashes, and underscores")
      return false
    }
    if (formData.email && !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(formData.email)) {
      setError("Invalid email address")
      return false
    }
    if (formData.phone && !/^[+]?[(]?[0-9]{1,4}[)]?[-\s.]?[(]?[0-9]{1,4}[)]?[-\s.]?[0-9]{1,9}$/.test(formData.phone)) {
      setError("Invalid phone number")
      return false
    }
    if (formData.website && !/^https?:\/\/.+/.test(formData.website)) {
      setError("Website must start with http:// or https://")
      return false
    }
    return true
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validateForm()) {
      return
    }

    try {
      setLoading(true)
      setError(null)

      if (organization) {
        await organizationsAPI.update(organization.id, formData)
      } else {
        await organizationsAPI.create(formData)
      }

      onClose(true)
    } catch (err: any) {
      setError(err.message || "Failed to save organization")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="border-4 border-black bg-background shadow-[16px_16px_0_#000] max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="border-b-4 border-black p-4 bg-main text-main-foreground flex items-center justify-between">
          <h2 className="text-2xl font-heading">
            {organization ? "Edit Organization" : "Create Organization"}
          </h2>
          <Button
            variant="noShadow"
            size="sm"
            onClick={() => onClose(false)}
            className="bg-background text-foreground border-2 border-black"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="border-2 border-red-500 bg-red-50 text-red-900 p-3 text-sm font-base">
              {error}
            </div>
          )}

          {/* Name */}
          <div className="space-y-2">
            <Label htmlFor="name" className="font-heading">
              Organization Name *
            </Label>
            <Input
              id="name"
              value={formData.name}
              onChange={(e) => handleChange("name", e.target.value)}
              className="border-2 border-black font-base"
              placeholder="e.g., Acme Corporation"
              required
              maxLength={200}
            />
          </div>

          {/* Code */}
          <div className="space-y-2">
            <Label htmlFor="code" className="font-heading">
              Organization Code *
            </Label>
            <Input
              id="code"
              value={formData.code}
              onChange={(e) => handleChange("code", e.target.value.toUpperCase())}
              className="border-2 border-black font-base font-mono"
              placeholder="e.g., ACME-001"
              required
              disabled={!!organization} // Code cannot be changed after creation
              maxLength={50}
            />
            <p className="text-xs font-base text-foreground/60">
              Uppercase letters, numbers, dashes, and underscores only
              {organization && " (cannot be changed after creation)"}
            </p>
          </div>

          {/* Description */}
          <div className="space-y-2">
            <Label htmlFor="description" className="font-heading">
              Description
            </Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => handleChange("description", e.target.value)}
              className="border-2 border-black font-base resize-none"
              placeholder="Describe the organization..."
              rows={3}
              maxLength={1000}
            />
          </div>

          {/* Contact Information Section */}
          <div className="border-t-2 border-black pt-4">
            <h3 className="text-lg font-heading mb-4">Contact Information</h3>

            {/* Email */}
            <div className="space-y-2 mb-4">
              <Label htmlFor="email" className="font-heading">
                Email
              </Label>
              <Input
                id="email"
                type="email"
                value={formData.email}
                onChange={(e) => handleChange("email", e.target.value)}
                className="border-2 border-black font-base"
                placeholder="contact@example.com"
                maxLength={100}
              />
            </div>

            {/* Phone */}
            <div className="space-y-2 mb-4">
              <Label htmlFor="phone" className="font-heading">
                Phone
              </Label>
              <Input
                id="phone"
                type="tel"
                value={formData.phone}
                onChange={(e) => handleChange("phone", e.target.value)}
                className="border-2 border-black font-base"
                placeholder="+1 (555) 123-4567"
                maxLength={20}
              />
            </div>

            {/* Website */}
            <div className="space-y-2 mb-4">
              <Label htmlFor="website" className="font-heading">
                Website
              </Label>
              <Input
                id="website"
                type="url"
                value={formData.website}
                onChange={(e) => handleChange("website", e.target.value)}
                className="border-2 border-black font-base"
                placeholder="https://example.com"
                maxLength={200}
              />
            </div>

            {/* Address */}
            <div className="space-y-2">
              <Label htmlFor="address" className="font-heading">
                Address
              </Label>
              <Textarea
                id="address"
                value={formData.address}
                onChange={(e) => handleChange("address", e.target.value)}
                className="border-2 border-black font-base resize-none"
                placeholder="Street address, city, state, ZIP"
                rows={2}
                maxLength={500}
              />
            </div>
          </div>

          {/* Actions */}
          <div className="border-t-2 border-black pt-4 flex gap-3 justify-end">
            <Button
              type="button"
              variant="noShadow"
              onClick={() => onClose(false)}
              className="border-2 border-black"
              disabled={loading}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              className="border-2 border-black shadow-[4px_4px_0_#000] bg-main text-main-foreground"
              disabled={loading}
            >
              {loading ? "Saving..." : organization ? "Update Organization" : "Create Organization"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
