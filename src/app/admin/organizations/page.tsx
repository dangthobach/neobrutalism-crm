"use client"

import { useEffect, useState } from "react"
import { Plus, Search, Building2, Mail, Phone, Globe, MapPin, Pencil, Trash2, Archive, PlayCircle, PauseCircle, Filter, X, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  organizationsAPI,
  type Organization,
  type OrganizationReadModel,
  type OrganizationStatus
} from "@/lib/api/organizations"
import { OrganizationDialog } from "./organization-dialog"
import { toast } from "sonner"

export default function OrganizationsPage() {
  const [organizations, setOrganizations] = useState<OrganizationReadModel[]>([])
  const [filteredOrganizations, setFilteredOrganizations] = useState<OrganizationReadModel[]>([])
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState<string | null>(null) // Track which action is loading
  const [searchQuery, setSearchQuery] = useState("")
  const [statusFilter, setStatusFilter] = useState<OrganizationStatus | "ALL">("ALL")
  const [dialogOpen, setDialogOpen] = useState(false)
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null)
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null)
  
  // Pagination state
  const [currentPage, setCurrentPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    loadOrganizations()
  }, [currentPage, pageSize])

  useEffect(() => {
    filterOrganizations()
  }, [organizations, searchQuery, statusFilter])

  const loadOrganizations = async () => {
    try {
      setLoading(true)
      const response = await organizationsAPI.queryAllPaged(currentPage, pageSize, "id", "ASC")
      setOrganizations(response.content)
      setTotalElements(response.totalElements)
      setTotalPages(response.totalPages)
    } catch (error) {
      console.error("Failed to load organizations:", error)
      toast.error("Failed to load organizations", {
        description: error instanceof Error ? error.message : "An unexpected error occurred"
      })
    } finally {
      setLoading(false)
    }
  }

  const filterOrganizations = () => {
    let filtered = organizations

    // Filter by status
    if (statusFilter !== "ALL") {
      filtered = filtered.filter(org => org.status === statusFilter)
    }

    // Filter by search query (client-side filtering for now)
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase()
      filtered = filtered.filter(org =>
        org.name.toLowerCase().includes(query) ||
        org.code.toLowerCase().includes(query) ||
        org.description?.toLowerCase().includes(query) ||
        org.email?.toLowerCase().includes(query)
      )
    }

    setFilteredOrganizations(filtered)
  }

  // Calculate statistics from current data
  const calculateStatistics = () => {
    const total = organizations.length
    const active = organizations.filter(org => org.status === "ACTIVE").length
    const withContact = organizations.filter(org => org.email || org.phone).length
    const deleted = organizations.filter(org => org.isDeleted).length
    
    return { total, active, withContact, deleted }
  }

  const handleCreate = () => {
    setSelectedOrg(null)
    setDialogOpen(true)
  }

  const handleEdit = async (org: OrganizationReadModel) => {
    try {
      setActionLoading(`edit-${org.id}`)
      // Load full organization details from write model
      const fullOrg = await organizationsAPI.getById(org.id)
      setSelectedOrg(fullOrg)
      setDialogOpen(true)
    } catch (error) {
      console.error("Failed to load organization:", error)
      toast.error("Failed to load organization", {
        description: error instanceof Error ? error.message : "An unexpected error occurred"
      })
    } finally {
      setActionLoading(null)
    }
  }

  const handleDelete = async (id: string) => {
    try {
      setActionLoading(`delete-${id}`)
      await organizationsAPI.delete(id)
      toast.success("Organization deleted successfully")
      await loadOrganizations()
      setDeleteConfirm(null)
    } catch (error) {
      console.error("Failed to delete organization:", error)
      toast.error("Failed to delete organization", {
        description: error instanceof Error ? error.message : "An unexpected error occurred"
      })
    } finally {
      setActionLoading(null)
    }
  }

  const handleStatusChange = async (id: string, action: "activate" | "suspend" | "archive") => {
    try {
      const reason = prompt(`Enter reason for ${action}:`)
      if (!reason) return

      setActionLoading(`${action}-${id}`)

      switch (action) {
        case "activate":
          await organizationsAPI.activate(id, reason)
          break
        case "suspend":
          await organizationsAPI.suspend(id, reason)
          break
        case "archive":
          await organizationsAPI.archive(id, reason)
          break
      }

      toast.success(`Organization ${action}d successfully`)
      await loadOrganizations()
    } catch (error) {
      console.error(`Failed to ${action} organization:`, error)
      toast.error(`Failed to ${action} organization`, {
        description: error instanceof Error ? error.message : "An unexpected error occurred"
      })
    } finally {
      setActionLoading(null)
    }
  }

  const handleDialogClose = async (success: boolean) => {
    setDialogOpen(false)
    setSelectedOrg(null)
    if (success) {
      await loadOrganizations()
    }
  }

  const getStatusColor = (status: OrganizationStatus) => {
    switch (status) {
      case "ACTIVE":
        return "bg-green-500"
      case "DRAFT":
        return "bg-gray-500"
      case "INACTIVE":
        return "bg-yellow-500"
      case "SUSPENDED":
        return "bg-red-500"
      case "ARCHIVED":
        return "bg-blue-500"
      default:
        return "bg-gray-500"
    }
  }

  const statusOptions: (OrganizationStatus | "ALL")[] = ["ALL", "DRAFT", "ACTIVE", "INACTIVE", "SUSPENDED", "ARCHIVED"]

  return (
    <div className="space-y-4">
      {/* Header */}
      <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-heading">Organizations</h1>
            <p className="text-sm font-base mt-1 opacity-90">
              Manage your organization accounts and contacts
            </p>
          </div>
          <Button
            onClick={handleCreate}
            className="bg-background text-foreground border-2 border-black hover:translate-x-1 hover:translate-y-1 transition-all shadow-[4px_4px_0_#000]"
          >
            <Plus className="h-4 w-4 mr-2" />
            Add Organization
          </Button>
        </div>
      </header>

      {/* Statistics */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        {(() => {
          const stats = calculateStatistics()
          return [
            { label: "Total", value: stats.total, icon: Building2, color: "bg-main" },
            { label: "Active", value: stats.active, icon: PlayCircle, color: "bg-green-500" },
            { label: "With Contact", value: stats.withContact, icon: Mail, color: "bg-blue-500" },
            { label: "Deleted", value: stats.deleted, icon: Archive, color: "bg-red-500" },
          ].map((stat, index) => {
            const Icon = stat.icon
            return (
              <div
                key={index}
                className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <p className="text-sm font-base text-foreground/70">{stat.label}</p>
                    <p className="text-3xl font-heading mt-2">{stat.value}</p>
                  </div>
                  <div className={`border-2 border-black p-2 ${stat.color} text-white`}>
                    <Icon className="h-5 w-5" />
                  </div>
                </div>
              </div>
            )
          })
        })()}
      </div>

      {/* Filters */}
      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <div className="flex flex-col sm:flex-row gap-4">
          {/* Search */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-foreground/50" />
            <Input
              placeholder="Search organizations..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 border-2 border-black font-base"
            />
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-2 flex-wrap">
            <Filter className="h-4 w-4 text-foreground/50" />
            {statusOptions.map((status) => (
              <Button
                key={status}
                variant={statusFilter === status ? "default" : "noShadow"}
                size="sm"
                onClick={() => setStatusFilter(status)}
                className={`border-2 border-black transition-all ${
                  statusFilter === status ? "shadow-[4px_4px_0_#000]" : ""
                }`}
              >
                {status}
              </Button>
            ))}
          </div>
        </div>
      </div>

      {/* Organizations Grid */}
      {loading ? (
        <div className="border-4 border-black bg-background p-12 shadow-[8px_8px_0_#000] text-center">
          <p className="text-lg font-base">Loading organizations...</p>
        </div>
      ) : filteredOrganizations.length === 0 ? (
        <div className="border-4 border-black bg-background p-12 shadow-[8px_8px_0_#000] text-center">
          <Building2 className="h-16 w-16 mx-auto mb-4 text-foreground/30" />
          <p className="text-lg font-heading mb-2">No organizations found</p>
          <p className="text-sm font-base text-foreground/60 mb-4">
            {searchQuery || statusFilter !== "ALL"
              ? "Try adjusting your filters"
              : "Get started by creating your first organization"}
          </p>
          {!searchQuery && statusFilter === "ALL" && (
            <Button onClick={handleCreate} className="border-2 border-black shadow-[4px_4px_0_#000]">
              <Plus className="h-4 w-4 mr-2" />
              Create Organization
            </Button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredOrganizations.map((org) => (
            <div
              key={org.id}
              className="border-4 border-black bg-background shadow-[8px_8px_0_#000] hover:translate-x-1 hover:translate-y-1 transition-all"
            >
              {/* Card Header */}
              <div className="border-b-4 border-black p-4 bg-secondary-background">
                <div className="flex items-start justify-between">
                  <div className="flex-1 min-w-0">
                    <h3 className="text-lg font-heading truncate">{org.name}</h3>
                    <p className="text-sm font-base text-foreground/60">{org.code}</p>
                  </div>
                  <div className={`px-2 py-1 border-2 border-black text-xs font-base ${getStatusColor(org.status)} text-white`}>
                    {org.status}
                  </div>
                </div>
              </div>

              {/* Card Body */}
              <div className="p-4 space-y-3">
                {org.description && (
                  <p className="text-sm font-base text-foreground/80 line-clamp-2">
                    {org.description}
                  </p>
                )}

                <div className="space-y-2 text-sm font-base">
                  {org.email && (
                    <div className="flex items-center gap-2 text-foreground/70">
                      <Mail className="h-4 w-4 flex-shrink-0" />
                      <span className="truncate">{org.email}</span>
                    </div>
                  )}
                  {org.phone && (
                    <div className="flex items-center gap-2 text-foreground/70">
                      <Phone className="h-4 w-4 flex-shrink-0" />
                      <span className="truncate">{org.phone}</span>
                    </div>
                  )}
                  {org.website && (
                    <div className="flex items-center gap-2 text-foreground/70">
                      <Globe className="h-4 w-4 flex-shrink-0" />
                      <span className="truncate">{org.website}</span>
                    </div>
                  )}
                </div>

                {/* Metadata */}
                <div className="pt-3 border-t-2 border-black space-y-1 text-xs font-base text-foreground/50">
                  <div>Created {org.daysSinceCreated} days ago</div>
                  <div>by {org.createdBy}</div>
                </div>
              </div>

              {/* Card Actions */}
              <div className="border-t-4 border-black p-2 flex gap-2">
                <Button
                  size="sm"
                  variant="noShadow"
                  onClick={() => handleEdit(org)}
                  disabled={actionLoading === `edit-${org.id}`}
                  className="flex-1 border-2 border-black"
                >
                  {actionLoading === `edit-${org.id}` ? (
                    <Loader2 className="h-3 w-3 mr-1 animate-spin" />
                  ) : (
                    <Pencil className="h-3 w-3 mr-1" />
                  )}
                  Edit
                </Button>

                {/* Status Actions */}
                {org.status !== "ACTIVE" && (
                  <Button
                    size="sm"
                    variant="noShadow"
                    onClick={() => handleStatusChange(org.id, "activate")}
                    disabled={actionLoading === `activate-${org.id}`}
                    className="border-2 border-black bg-green-500 text-white hover:bg-green-600"
                    title="Activate"
                  >
                    {actionLoading === `activate-${org.id}` ? (
                      <Loader2 className="h-3 w-3 animate-spin" />
                    ) : (
                      <PlayCircle className="h-3 w-3" />
                    )}
                  </Button>
                )}
                {org.status === "ACTIVE" && (
                  <Button
                    size="sm"
                    variant="noShadow"
                    onClick={() => handleStatusChange(org.id, "suspend")}
                    disabled={actionLoading === `suspend-${org.id}`}
                    className="border-2 border-black bg-yellow-500 text-white hover:bg-yellow-600"
                    title="Suspend"
                  >
                    {actionLoading === `suspend-${org.id}` ? (
                      <Loader2 className="h-3 w-3 animate-spin" />
                    ) : (
                      <PauseCircle className="h-3 w-3" />
                    )}
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="noShadow"
                  onClick={() => handleStatusChange(org.id, "archive")}
                  disabled={actionLoading === `archive-${org.id}`}
                  className="border-2 border-black bg-blue-500 text-white hover:bg-blue-600"
                  title="Archive"
                >
                  {actionLoading === `archive-${org.id}` ? (
                    <Loader2 className="h-3 w-3 animate-spin" />
                  ) : (
                    <Archive className="h-3 w-3" />
                  )}
                </Button>

                {/* Delete */}
                {deleteConfirm === org.id ? (
                  <>
                    <Button
                      size="sm"
                      variant="noShadow"
                      onClick={() => handleDelete(org.id)}
                      disabled={actionLoading === `delete-${org.id}`}
                      className="border-2 border-black bg-red-500 text-white hover:bg-red-600"
                      title="Confirm Delete"
                    >
                      {actionLoading === `delete-${org.id}` ? (
                        <Loader2 className="h-3 w-3 animate-spin" />
                      ) : (
                        "✓"
                      )}
                    </Button>
                    <Button
                      size="sm"
                      variant="noShadow"
                      onClick={() => setDeleteConfirm(null)}
                      disabled={actionLoading === `delete-${org.id}`}
                      className="border-2 border-black"
                      title="Cancel"
                    >
                      <X className="h-3 w-3" />
                    </Button>
                  </>
                ) : (
                  <Button
                    size="sm"
                    variant="noShadow"
                    onClick={() => setDeleteConfirm(org.id)}
                    disabled={!!actionLoading}
                    className="border-2 border-black bg-red-500 text-white hover:bg-red-600"
                    title="Delete"
                  >
                    <Trash2 className="h-3 w-3" />
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-6">
          <div className="text-sm text-foreground/70">
            Showing {currentPage * pageSize + 1} to {Math.min((currentPage + 1) * pageSize, totalElements)} of {totalElements} organizations
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="noShadow"
              size="sm"
              onClick={() => setCurrentPage(0)}
              disabled={currentPage === 0}
              className="border-2 border-black"
            >
              First
            </Button>
            <Button
              variant="noShadow"
              size="sm"
              onClick={() => setCurrentPage(currentPage - 1)}
              disabled={currentPage === 0}
              className="border-2 border-black"
            >
              Previous
            </Button>
            <span className="px-3 py-1 border-2 border-black bg-background">
              {currentPage + 1} of {totalPages}
            </span>
            <Button
              variant="noShadow"
              size="sm"
              onClick={() => setCurrentPage(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
              className="border-2 border-black"
            >
              Next
            </Button>
            <Button
              variant="noShadow"
              size="sm"
              onClick={() => setCurrentPage(totalPages - 1)}
              disabled={currentPage >= totalPages - 1}
              className="border-2 border-black"
            >
              Last
            </Button>
          </div>
        </div>
      )}

      {/* Create/Edit Dialog */}
      {dialogOpen && (
        <OrganizationDialog
          organization={selectedOrg}
          onClose={handleDialogClose}
        />
      )}
    </div>
  )
}
