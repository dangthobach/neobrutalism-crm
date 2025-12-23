"use client"

import { useMemo, useState, useCallback } from "react"
import { Plus, Search, Building2, Mail, Phone, Globe, Pencil, Trash2, Archive, PlayCircle, PauseCircle, Filter, X, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  type Organization,
  type OrganizationStatus
} from "@/lib/api/organizations"
import { 
  useOrganizations,
  useDeleteOrganization,
  useActivateOrganization,
  useSuspendOrganization,
  useArchiveOrganization
} from "@/hooks/useOrganizations"
import { OrganizationDialog } from "./organization-dialog"
import { PermissionGuard } from "@/components/auth/permission-guard"
import { toast } from "sonner"
import { AdvancedSearchDialog, organizationSearchFilters } from "@/components/ui/advanced-search-dialog"

export default function OrganizationsPage() {
  const [searchQuery, setSearchQuery] = useState("")
  const [statusFilter, setStatusFilter] = useState<OrganizationStatus | "ALL">("ALL")
  const [dialogOpen, setDialogOpen] = useState(false)
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null)
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null)
  const [advancedSearchOpen, setAdvancedSearchOpen] = useState(false)
  
  // Pagination state
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 20 })

  // Fetch organizations with React Query
  const { data: orgsData, isLoading, error, refetch } = useOrganizations({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sortBy: "id",
    sortDirection: "ASC"
  })

  // Mutations
  const deleteMutation = useDeleteOrganization()
  const activateMutation = useActivateOrganization()
  const suspendMutation = useSuspendOrganization()
  const archiveMutation = useArchiveOrganization()

  const organizations = useMemo(() => orgsData?.content || [], [orgsData?.content])
  const totalElements = orgsData?.totalElements || 0
  const totalPages = orgsData?.totalPages || 0

  // Define handlers with useCallback
  const onDelete = useCallback(async (id: string) => {
    await deleteMutation.mutateAsync(id)
    setDeleteConfirm(null)
    refetch()
  }, [deleteMutation, refetch])

  const onActivate = useCallback(async (id: string) => {
    const reason = prompt("Enter reason for activation:")
    if (!reason) return
    await activateMutation.mutateAsync({ id, reason })
    refetch()
  }, [activateMutation, refetch])

  const onSuspend = useCallback(async (id: string) => {
    const reason = prompt("Enter reason for suspension:")
    if (!reason) return
    await suspendMutation.mutateAsync({ id, reason })
    refetch()
  }, [suspendMutation, refetch])

  const onArchive = useCallback(async (id: string) => {
    const reason = prompt("Enter reason for archiving:")
    if (!reason) return
    await archiveMutation.mutateAsync({ id, reason })
    refetch()
  }, [archiveMutation, refetch])

  const handleAdvancedSearch = useCallback((filters: Record<string, string>) => {
    console.log('Advanced search filters:', filters)
    toast.success('Filters applied', {
      description: `${Object.keys(filters).length} filter(s) active`
    })
  }, [])

  // Filter organizations (client-side)
  const filteredOrganizations = useMemo(() => {
    let filtered = organizations

    // Filter by status
    if (statusFilter !== "ALL") {
      filtered = filtered.filter(org => org.status === statusFilter)
    }

    // Filter by search query
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase()
      filtered = filtered.filter(org =>
        org.name.toLowerCase().includes(query) ||
        org.code.toLowerCase().includes(query) ||
        org.description?.toLowerCase().includes(query) ||
        org.email?.toLowerCase().includes(query)
      )
    }

    return filtered
  }, [organizations, searchQuery, statusFilter])

  // Calculate statistics
  const statistics = useMemo(() => {
    const total = organizations.length
    const active = organizations.filter(org => org.status === "ACTIVE").length
    const withContact = organizations.filter(org => org.email || org.phone).length
    const deleted = organizations.filter(org => org.deleted).length
    
    return { total, active, withContact, deleted }
  }, [organizations])

  const handleCreate = () => {
    setSelectedOrg(null)
    setDialogOpen(true)
  }

  const handleEdit = (org: Organization) => {
    // Set organization for dialog
    setSelectedOrg(org)
    setDialogOpen(true)
  }

  const handleDialogClose = async (success: boolean) => {
    setDialogOpen(false)
    setSelectedOrg(null)
    if (success) {
      await refetch()
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

  if (error) {
    return (
      <div className="space-y-4">
        <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
          <h1 className="text-2xl font-heading">Organizations</h1>
        </header>
        <div className="border-4 border-black bg-background p-12 shadow-[8px_8px_0_#000] text-center">
          <p className="text-lg font-base text-red-500">Error loading organizations: {(error as Error).message}</p>
          <Button onClick={() => refetch()} className="mt-4">Retry</Button>
        </div>
      </div>
    )
  }

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
          <PermissionGuard routeOrCode="/organizations" permission="canCreate">
            <Button
              onClick={handleCreate}
              className="bg-background text-foreground border-2 border-black hover:translate-x-1 hover:translate-y-1 transition-all shadow-[4px_4px_0_#000]"
            >
              <Plus className="h-4 w-4 mr-2" />
              Add Organization
            </Button>
          </PermissionGuard>
        </div>
      </header>

      {/* Statistics */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        {[
          { label: "Total", value: statistics.total, icon: Building2, color: "bg-main" },
          { label: "Active", value: statistics.active, icon: PlayCircle, color: "bg-green-500" },
          { label: "With Contact", value: statistics.withContact, icon: Mail, color: "bg-blue-500" },
          { label: "Deleted", value: statistics.deleted, icon: Archive, color: "bg-red-500" },
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
        })}
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

          <Button
            variant="noShadow"
            size="sm"
            onClick={() => setAdvancedSearchOpen(true)}
            disabled={isLoading}
          >
            <Filter className="h-4 w-4 mr-2" />
            Advanced Search
          </Button>

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
      {isLoading ? (
        <div className="border-4 border-black bg-background p-12 shadow-[8px_8px_0_#000] text-center">
          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
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
          <PermissionGuard routeOrCode="/organizations" permission="canCreate">
            {!searchQuery && statusFilter === "ALL" && (
              <Button onClick={handleCreate} className="border-2 border-black shadow-[4px_4px_0_#000]">
                <Plus className="h-4 w-4 mr-2" />
                Create Organization
              </Button>
            )}
          </PermissionGuard>
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
                  <div>Created by {org.createdBy}</div>
                </div>
              </div>

              {/* Card Actions */}
              <div className="border-t-4 border-black p-2 flex gap-2 flex-wrap">
                <PermissionGuard routeOrCode="/organizations" permission="canEdit">
                  <Button
                    size="sm"
                    variant="noShadow"
                    onClick={() => handleEdit(org)}
                    className="border-2 border-black"
                  >
                    <Pencil className="h-3 w-3 mr-1" />
                    Edit
                  </Button>
                </PermissionGuard>

                {/* Status Actions */}
                <PermissionGuard routeOrCode="/organizations" permission="canEdit">
                  {org.status !== "ACTIVE" && (
                    <Button
                      size="sm"
                      variant="noShadow"
                      onClick={() => onActivate(org.id)}
                      disabled={activateMutation.isPending}
                      className="border-2 border-black bg-green-500 text-white hover:bg-green-600"
                      title="Activate"
                    >
                      {activateMutation.isPending ? (
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
                      onClick={() => onSuspend(org.id)}
                      disabled={suspendMutation.isPending}
                      className="border-2 border-black bg-yellow-500 text-white hover:bg-yellow-600"
                      title="Suspend"
                    >
                      {suspendMutation.isPending ? (
                        <Loader2 className="h-3 w-3 animate-spin" />
                      ) : (
                        <PauseCircle className="h-3 w-3" />
                      )}
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="noShadow"
                    onClick={() => onArchive(org.id)}
                    disabled={archiveMutation.isPending}
                    className="border-2 border-black bg-blue-500 text-white hover:bg-blue-600"
                    title="Archive"
                  >
                    {archiveMutation.isPending ? (
                      <Loader2 className="h-3 w-3 animate-spin" />
                    ) : (
                      <Archive className="h-3 w-3" />
                    )}
                  </Button>
                </PermissionGuard>

                {/* Delete */}
                <PermissionGuard routeOrCode="/organizations" permission="canDelete">
                  {deleteConfirm === org.id ? (
                    <>
                      <Button
                        size="sm"
                        variant="noShadow"
                        onClick={() => onDelete(org.id)}
                        disabled={deleteMutation.isPending}
                        className="border-2 border-black bg-red-500 text-white hover:bg-red-600"
                        title="Confirm Delete"
                      >
                        {deleteMutation.isPending ? (
                          <Loader2 className="h-3 w-3 animate-spin" />
                        ) : (
                          "✓"
                        )}
                      </Button>
                      <Button
                        size="sm"
                        variant="noShadow"
                        onClick={() => setDeleteConfirm(null)}
                        disabled={deleteMutation.isPending}
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
                      className="border-2 border-black bg-red-500 text-white hover:bg-red-600"
                      title="Delete"
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  )}
                </PermissionGuard>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
          <div className="flex items-center justify-between">
            <div className="text-sm font-base text-foreground/70">
              Showing {pagination.pageIndex * pagination.pageSize + 1} to {Math.min((pagination.pageIndex + 1) * pagination.pageSize, totalElements)} of {totalElements} organizations
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="noShadow"
                size="sm"
                onClick={() => setPagination({ ...pagination, pageIndex: 0 })}
                disabled={pagination.pageIndex === 0}
                className="border-2 border-black"
              >
                First
              </Button>
              <Button
                variant="noShadow"
                size="sm"
                onClick={() => setPagination({ ...pagination, pageIndex: pagination.pageIndex - 1 })}
                disabled={pagination.pageIndex === 0}
                className="border-2 border-black"
              >
                Previous
              </Button>
              <span className="px-3 py-1 border-2 border-black bg-background font-base">
                {pagination.pageIndex + 1} of {totalPages}
              </span>
              <Button
                variant="noShadow"
                size="sm"
                onClick={() => setPagination({ ...pagination, pageIndex: pagination.pageIndex + 1 })}
                disabled={pagination.pageIndex >= totalPages - 1}
                className="border-2 border-black"
              >
                Next
              </Button>
              <Button
                variant="noShadow"
                size="sm"
                onClick={() => setPagination({ ...pagination, pageIndex: totalPages - 1 })}
                disabled={pagination.pageIndex >= totalPages - 1}
                className="border-2 border-black"
              >
                Last
              </Button>
            </div>
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

      {/* Advanced Search Dialog */}
      <AdvancedSearchDialog
        open={advancedSearchOpen}
        onOpenChange={setAdvancedSearchOpen}
        filters={organizationSearchFilters}
        onSearch={handleAdvancedSearch}
        title="Advanced Organization Search"
      />
    </div>
  )
}
