"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useState } from "react"
import { LayoutDashboard, Users, Shield, Lock, Building2, Menu, ChevronLeft, LogOut } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { ProtectedRoute } from "@/components/auth/protected-route"
import { useAuth } from "@/contexts/auth-context"

const menuItems = [
  { href: "/admin", icon: LayoutDashboard, label: "Dashboard" },
  { href: "/admin/organizations", icon: Building2, label: "Organizations" },
  { href: "/admin/users", icon: Users, label: "Users" },
  { href: "/admin/roles", icon: Shield, label: "Roles" },
  { href: "/admin/permissions", icon: Lock, label: "Permissions" },
]

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const [collapsed, setCollapsed] = useState(false)
  const pathname = usePathname()
  const { user, logout } = useAuth()

  return (
    <ProtectedRoute>
      <div className="flex gap-4 px-6 pt-[90px] pb-6 min-h-[calc(100vh-70px)] bg-secondary-background">
        <aside className={cn(
          "border-4 border-black bg-background shadow-[8px_8px_0_#000] transition-all duration-300 flex flex-col h-fit sticky top-6",
          collapsed ? "w-20" : "w-64"
        )}>
          <div className="p-4 flex items-center justify-between border-b-4 border-black">
            {!collapsed && <h2 className="text-2xl font-heading">Management</h2>}
            <Button
              variant="noShadow"
              size="sm"
              onClick={() => setCollapsed(!collapsed)}
              className="ml-auto"
            >
              {collapsed ? <Menu className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
            </Button>
          </div>
          <nav className="flex flex-col gap-2 p-3 font-base flex-1">
            {menuItems.map((item) => {
              const Icon = item.icon
              const isActive = pathname === item.href
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "border-2 border-black px-3 py-2 flex items-center gap-3 hover:translate-x-1 hover:translate-y-1 transition-all",
                    isActive ? "bg-main text-main-foreground" : "bg-background",
                    collapsed && "justify-center"
                  )}
                  title={collapsed ? item.label : undefined}
                >
                  <Icon className="h-5 w-5 flex-shrink-0" />
                  {!collapsed && <span>{item.label}</span>}
                </Link>
              )
            })}
          </nav>
          <div className="p-3 border-t-4 border-black">
            <div className="flex items-center gap-3 mb-3">
              {!collapsed && (
                <div className="flex-1">
                  <p className="text-sm font-base font-semibold">{user?.fullName || user?.username}</p>
                  <p className="text-xs font-base text-foreground/70">{user?.email}</p>
                </div>
              )}
              <Button
                variant="noShadow"
                size="sm"
                onClick={logout}
                className="bg-red-500 text-white border-2 border-black hover:translate-x-1 hover:translate-y-1 transition-all"
                title="Logout"
              >
                <LogOut className="h-4 w-4" />
                {!collapsed && <span className="ml-2">Logout</span>}
              </Button>
            </div>
          </div>
        </aside>
        <main className="flex-1 space-y-4 pb-6">{children}</main>
      </div>
    </ProtectedRoute>
  )
}


