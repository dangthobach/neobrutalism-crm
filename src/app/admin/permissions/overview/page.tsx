'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  mockUsers,
  mockRoles,
  mockGroups,
  mockPermissions,
  mockUserRoles,
  mockRolePermissions,
} from '@/data/mock-permissions';
import { UserStatus, RoleStatus } from '@/types/permission';
import {
  Users,
  Shield,
  UsersRound,
  Key,
  AlertTriangle,
  TrendingUp,
  ArrowRight,
} from 'lucide-react';
import Link from 'next/link';

export default function PermissionsOverviewPage() {
  // Calculate statistics
  const activeUsers = mockUsers.filter((u) => u.status === UserStatus.ACTIVE).length;
  const activeRoles = mockRoles.filter((r) => r.status === RoleStatus.ACTIVE).length;
  const activeGroups = mockGroups.length;
  const totalPermissions = mockPermissions.length;

  // High risk permissions count
  const highRiskPermissions = mockPermissions.filter(
    (p) => p.riskLevel === 'HIGH' || p.riskLevel === 'CRITICAL'
  ).length;

  // Users with multiple roles
  const usersWithMultipleRoles = Array.from(mockUserRoles.values()).filter(
    (roles) => roles.length > 1
  ).length;

  // Suspended/Locked users
  const suspendedUsers = mockUsers.filter(
    (u) => u.status === UserStatus.SUSPENDED || u.status === UserStatus.LOCKED
  ).length;

  return (
    <div className="container mx-auto py-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">Permission System Overview</h1>
        <p className="text-muted-foreground">
          Manage users, roles, groups and permissions
        </p>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Total Users</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{mockUsers.length}</div>
            <p className="text-xs text-muted-foreground">
              {activeUsers} active
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Roles</CardTitle>
            <Shield className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{mockRoles.length}</div>
            <p className="text-xs text-muted-foreground">
              {activeRoles} active
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Groups/Teams</CardTitle>
            <UsersRound className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{mockGroups.length}</div>
            <p className="text-xs text-muted-foreground">
              {activeGroups} active
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Permissions</CardTitle>
            <Key className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalPermissions}</div>
            <p className="text-xs text-muted-foreground">
              {highRiskPermissions} high risk
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Alerts & Warnings */}
      {(suspendedUsers > 0 || highRiskPermissions > 0) && (
        <Card className="mb-6 border-yellow-500/50 bg-yellow-50 dark:bg-yellow-950/20">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-yellow-800 dark:text-yellow-200">
              <AlertTriangle className="h-5 w-5" />
              Security Alerts
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="space-y-2 text-sm text-yellow-800 dark:text-yellow-200">
              {suspendedUsers > 0 && (
                <li>
                  • {suspendedUsers} user(s) are currently suspended or locked
                </li>
              )}
              {highRiskPermissions > 0 && (
                <li>
                  • {highRiskPermissions} high risk permission(s) are configured
                </li>
              )}
              {usersWithMultipleRoles > 0 && (
                <li>
                  • {usersWithMultipleRoles} user(s) have multiple roles
                  assigned
                </li>
              )}
            </ul>
          </CardContent>
        </Card>
      )}

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Link href="/admin/permissions/users">
          <Card className="hover:border-primary transition-colors cursor-pointer">
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <span>User Management</span>
                <ArrowRight className="h-5 w-5" />
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                Manage users, assign roles and groups, view effective
                permissions
              </p>
            </CardContent>
          </Card>
        </Link>

        <Link href="/admin/permissions/roles/ROLE_ADMIN">
          <Card className="hover:border-primary transition-colors cursor-pointer">
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <span>Role Management</span>
                <ArrowRight className="h-5 w-5" />
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                Configure roles, set menu access and permission matrix
              </p>
            </CardContent>
          </Card>
        </Link>

        <Link href="/admin/permissions/audit">
          <Card className="hover:border-primary transition-colors cursor-pointer">
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <span>Permission Audit</span>
                <ArrowRight className="h-5 w-5" />
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                Audit permission assignments across roles and users
              </p>
            </CardContent>
          </Card>
        </Link>
      </div>

      {/* Recent Activity */}
      <Card>
        <CardHeader>
          <CardTitle>Recent Roles</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {mockRoles.slice(0, 5).map((role) => {
              // Count users with this role
              let userCount = 0;
              mockUserRoles.forEach((roles) => {
                if (roles.includes(role.code)) {
                  userCount++;
                }
              });

              // Count permissions
              const permCount =
                mockRolePermissions.get(role.code)?.length || 0;

              return (
                <Link key={role.id} href={`/admin/permissions/roles/${role.code}`}>
                  <div className="flex items-center justify-between p-3 border rounded-lg hover:bg-muted transition-colors cursor-pointer">
                    <div className="flex items-center gap-3">
                      <Shield className="h-5 w-5 text-muted-foreground" />
                      <div>
                        <div className="font-medium">{role.name}</div>
                        <div className="text-sm text-muted-foreground">
                          {role.code}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant="neutral">{userCount} users</Badge>
                      <Badge variant="neutral">{permCount} perms</Badge>
                      <Badge
                        variant={
                          role.status === RoleStatus.ACTIVE
                            ? 'default'
                            : 'neutral'
                        }
                      >
                        {role.status}
                      </Badge>
                      <ArrowRight className="h-4 w-4 text-muted-foreground" />
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
