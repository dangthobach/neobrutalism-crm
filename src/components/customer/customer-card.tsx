/**
 * Customer Card Component
 * Displays customer information in a card with Neobrutalism design
 */

'use client'

import Link from 'next/link'
import { Building2, Mail, Phone, MapPin, TrendingUp, Users } from 'lucide-react'
import { Customer, CustomerType } from '@/types/customer'
import { CustomerStatusBadge } from './customer-status-badge'
import { formatCurrency, formatDate } from '@/lib/utils'

interface CustomerCardProps {
  customer: Customer
  showActions?: boolean
  onClick?: () => void
}

export function CustomerCard({ customer, showActions = true, onClick }: CustomerCardProps) {
  const getTypeColor = (type: CustomerType) => {
    switch (type) {
      case CustomerType.B2B:
        return 'bg-purple-200'
      case CustomerType.B2C:
        return 'bg-blue-200'
      case CustomerType.PARTNER:
        return 'bg-green-200'
      case CustomerType.RESELLER:
        return 'bg-yellow-200'
      case CustomerType.VENDOR:
        return 'bg-orange-200'
      case CustomerType.PROSPECT:
        return 'bg-pink-200'
      default:
        return 'bg-gray-200'
    }
  }

  const content = (
    <>
      {/* Header */}
      <div className={`flex items-center justify-between border-b-2 border-black ${getTypeColor(customer.customerType)} px-4 py-3`}>
        <div className="flex items-center gap-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-full border-2 border-black bg-white">
            <Building2 className="h-5 w-5" />
          </div>
          <div>
            <h3 className="font-black uppercase leading-tight">{customer.companyName}</h3>
            <p className="text-xs font-bold uppercase text-gray-600">{customer.code}</p>
          </div>
        </div>
        <CustomerStatusBadge status={customer.status} />
      </div>

      {/* Content */}
      <div className="space-y-3 p-4">
        {/* Type & VIP */}
        <div className="flex items-center gap-2">
          <span className="rounded-full border-2 border-black bg-white px-2 py-1 text-xs font-black uppercase">
            {customer.customerType}
          </span>
          {customer.isVip && (
            <span className="rounded-full border-2 border-black bg-yellow-400 px-2 py-1 text-xs font-black uppercase">
              ⭐ VIP
            </span>
          )}
        </div>

        {/* Contact Info */}
        {(customer.email || customer.phone) && (
          <div className="space-y-1">
            {customer.email && (
              <div className="flex items-center gap-2 text-sm">
                <Mail className="h-4 w-4 text-gray-500" />
                <span className="font-medium">{customer.email}</span>
              </div>
            )}
            {customer.phone && (
              <div className="flex items-center gap-2 text-sm">
                <Phone className="h-4 w-4 text-gray-500" />
                <span className="font-medium">{customer.phone}</span>
              </div>
            )}
          </div>
        )}

        {/* Location */}
        {(customer.city || customer.country) && (
          <div className="flex items-center gap-2 text-sm">
            <MapPin className="h-4 w-4 text-gray-500" />
            <span className="font-medium">
              {[customer.city, customer.state, customer.country].filter(Boolean).join(', ')}
            </span>
          </div>
        )}

        {/* Stats */}
        <div className="grid grid-cols-2 gap-2 pt-2">
          {customer.annualRevenue && (
            <div className="rounded border-2 border-black bg-green-100 p-2">
              <div className="flex items-center gap-1">
                <TrendingUp className="h-4 w-4 text-green-700" />
                <span className="text-xs font-bold uppercase text-gray-600">Revenue</span>
              </div>
              <p className="font-black text-green-900">{formatCurrency(customer.annualRevenue)}</p>
            </div>
          )}
          {customer.employeeCount && (
            <div className="rounded border-2 border-black bg-blue-100 p-2">
              <div className="flex items-center gap-1">
                <Users className="h-4 w-4 text-blue-700" />
                <span className="text-xs font-bold uppercase text-gray-600">Employees</span>
              </div>
              <p className="font-black text-blue-900">{customer.employeeCount}</p>
            </div>
          )}
        </div>

        {/* Industry & Owner */}
        {(customer.industry || customer.ownerName) && (
          <div className="space-y-1 border-t-2 border-black pt-2 text-xs">
            {customer.industry && (
              <div>
                <span className="font-bold uppercase text-gray-500">Industry: </span>
                <span className="font-bold">{customer.industry}</span>
              </div>
            )}
            {customer.ownerName && (
              <div>
                <span className="font-bold uppercase text-gray-500">Owner: </span>
                <span className="font-bold">{customer.ownerName}</span>
              </div>
            )}
          </div>
        )}

        {/* Dates */}
        <div className="space-y-1 text-xs text-gray-600">
          {customer.lastContactDate && (
            <div>
              <span className="font-bold uppercase">Last Contact: </span>
              <span className="font-medium">{formatDate(customer.lastContactDate)}</span>
            </div>
          )}
          {customer.nextFollowupDate && (
            <div>
              <span className="font-bold uppercase">Next Follow-up: </span>
              <span className="font-medium">{formatDate(customer.nextFollowupDate)}</span>
            </div>
          )}
        </div>
      </div>
    </>
  )

  if (onClick) {
    return (
      <div
        onClick={onClick}
        className="group block cursor-pointer transform border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all duration-200 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
      >
        {content}
      </div>
    )
  }

  return (
    <Link
      href={`/admin/customers/${customer.id}`}
      className="group block transform border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all duration-200 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
    >
      {content}
    </Link>
  )
}
