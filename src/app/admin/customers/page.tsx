"use client"

import { useState, useCallback } from "react"
import Link from "next/link"
import { Plus, TrendingUp, Users, DollarSign, Star, Filter, X } from "lucide-react"
import {
  useCustomers,
  useCustomerStats,
  useDeleteCustomer
} from "@/hooks/use-customers"
import { CustomerTable } from "@/components/customer"
import { CustomerType, CustomerStatus } from "@/types/customer"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Card } from "@/components/ui/card"
import { PermissionGuard } from "@/components/auth/permission-guard"
import { usePermission } from "@/hooks/usePermission"
import { AdvancedSearchDialog, customerSearchFilters } from "@/components/ui/advanced-search-dialog"
import { toast } from "sonner"

export default function CustomersPage() {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [keyword, setKeyword] = useState("")
  const [tempKeyword, setTempKeyword] = useState("")
  const [type, setType] = useState<CustomerType | "ALL">("ALL")
  const [advancedSearchOpen, setAdvancedSearchOpen] = useState(false)
  const [advancedFilters, setAdvancedFilters] = useState<Record<string, string>>({})
  const [status, setStatus] = useState<CustomerStatus | "ALL">("ALL")
  const [sortBy, setSortBy] = useState<string>("name")
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("asc")

  // Check permissions
  const { canDelete } = usePermission()

  // Fetch customers
  const { data: customersData, isLoading, error, refetch } = useCustomers({
    page,
    size,
    keyword: keyword || undefined,
    customerType: type !== "ALL" ? type : undefined,
    status: status !== "ALL" ? status : undefined,
    sortBy,
    sortDirection,
  })

  // Fetch statistics
  const { data: stats } = useCustomerStats()

  // Delete mutation
  const deleteMutation = useDeleteCustomer()

  const customers = customersData?.content || []
  const totalElements = customersData?.totalElements || 0
  const totalPages = customersData?.totalPages || 0

  const handleSearch = () => {
    setKeyword(tempKeyword)
    setPage(0)
  }

  const handleAdvancedSearch = useCallback((filters: Record<string, string>) => {
    setAdvancedFilters(filters)
    setPage(0)
    const filterCount = Object.keys(filters).length
    toast.success(`Applied ${filterCount} advanced filter${filterCount !== 1 ? 's' : ''}`)
  }, [])

  const handleClearFilters = () => {
    setTempKeyword("")
    setKeyword("")
    setType("ALL")
    setStatus("ALL")
    setSortBy("name")
    setSortDirection("asc" as "asc" | "desc")
    setAdvancedFilters({})
    setPage(0)
  }

  const handleDelete = (id: string) => {
    if (confirm("Are you sure you want to delete this customer?")) {
      deleteMutation.mutate(id, {
        onSuccess: () => {
          refetch()
        }
      })
    }
  }

  // Calculate statistics from stats data
  const activeCount = stats?.byStatus?.ACTIVE || 0
  const prospectCount = stats?.byStatus?.PROSPECT || 0
  const totalRevenue = stats?.averageRevenue ? stats.averageRevenue * stats.total : 0
  const vipCount = stats?.vipCount || 0

  return (
    <div className="container mx-auto p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-heading text-4xl font-black uppercase">
            Customers
          </h1>
          <p className="text-gray-600 mt-1">
            Manage your customer database
          </p>
        </div>
        <PermissionGuard routeOrCode="/customers" permission="canCreate">
          <Link href="/admin/customers/new">
            <Button size="lg">
              <Plus className="mr-2 h-5 w-5" />
              Add Customer
            </Button>
          </Link>
        </PermissionGuard>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-6 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-bold uppercase text-gray-600">
                Total Customers
              </p>
              <p className="text-3xl font-black mt-2">{totalElements}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-blue-200 border-2 border-black flex items-center justify-center">
              <Users className="h-6 w-6" />
            </div>
          </div>
        </Card>

        <Card className="p-6 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-bold uppercase text-gray-600">
                Active
              </p>
              <p className="text-3xl font-black mt-2">{activeCount}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-green-200 border-2 border-black flex items-center justify-center">
              <TrendingUp className="h-6 w-6" />
            </div>
          </div>
        </Card>

        <Card className="p-6 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-bold uppercase text-gray-600">
                Total Revenue
              </p>
              <p className="text-3xl font-black mt-2">
                ${(totalRevenue / 1000).toFixed(0)}K
              </p>
            </div>
            <div className="h-12 w-12 rounded-full bg-yellow-200 border-2 border-black flex items-center justify-center">
              <DollarSign className="h-6 w-6" />
            </div>
          </div>
        </Card>

        <Card className="p-6 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-bold uppercase text-gray-600">
                VIP Customers
              </p>
              <p className="text-3xl font-black mt-2">{vipCount}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-purple-200 border-2 border-black flex items-center justify-center">
              <Star className="h-6 w-6" />
            </div>
          </div>
        </Card>
      </div>

      {/* Filters */}
      <Card className="p-6 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-heading text-xl font-black uppercase flex items-center">
              <Filter className="mr-2 h-5 w-5" />
              Filters
            </h2>
            <div className="flex gap-2">
              <Button
                variant="neutral"
                size="sm"
                onClick={() => setAdvancedSearchOpen(true)}
                disabled={isLoading}
                className="font-bold"
              >
                <Filter className="mr-1 h-4 w-4" />
                Advanced Search
              </Button>
              <Button
                variant="neutral"
                size="sm"
                onClick={handleClearFilters}
                className="font-bold"
              >
                <X className="mr-1 h-4 w-4" />
                Clear
              </Button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
            {/* Search */}
            <div className="lg:col-span-2">
              <Input
                placeholder="Search customers..."
                value={tempKeyword}
                onChange={(e) => setTempKeyword(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleSearch()
                }}
                className="h-11"
              />
            </div>

            {/* Type Filter */}
            <Select value={type} onValueChange={(value) => setType(value as CustomerType | "ALL")}>
              <SelectTrigger className="h-11">
                <SelectValue placeholder="Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Types</SelectItem>
                <SelectItem value="INDIVIDUAL">Individual</SelectItem>
                <SelectItem value="BUSINESS">Business</SelectItem>
              </SelectContent>
            </Select>

            {/* Status Filter */}
            <Select value={status} onValueChange={(value) => setStatus(value as CustomerStatus | "ALL")}>
              <SelectTrigger className="h-11">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Status</SelectItem>
                <SelectItem value="PROSPECT">Prospect</SelectItem>
                <SelectItem value="ACTIVE">Active</SelectItem>
                <SelectItem value="INACTIVE">Inactive</SelectItem>
                <SelectItem value="VIP">VIP</SelectItem>
                <SelectItem value="BLACKLISTED">Blacklisted</SelectItem>
              </SelectContent>
            </Select>

            {/* Search Button */}
            <Button onClick={handleSearch} className="h-11 font-bold">
              Search
            </Button>
          </div>
        </div>
      </Card>

      {/* Table */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <CustomerTable
          customers={customers}
          onDelete={canDelete("/customers") ? handleDelete : undefined}
          isLoading={isLoading}
        />

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t-2 border-black p-4">
            <div className="text-sm text-gray-600">
              Page {page + 1} of {totalPages} ({totalElements} total)
            </div>
            <div className="flex gap-2">
              <Button
                variant="neutral"
                size="sm"
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="font-bold"
              >
                Previous
              </Button>
              <Button
                variant="neutral"
                size="sm"
                onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="font-bold"
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Error State */}
      {error && (
        <Card className="p-6 border-2 border-red-500 bg-red-50">
          <p className="text-red-600 font-bold">
            Error loading customers: {error.message}
          </p>
        </Card>
      )}

      <AdvancedSearchDialog
        open={advancedSearchOpen}
        onOpenChange={setAdvancedSearchOpen}
        filters={customerSearchFilters}
        onSearch={handleAdvancedSearch}
        title="Advanced Customer Search"
      />
    </div>
  )
}
