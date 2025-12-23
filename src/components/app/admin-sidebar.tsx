"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { 
  Users, 
  Shield, 
  UserCircle, 
  Building2, 
  Menu as MenuIcon,
  Grid,
  Key,
  Layout,
  List,
  Database
} from "lucide-react"
import { cn } from "@/lib/utils"

const adminLinks = [
  {
    title: "User Management",
    items: [
      {
        href: "/admin/users",
        label: "Users",
        icon: Users,
        description: "Manage user accounts"
      },
      {
        href: "/admin/roles",
        label: "Roles",
        icon: Shield,
        description: "Manage roles & permissions"
      },
      {
        href: "/admin/groups",
        label: "Groups",
        icon: UserCircle,
        description: "Manage user groups"
      },
      {
        href: "/admin/organizations",
        label: "Organizations",
        icon: Building2,
        description: "Manage organizations"
      }
    ]
  },
  {
    title: "Permission System",
    items: [
      {
        href: "/admin/permissions/users",
        label: "User Permissions",
        icon: Key,
        description: "Assign permissions to users"
      },
      {
        href: "/admin/permissions/roles",
        label: "Role Permissions",
        icon: Shield,
        description: "Configure role permissions"
      }
    ]
  },
  {
    title: "Menu Management",
    items: [
      {
        href: "/admin/menus",
        label: "Menus",
        icon: MenuIcon,
        description: "Manage menu structure"
      },
      {
        href: "/admin/menu-tabs",
        label: "Menu Tabs",
        icon: Grid,
        description: "Manage menu tabs"
      },
      {
        href: "/admin/menu-screens",
        label: "Menu Screens",
        icon: Layout,
        description: "Manage menu screens"
      },
      {
        href: "/admin/api-endpoints",
        label: "API Endpoints",
        icon: Database,
        description: "Manage API endpoints"
      }
    ]
  }
]

export function AdminSidebar() {
  const pathname = usePathname()

  return (
    <aside className="fixed left-0 top-[70px] h-[calc(100vh-70px)] w-64 border-r-4 border-border bg-secondary-background overflow-y-auto">
      <div className="p-4 space-y-6">
        {adminLinks.map((section) => (
          <div key={section.title} className="space-y-2">
            <h3 className="font-heading text-sm uppercase tracking-wide text-muted-foreground px-2">
              {section.title}
            </h3>
            <div className="space-y-1">
              {section.items.map((item) => {
                const Icon = item.icon
                const isActive = pathname === item.href || pathname.startsWith(item.href + "/")
                
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      "flex items-center gap-3 px-3 py-2 rounded-base border-2 transition-all font-base text-sm",
                      isActive
                        ? "bg-main text-main-foreground border-black shadow-[4px_4px_0_#000]"
                        : "bg-background border-border hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-[2px_2px_0_#000]"
                    )}
                    title={item.description}
                  >
                    <Icon className="h-4 w-4 shrink-0" />
                    <span>{item.label}</span>
                  </Link>
                )
              })}
            </div>
          </div>
        ))}
      </div>
    </aside>
  )
}
