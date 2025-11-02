/**
 * Customers List Page
 * Main page for viewing and managing customers
 */

'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { CustomerTable } from '@/components/customers/customer-table'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useCustomerStats } from '@/hooks/useCustomers'
import { Plus, Search, Users, TrendingUp, Star, DollarSign } from 'lucide-react'
import { CustomerStatus, CustomerType, CustomerSearchParams } from '@/types/customer'

export default function CustomersPage() {
  const router = useRouter()
  const [searchParams, setSearchParams] = useState<CustomerSearchParams>({})
  const { data: stats } = useCustomerStats()

  const handleSearch = (keyword: string) => {
    setSearchParams({ ...searchParams, keyword })
  }

  const handleStatusFilter = (status: string) => {
    if (status === 'all') {
      const { status: _, ...rest } = searchParams
      setSearchParams(rest)
    } else {
      setSearchParams({ ...searchParams, status: status as CustomerStatus })
    }
  }

  const handleTypeFilter = (type: string) => {
    if (type === 'all') {
      const { customerType: _, ...rest } = searchParams
      setSearchParams(rest)
    } else {
      setSearchParams({ ...searchParams, customerType: type as CustomerType })
    }
  }

  const handleVipFilter = (vip: string) => {
    if (vip === 'all') {
      const { isVip: _, ...rest } = searchParams
      setSearchParams(rest)
    } else {
      setSearchParams({ ...searchParams, isVip: vip === 'true' })
    }
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-4xl font-black mb-2">Customers</h1>
          <p className="text-gray-600 font-medium">
            Manage your customer relationships and track interactions
          </p>
        </div>
        <Button
          onClick={() => router.push('/admin/customers/new')}
          className="border-2 border-black"
        >
          <Plus className="h-4 w-4 mr-2" />
          Add Customer
        </Button>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-4 gap-4">
          <Card className="border-4 border-black">
            <CardHeader className="bg-blue-300 border-b-4 border-black pb-3">
              <CardTitle className="text-sm font-bold flex items-center gap-2">
                <Users className="h-4 w-4" />
                Total Customers
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-4">
              <div className="text-3xl font-black">{stats.total.toLocaleString()}</div>
            </CardContent>
          </Card>

          <Card className="border-4 border-black">
            <CardHeader className="bg-green-300 border-b-4 border-black pb-3">
              <CardTitle className="text-sm font-bold flex items-center gap-2">
                <TrendingUp className="h-4 w-4" />
                Active
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-4">
              <div className="text-3xl font-black">
                {stats.byStatus[CustomerStatus.ACTIVE]?.toLocaleString() || 0}
              </div>
            </CardContent>
          </Card>

          <Card className="border-4 border-black">
            <CardHeader className="bg-amber-300 border-b-4 border-black pb-3">
              <CardTitle className="text-sm font-bold flex items-center gap-2">
                <Star className="h-4 w-4" />
                VIP
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-4">
              <div className="text-3xl font-black">{stats.vipCount.toLocaleString()}</div>
            </CardContent>
          </Card>

          <Card className="border-4 border-black">
            <CardHeader className="bg-purple-300 border-b-4 border-black pb-3">
              <CardTitle className="text-sm font-bold flex items-center gap-2">
                <DollarSign className="h-4 w-4" />
                Avg Revenue
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-4">
              <div className="text-3xl font-black">
                ${Math.round(stats.averageRevenue).toLocaleString()}
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Filters */}
      <Card className="border-4 border-black">
        <CardContent className="pt-6">
          <div className="flex gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-500" />
              <Input
                placeholder="Search customers..."
                onChange={(e) => handleSearch(e.target.value)}
                className="pl-10 border-2 border-black"
              />
            </div>

            <Select onValueChange={handleStatusFilter} defaultValue="all">
              <SelectTrigger className="w-[180px] border-2 border-black">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent className="border-4 border-black">
                <SelectItem value="all">All Status</SelectItem>
                {Object.values(CustomerStatus).map((status) => (
                  <SelectItem key={status} value={status}>
                    {status}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select onValueChange={handleTypeFilter} defaultValue="all">
              <SelectTrigger className="w-[180px] border-2 border-black">
                <SelectValue placeholder="Type" />
              </SelectTrigger>
              <SelectContent className="border-4 border-black">
                <SelectItem value="all">All Types</SelectItem>
                {Object.values(CustomerType).map((type) => (
                  <SelectItem key={type} value={type}>
                    {type}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select onValueChange={handleVipFilter} defaultValue="all">
              <SelectTrigger className="w-[150px] border-2 border-black">
                <SelectValue placeholder="VIP" />
              </SelectTrigger>
              <SelectContent className="border-4 border-black">
                <SelectItem value="all">All</SelectItem>
                <SelectItem value="true">VIP Only</SelectItem>
                <SelectItem value="false">Non-VIP</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Customer Table */}
      <CustomerTable searchParams={searchParams} />
    </div>
  )
}
