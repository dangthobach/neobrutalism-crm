"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Search, Shield, Users, Menu as MenuIcon, ArrowRight, Loader2 } from "lucide-react"
import { useQuery } from "@tanstack/react-query"
import { roleApi, Role, RoleStatus } from "@/lib/api/roles"
import { userRoleApi } from "@/lib/api/user-roles"

export default function RolePermissionsPage() {
  const router = useRouter()
  const [searchQuery, setSearchQuery] = useState("")

  // Fetch all roles
  const { data: rolesData, isLoading, error } = useQuery({
    queryKey: ['roles'],
    queryFn: () => roleApi.getRoles({ size: 100 }),
  })

  const roles = rolesData?.content || []

  // Filter roles based on search
  const filteredRoles = roles.filter((role) => {
    const query = searchQuery.toLowerCase()
    return (
      role.code.toLowerCase().includes(query) ||
      role.name.toLowerCase().includes(query) ||
      (role.description && role.description.toLowerCase().includes(query))
    )
  })

  // Loading state
  if (isLoading) {
    return (
      <div className="container mx-auto py-6 space-y-4">
        <Skeleton className="h-24 w-full" />
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <Skeleton key={i} className="h-48 w-full" />
          ))}
        </div>
      </div>
    )
  }

  // Error state
  if (error) {
    return (
      <div className="container mx-auto py-6">
        <Card className="border-4 border-border shadow-[8px_8px_0_#000]">
          <CardHeader>
            <CardTitle className="text-2xl font-heading text-red-500">Error Loading Roles</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground mb-4">
              {(error as Error).message || 'Failed to load roles'}
            </p>
            <Button onClick={() => window.location.reload()}>
              Retry
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Header */}
      <Card className="border-4 border-border bg-main text-main-foreground shadow-[8px_8px_0_#000]">
        <CardHeader>
          <CardTitle className="text-3xl font-heading flex items-center gap-3">
            <Shield className="h-8 w-8" />
            Role Permissions
          </CardTitle>
          <CardDescription className="text-main-foreground/80 mt-2">
            Configure permissions and menu access for each role. Click on a role to view and manage its permissions.
          </CardDescription>
        </CardHeader>
      </Card>

      {/* Search */}
      <Card className="border-4 border-border shadow-[8px_8px_0_#000]">
        <CardContent className="pt-6">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground" />
            <Input
              placeholder="Search roles by code, name, or description..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 border-2 border-border text-lg"
            />
          </div>
        </CardContent>
      </Card>

      {/* Role Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredRoles.map((role) => (
          <RoleCard key={role.id} role={role} onClick={() => router.push(`/admin/permissions/roles/${role.code}`)} />
        ))}
      </div>

      {/* No results */}
      {filteredRoles.length === 0 && (
        <Card className="border-4 border-border shadow-[8px_8px_0_#000]">
          <CardContent className="py-12 text-center">
            <Shield className="h-16 w-16 mx-auto mb-4 text-muted-foreground" />
            <h3 className="text-xl font-heading mb-2">No roles found</h3>
            <p className="text-muted-foreground">
              {searchQuery ? 'Try adjusting your search query' : 'No roles available'}
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

interface RoleCardProps {
  role: Role
  onClick: () => void
}

function RoleCard({ role, onClick }: RoleCardProps) {
  // Fetch user count for this role
  const { data: userRoles = [], isLoading: loadingUsers } = useQuery({
    queryKey: ['user-roles', 'role', role.id],
    queryFn: () => userRoleApi.getUsersByRole(role.id),
  })

  const userCount = userRoles.length

  return (
    <Card
      className="border-4 border-border shadow-[8px_8px_0_#000] hover:shadow-[4px_4px_0_#000] hover:translate-x-[4px] hover:translate-y-[4px] transition-all cursor-pointer group"
      onClick={onClick}
    >
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <CardTitle className="text-xl font-heading mb-2 flex items-center gap-2">
              {role.code}
              {role.isSystem && (
                <Badge variant="neutral" className="text-xs">
                  System
                </Badge>
              )}
            </CardTitle>
            <CardDescription className="text-sm font-base">
              {role.name}
            </CardDescription>
          </div>
          <Badge
            variant={role.status === RoleStatus.ACTIVE ? 'default' : 'neutral'}
            className="shrink-0"
          >
            {role.status === RoleStatus.ACTIVE && <span className="mr-1">●</span>}
            {role.status}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        {role.description && (
          <p className="text-sm text-muted-foreground mb-4 line-clamp-2">
            {role.description}
          </p>
        )}

        {/* Stats */}
        <div className="grid grid-cols-2 gap-3 mb-4">
          <div className="bg-secondary-background border-2 border-border p-3 rounded-base">
            <div className="flex items-center gap-2 text-muted-foreground mb-1">
              <Users className="h-4 w-4" />
              <span className="text-xs font-base">Users</span>
            </div>
            <div className="text-2xl font-heading">
              {loadingUsers ? (
                <Loader2 className="h-5 w-5 animate-spin" />
              ) : (
                userCount
              )}
            </div>
          </div>
          <div className="bg-secondary-background border-2 border-border p-3 rounded-base">
            <div className="flex items-center gap-2 text-muted-foreground mb-1">
              <Shield className="h-4 w-4" />
              <span className="text-xs font-base">Priority</span>
            </div>
            <div className="text-2xl font-heading">{role.priority}</div>
          </div>
        </div>

        {/* View Details Button */}
        <Button
          variant="noShadow"
          className="w-full group-hover:bg-main group-hover:text-main-foreground transition-colors"
        >
          <MenuIcon className="h-4 w-4 mr-2" />
          Configure Permissions
          <ArrowRight className="h-4 w-4 ml-auto" />
        </Button>
      </CardContent>
    </Card>
  )
}
