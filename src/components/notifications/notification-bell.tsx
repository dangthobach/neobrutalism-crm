/**
 * Notification Bell Component
 * High-performance bell icon with badge for 50K CCU
 * 
 * Features:
 * - Optimized re-renders with memo
 * - Smooth animations
 * - Accessible ARIA labels
 * - Visual feedback
 */

"use client"

import React, { memo } from "react"
import { Bell } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  DropdownMenu,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { useUnreadCount } from "@/hooks/useNotifications"
import { NotificationDropdown } from "@/components/notifications/notification-dropdown"

interface NotificationBellProps {
  className?: string
}

/**
 * Memoized badge component to prevent unnecessary re-renders
 */
const UnreadBadge = memo(({ count }: { count: number }) => {
  if (count === 0) return null

  const displayCount = count > 99 ? '99+' : count.toString()

  return (
    <Badge
      className="absolute -top-1 -right-1 h-5 min-w-[20px] px-1 bg-red-500 border-2 border-black text-white text-xs font-bold flex items-center justify-center animate-in zoom-in-50"
    >
      {displayCount}
    </Badge>
  )
})

UnreadBadge.displayName = 'UnreadBadge'

/**
 * Main Notification Bell Component
 */
export const NotificationBell = memo(({ className }: NotificationBellProps) => {
  const unreadCount = useUnreadCount()
  const [open, setOpen] = React.useState(false)

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="noShadow"
          size="sm"
          className={`relative ${className}`}
          aria-label={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ''}`}
        >
          <Bell className={`h-5 w-5 ${unreadCount > 0 ? 'animate-pulse' : ''}`} />
          <UnreadBadge count={unreadCount} />
        </Button>
      </DropdownMenuTrigger>
      
      <NotificationDropdown open={open} onOpenChange={setOpen} />
    </DropdownMenu>
  )
})

NotificationBell.displayName = 'NotificationBell'
