/**
 * ✅ PHASE 1.3: Contacts Management Page
 * 
 * Features:
 * - List all contacts with pagination
 * - Filter by customer, owner, type, status
 * - Search by name/email
 * - Primary contact indicator
 * - Create/Edit/Delete contacts
 * - Link to customer details
 */

"use client"

import React from "react"
import Link from "next/link"
import { ColumnDef } from "@tanstack/react-table"
import { GenericDataTable } from "@/components/ui/generic-data-table"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { 
  MoreHorizontal, 
  UserPlus, 
  ArrowUpDown,
  Mail,
  Phone,
  Building2,
  Star,
  Filter,
  X,
} from "lucide-react"
import { useContacts } from "@/hooks/use-contacts"
import { Contact, ContactRole, ContactStatus } from "@/types/contact"
import { Card } from "@/components/ui/card"
import { PermissionGuard } from "@/components/auth/permission-guard"
import { usePermission } from "@/hooks/usePermission"

export default function ContactsPage() {
  const [page, setPage] = React.useState(0)
  const [size, setSize] = React.useState(20)
  const [keyword, setKeyword] = React.useState("")
  const [tempKeyword, setTempKeyword] = React.useState("")
  const [contactRole, setContactRole] = React.useState<ContactRole | "ALL">("ALL")
  const [status, setStatus] = React.useState<ContactStatus | "ALL">("ALL")
  const [sortBy, setSortBy] = React.useState<string>("fullName")
  const [sortDirection, setSortDirection] = React.useState<"asc" | "desc">("asc")
  
  // Check permissions
  const { canCreate, canEdit, canDelete } = usePermission()

  // Fetch contacts with pagination
  const { data, isLoading, error, refetch } = useContacts({
    page,
    size,
    keyword: keyword || undefined,
    role: contactRole !== "ALL" ? contactRole : undefined,
    status: status !== "ALL" ? status : undefined,
    sortBy,
    sortDirection,
  })

  /**
   * Get contact role badge
   */
  const getContactRoleBadge = (role: ContactRole) => {
    const variants: Record<ContactRole, { color: string; label: string }> = {
      DECISION_MAKER: { color: "bg-purple-500", label: "Decision Maker" },
      INFLUENCER: { color: "bg-blue-500", label: "Influencer" },
      CHAMPION: { color: "bg-green-500", label: "Champion" },
      EVALUATOR: { color: "bg-orange-500", label: "Evaluator" },
      GATEKEEPER: { color: "bg-red-500", label: "Gatekeeper" },
      USER: { color: "bg-cyan-500", label: "User" },
      OTHER: { color: "bg-gray-500", label: "Other" },
    }
    const variant = variants[role]
    return (
      <Badge className={variant.color}>
        {variant.label}
      </Badge>
    )
  }

  /**
   * Get status badge
   */
  const getStatusBadge = (status: ContactStatus) => {
    switch (status) {
      case "ACTIVE":
        return <Badge variant="default" className="bg-green-500">Active</Badge>
      case "INACTIVE":
        return <Badge variant="neutral">Inactive</Badge>
      default:
        return <Badge variant="neutral">{status}</Badge>
    }
  }

  /**
   * Table columns definition
   */
  const columns: ColumnDef<Contact>[] = [
    {
      accessorKey: "fullName",
      header: ({ column }) => {
        return (
          <Button
            variant="noShadow"
            onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
          >
            Contact Name
            <ArrowUpDown className="ml-2 h-4 w-4" />
          </Button>
        )
      },
      cell: ({ row }) => {
        const contact = row.original
        return (
          <div className="flex items-center gap-2">
            {contact.isPrimary && (
              <Star className="h-4 w-4 text-yellow-500 fill-yellow-500" />
            )}
            <div className="flex flex-col">
              <span className="font-medium">{contact.fullName}</span>
              <span className="text-sm text-muted-foreground">{contact.title || "No title"}</span>
            </div>
          </div>
        )
      },
    },
    {
      accessorKey: "customerName",
      header: "Customer",
      cell: ({ row }) => {
        const contact = row.original
        return contact.customerName ? (
          <Link 
            href={`/admin/customers/${contact.customerId}`}
            className="flex items-center gap-2 text-blue-600 hover:underline"
          >
            <Building2 className="h-4 w-4" />
            {contact.customerName}
          </Link>
        ) : (
          <span className="text-muted-foreground">No customer</span>
        )
      },
    },
    {
      accessorKey: "email",
      header: "Contact Info",
      cell: ({ row }) => {
        const contact = row.original
        return (
          <div className="flex flex-col gap-1">
            {contact.email && (
              <div className="flex items-center gap-2 text-sm">
                <Mail className="h-3 w-3 text-muted-foreground" />
                <a href={`mailto:${contact.email}`} className="text-blue-600 hover:underline">
                  {contact.email}
                </a>
              </div>
            )}
            {contact.phone && (
              <div className="flex items-center gap-2 text-sm">
                <Phone className="h-3 w-3 text-muted-foreground" />
                <a href={`tel:${contact.phone}`} className="text-blue-600 hover:underline">
                  {contact.phone}
                </a>
              </div>
            )}
          </div>
        )
      },
    },
    {
      accessorKey: "role",
      header: "Role",
      cell: ({ row }) => getContactRoleBadge(row.original.role),
    },
    {
      accessorKey: "status",
      header: "Status",
      cell: ({ row }) => getStatusBadge(row.original.status),
    },
    {
      accessorKey: "createdAt",
      header: ({ column }) => {
        return (
          <Button
            variant="noShadow"
            onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
          >
            Created At
            <ArrowUpDown className="ml-2 h-4 w-4" />
          </Button>
        )
      },
      cell: ({ row }) => {
        const date = new Date(row.original.createdAt)
        return date.toLocaleDateString()
      },
    },
    {
      id: "actions",
      cell: ({ row }) => {
        const contact = row.original

        return (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="noShadow" className="h-8 w-8 p-0">
                <span className="sr-only">Open menu</span>
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuLabel>Actions</DropdownMenuLabel>
              <DropdownMenuItem
                onClick={() => navigator.clipboard.writeText(contact.id.toString())}
              >
                Copy contact ID
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem>View details</DropdownMenuItem>
              <DropdownMenuItem>Edit contact</DropdownMenuItem>
              {!contact.isPrimary && (
                <DropdownMenuItem>Set as primary</DropdownMenuItem>
              )}
              <DropdownMenuSeparator />
              <DropdownMenuItem className="text-red-600">
                Delete contact
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )
      },
    },
  ]

  /**
   * Handle pagination change
   */
  const handlePaginationChange = (newPage: number, newSize: number) => {
    setPage(newPage)
    setSize(newSize)
  }

  /**
   * Handle search
   */
  const handleSearch = () => {
    setKeyword(tempKeyword)
    setPage(0)
  }

  /**
   * Handle clear filters
   */
  const handleClearFilters = () => {
    setTempKeyword("")
    setKeyword("")
    setContactRole("ALL")
    setStatus("ALL")
    setSortBy("fullName")
    setSortDirection("asc")
    setPage(0)
  }

  /**
   * Check if filters are active
   */
  const hasActiveFilters = keyword || contactRole !== "ALL" || status !== "ALL"

  /**
   * Handle error state
   */
  if (error) {
    return (
      <div className="container mx-auto py-10">
        <div className="rounded-lg border border-red-200 bg-red-50 p-4">
          <h3 className="text-lg font-semibold text-red-800">Error loading contacts</h3>
          <p className="text-sm text-red-600">{error.message}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="container mx-auto py-10">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Contacts</h1>
          <p className="text-muted-foreground">
            Manage customer contacts and communication
          </p>
        </div>
        <Link href="/admin/contacts/new">
          <Button>
            <UserPlus className="mr-2 h-4 w-4" />
            Add Contact
          </Button>
        </Link>
      </div>

      {/* Filters Card */}
      <Card className="mb-6 p-4">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-muted-foreground" />
            <span className="text-sm font-medium">Filters</span>
          </div>
          
          {/* Search */}
          <div className="flex-1">
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="Search by name or email..."
                value={tempKeyword}
                onChange={(e) => setTempKeyword(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                className="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              <Button onClick={handleSearch} size="sm">
                Search
              </Button>
            </div>
          </div>

          {/* Role Filter */}
          <Select
            value={contactRole}
            onValueChange={(value) => {
              setContactRole(value as ContactRole | "ALL")
              setPage(0)
            }}
          >
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="Role" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Roles</SelectItem>
              <SelectItem value="DECISION_MAKER">Decision Maker</SelectItem>
              <SelectItem value="INFLUENCER">Influencer</SelectItem>
              <SelectItem value="CHAMPION">Champion</SelectItem>
              <SelectItem value="EVALUATOR">Evaluator</SelectItem>
              <SelectItem value="GATEKEEPER">Gatekeeper</SelectItem>
              <SelectItem value="USER">User</SelectItem>
              <SelectItem value="OTHER">Other</SelectItem>
            </SelectContent>
          </Select>

          {/* Status Filter */}
          <Select
            value={status}
            onValueChange={(value) => {
              setStatus(value as ContactStatus | "ALL")
              setPage(0)
            }}
          >
            <SelectTrigger className="w-[150px]">
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Status</SelectItem>
              <SelectItem value="ACTIVE">Active</SelectItem>
              <SelectItem value="INACTIVE">Inactive</SelectItem>
            </SelectContent>
          </Select>

          {/* Clear Filters */}
          {hasActiveFilters && (
            <Button
              variant="noShadow"
              size="sm"
              onClick={handleClearFilters}
            >
              <X className="mr-2 h-4 w-4" />
              Clear
            </Button>
          )}
        </div>
      </Card>

      {/* Data Table */}
      <GenericDataTable
        columns={columns}
        data={data?.content || []}
        loading={isLoading}
        pageIndex={page}
        pageSize={size}
        totalItems={data?.totalElements || 0}
        onPaginationChange={handlePaginationChange}
        emptyMessage="No contacts found. Create your first contact to get started."
      />
    </div>
  )
}
