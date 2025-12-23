/**
 * Contact Card Component
 * Displays contact information in a card with Neobrutalism design
 */

'use client'

import Link from 'next/link'
import { User, Mail, Phone, Briefcase, Building2, Linkedin, Star } from 'lucide-react'
import { Contact } from '@/types/contact'
import { ContactRoleBadge } from './contact-role-badge'
import { ContactStatusBadge } from './contact-status-badge'

interface ContactCardProps {
  contact: Contact
  showCustomer?: boolean
  onClick?: () => void
}

export function ContactCard({ contact, showCustomer = true, onClick }: ContactCardProps) {
  const content = (
    <>
      {/* Header */}
      <div className="flex items-center justify-between border-b-2 border-black bg-green-200 px-4 py-3">
        <div className="flex items-center gap-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-full border-2 border-black bg-white">
            <User className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-black uppercase leading-tight">{contact.fullName}</h3>
              {contact.isPrimary && (
                <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
              )}
            </div>
            {contact.title && (
              <p className="text-xs font-bold text-gray-600">{contact.title}</p>
            )}
          </div>
        </div>
        <ContactStatusBadge status={contact.status} />
      </div>

      {/* Content */}
      <div className="space-y-3 p-4">
        {/* Role */}
        <div className="flex items-center gap-2">
          <ContactRoleBadge role={contact.role} />
          {contact.isPrimary && (
            <span className="rounded-full border-2 border-black bg-yellow-400 px-2 py-1 text-xs font-black uppercase">
              Primary
            </span>
          )}
        </div>

        {/* Customer */}
        {showCustomer && contact.customerName && (
          <div className="flex items-center gap-2 text-sm">
            <Building2 className="h-4 w-4 text-gray-500" />
            <span className="font-bold">{contact.customerName}</span>
          </div>
        )}

        {/* Department */}
        {contact.department && (
          <div className="flex items-center gap-2 text-sm">
            <Briefcase className="h-4 w-4 text-gray-500" />
            <span className="font-medium">{contact.department}</span>
          </div>
        )}

        {/* Contact Info */}
        <div className="space-y-1">
          {contact.email && (
            <div className="flex items-center gap-2 text-sm">
              <Mail className="h-4 w-4 text-gray-500" />
              <span className="font-medium">{contact.email}</span>
            </div>
          )}
          {contact.phone && (
            <div className="flex items-center gap-2 text-sm">
              <Phone className="h-4 w-4 text-gray-500" />
              <span className="font-medium">{contact.phone}</span>
            </div>
          )}
          {contact.mobile && (
            <div className="flex items-center gap-2 text-sm">
              <Phone className="h-4 w-4 text-gray-500" />
              <span className="font-medium">{contact.mobile} (Mobile)</span>
            </div>
          )}
          {contact.linkedinUrl && (
            <div className="flex items-center gap-2 text-sm">
              <Linkedin className="h-4 w-4 text-blue-500" />
              <a
                href={contact.linkedinUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="font-medium text-blue-600 hover:underline"
              >
                LinkedIn Profile
              </a>
            </div>
          )}
        </div>

        {/* Preferred Contact Method */}
        {contact.preferredContactMethod && (
          <div className="border-t-2 border-black pt-2 text-xs">
            <span className="font-bold uppercase text-gray-500">Preferred: </span>
            <span className="font-bold">{contact.preferredContactMethod}</span>
          </div>
        )}
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
      href={`/admin/contacts/${contact.id}`}
      className="group block transform border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all duration-200 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
    >
      {content}
    </Link>
  )
}
