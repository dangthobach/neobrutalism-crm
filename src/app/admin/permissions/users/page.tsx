'use client';

import { useState, useMemo } from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';
import {
  mockUsers,
  mockRoles,
  mockGroups,
  getUserWithRoles,
} from '@/data/mock-permissions';
import { UserStatus } from '@/types/permission';
import { Search, UserPlus, X } from 'lucide-react';

export default function UserManagementPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterTeam, setFilterTeam] = useState<string>('all');
  const [filterRole, setFilterRole] = useState<string>('all');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [selectedUserId, setSelectedUserId] = useState<string | null>(
    mockUsers[0]?.id || null
  );

  // Filter users
  const filteredUsers = useMemo(() => {
    return mockUsers.filter((user) => {
      // Search filter
      if (searchQuery) {
        const query = searchQuery.toLowerCase();
        const matchesSearch =
          user.username.toLowerCase().includes(query) ||
          user.email.toLowerCase().includes(query) ||
          user.firstName.toLowerCase().includes(query) ||
          user.lastName.toLowerCase().includes(query);
        if (!matchesSearch) return false;
      }

      // Status filter
      if (filterStatus !== 'all' && user.status !== filterStatus) {
        return false;
      }

      // Team filter (would need user-group data)
      // Role filter (would need user-role data)

      return true;
    });
  }, [searchQuery, filterTeam, filterRole, filterStatus]);

  // Get selected user with roles
  const selectedUserWithRoles = selectedUserId
    ? getUserWithRoles(selectedUserId)
    : null;

  // Status badge color
  const getStatusColor = (status: UserStatus) => {
    switch (status) {
      case UserStatus.ACTIVE:
        return 'bg-green-500';
      case UserStatus.PENDING:
        return 'bg-yellow-500';
      case UserStatus.SUSPENDED:
        return 'bg-orange-500';
      case UserStatus.LOCKED:
        return 'bg-red-500';
      case UserStatus.INACTIVE:
        return 'bg-gray-500';
      default:
        return 'bg-gray-500';
    }
  };

  return (
    <div className="container mx-auto py-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">User Management</h1>
        <p className="text-muted-foreground">
          Manage users, roles, teams and permissions
        </p>
      </div>

      <div className="grid grid-cols-12 gap-4">
        {/* Left Pane - User List */}
        <Card className="col-span-3">
          <CardHeader>
            <CardTitle>Users</CardTitle>
            <CardDescription>
              Search and filter users
            </CardDescription>
          </CardHeader>
          <CardContent>
            {/* Search */}
            <div className="relative mb-4">
              <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search users..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-8"
              />
            </div>

            {/* Filters */}
            <div className="space-y-2 mb-4">
              <Select value={filterTeam} onValueChange={setFilterTeam}>
                <SelectTrigger>
                  <SelectValue placeholder="Filter by Team" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Teams</SelectItem>
                  {mockGroups.map((group) => (
                    <SelectItem key={group.id} value={group.code}>
                      {group.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select value={filterRole} onValueChange={setFilterRole}>
                <SelectTrigger>
                  <SelectValue placeholder="Filter by Role" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Roles</SelectItem>
                  {mockRoles.map((role) => (
                    <SelectItem key={role.id} value={role.code}>
                      {role.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select value={filterStatus} onValueChange={setFilterStatus}>
                <SelectTrigger>
                  <SelectValue placeholder="Filter by Status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Status</SelectItem>
                  <SelectItem value={UserStatus.ACTIVE}>Active</SelectItem>
                  <SelectItem value={UserStatus.PENDING}>Pending</SelectItem>
                  <SelectItem value={UserStatus.SUSPENDED}>Suspended</SelectItem>
                  <SelectItem value={UserStatus.LOCKED}>Locked</SelectItem>
                  <SelectItem value={UserStatus.INACTIVE}>Inactive</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Separator className="my-4" />

            {/* User List */}
            <ScrollArea className="h-[500px]">
              <div className="space-y-2">
                {filteredUsers.map((user) => (
                  <div
                    key={user.id}
                    onClick={() => setSelectedUserId(user.id)}
                    className={`p-3 rounded-lg border cursor-pointer transition-colors ${
                      selectedUserId === user.id
                        ? 'bg-primary/10 border-primary'
                        : 'hover:bg-muted'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-medium">
                        {user.firstName} {user.lastName}
                      </span>
                      <div
                        className={`w-2 h-2 rounded-full ${getStatusColor(
                          user.status
                        )}`}
                      />
                    </div>
                    <div className="text-sm text-muted-foreground">
                      @{user.username}
                    </div>
                  </div>
                ))}
              </div>
            </ScrollArea>
          </CardContent>
        </Card>

        {/* Middle Pane - User Detail */}
        <Card className="col-span-5">
          <CardHeader>
            <CardTitle>User Details</CardTitle>
            <CardDescription>
              Profile, teams, and roles
            </CardDescription>
          </CardHeader>
          <CardContent>
            {selectedUserWithRoles ? (
              <div className="space-y-6">
                {/* User Profile */}
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <div>
                      <h3 className="text-lg font-semibold">
                        {selectedUserWithRoles.firstName}{' '}
                        {selectedUserWithRoles.lastName}
                      </h3>
                      <p className="text-sm text-muted-foreground">
                        @{selectedUserWithRoles.username}
                      </p>
                    </div>
                    <Badge className={getStatusColor(selectedUserWithRoles.status)}>
                      {selectedUserWithRoles.status}
                    </Badge>
                  </div>

                  <div className="space-y-2 text-sm">
                    <div>
                      <span className="font-medium">Email:</span>{' '}
                      {selectedUserWithRoles.email}
                    </div>
                    {selectedUserWithRoles.phone && (
                      <div>
                        <span className="font-medium">Phone:</span>{' '}
                        {selectedUserWithRoles.phone}
                      </div>
                    )}
                    {selectedUserWithRoles.lastLoginAt && (
                      <div>
                        <span className="font-medium">Last Login:</span>{' '}
                        {new Date(
                          selectedUserWithRoles.lastLoginAt
                        ).toLocaleString()}
                      </div>
                    )}
                  </div>
                </div>

                <Separator />

                {/* Teams/Groups */}
                <div>
                  <h4 className="font-semibold mb-3">Teams / Groups</h4>
                  <div className="flex flex-wrap gap-2">
                    {selectedUserWithRoles.groups.length > 0 ? (
                      selectedUserWithRoles.groups.map((group) => (
                        <Badge key={group.id} variant="neutral">
                          {group.name}
                        </Badge>
                      ))
                    ) : (
                      <p className="text-sm text-muted-foreground">
                        No teams assigned
                      </p>
                    )}
                  </div>
                </div>

                <Separator />

                {/* Roles */}
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <h4 className="font-semibold">Roles</h4>
                    <Button size="sm" variant="neutral">
                      <UserPlus className="h-4 w-4 mr-1" />
                      Assign Role
                    </Button>
                  </div>
                  <div className="space-y-2">
                    {selectedUserWithRoles.roles.length > 0 ? (
                      selectedUserWithRoles.roles.map((role) => (
                        <div
                          key={role.id}
                          className="flex items-center justify-between p-2 border rounded-lg"
                        >
                          <div>
                            <div className="font-medium">{role.name}</div>
                            <div className="text-xs text-muted-foreground">
                              {role.code}
                            </div>
                          </div>
                          <Button size="sm" variant="noShadow">
                            <X className="h-4 w-4" />
                          </Button>
                        </div>
                      ))
                    ) : (
                      <p className="text-sm text-muted-foreground">
                        No roles assigned
                      </p>
                    )}
                  </div>
                </div>

                <Separator />

                {/* Failed Login Attempts */}
                {selectedUserWithRoles.failedLoginAttempts > 0 && (
                  <div className="bg-yellow-50 dark:bg-yellow-950 p-3 rounded-lg">
                    <p className="text-sm font-medium text-yellow-800 dark:text-yellow-200">
                      Failed Login Attempts:{' '}
                      {selectedUserWithRoles.failedLoginAttempts}
                    </p>
                  </div>
                )}

                {selectedUserWithRoles.lockedUntil && (
                  <div className="bg-red-50 dark:bg-red-950 p-3 rounded-lg">
                    <p className="text-sm font-medium text-red-800 dark:text-red-200">
                      Account locked until:{' '}
                      {new Date(
                        selectedUserWithRoles.lockedUntil
                      ).toLocaleString()}
                    </p>
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center py-12 text-muted-foreground">
                Select a user to view details
              </div>
            )}
          </CardContent>
        </Card>

        {/* Right Pane - Effective Access */}
        <Card className="col-span-4">
          <CardHeader>
            <CardTitle>Effective Access</CardTitle>
            <CardDescription>
              Menus and permissions from all roles
            </CardDescription>
          </CardHeader>
          <CardContent>
            {selectedUserWithRoles ? (
              <div className="space-y-6">
                {/* Menus Visible */}
                <div>
                  <h4 className="font-semibold mb-3">Menus Visible</h4>
                  <ScrollArea className="h-[200px]">
                    <div className="space-y-2">
                      {selectedUserWithRoles.effectiveMenus.map((menu) => (
                        <div
                          key={menu.id}
                          className="flex items-center p-2 border rounded-lg"
                        >
                          <div className="flex-1">
                            <div className="text-sm font-medium">
                              {menu.name}
                            </div>
                            <div className="text-xs text-muted-foreground">
                              {menu.route}
                            </div>
                          </div>
                        </div>
                      ))}
                      {selectedUserWithRoles.effectiveMenus.length === 0 && (
                        <p className="text-sm text-muted-foreground">
                          No menus accessible
                        </p>
                      )}
                    </div>
                  </ScrollArea>
                </div>

                <Separator />

                {/* Permissions / Actions */}
                <div>
                  <h4 className="font-semibold mb-3">Permissions / Actions</h4>
                  <ScrollArea className="h-[300px]">
                    <div className="space-y-2">
                      {selectedUserWithRoles.effectivePermissions.map(
                        (permission) => {
                          const getRiskColor = (riskLevel: string) => {
                            switch (riskLevel) {
                              case 'LOW':
                                return 'bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-200';
                              case 'MEDIUM':
                                return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-950 dark:text-yellow-200';
                              case 'HIGH':
                                return 'bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-200';
                              case 'CRITICAL':
                                return 'bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-200';
                              default:
                                return 'bg-gray-100 text-gray-800 dark:bg-gray-950 dark:text-gray-200';
                            }
                          };

                          return (
                            <div
                              key={permission.id}
                              className="flex items-center justify-between p-2 border rounded-lg"
                            >
                              <div className="flex-1">
                                <div className="text-sm font-medium">
                                  {permission.actionCode}
                                </div>
                                <div className="text-xs text-muted-foreground">
                                  {permission.description}
                                </div>
                              </div>
                              {permission.riskLevel && (
                                <Badge
                                  className={getRiskColor(
                                    permission.riskLevel
                                  )}
                                  variant="neutral"
                                >
                                  {permission.riskLevel}
                                </Badge>
                              )}
                            </div>
                          );
                        }
                      )}
                      {selectedUserWithRoles.effectivePermissions.length ===
                        0 && (
                        <p className="text-sm text-muted-foreground">
                          No permissions granted
                        </p>
                      )}
                    </div>
                  </ScrollArea>
                </div>
              </div>
            ) : (
              <div className="text-center py-12 text-muted-foreground">
                Select a user to view effective access
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
