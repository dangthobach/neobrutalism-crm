'use client';

import { useState, useMemo } from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
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
import { UserStatus } from '@/types/permission';
import { Search, UserPlus, X, Loader2 } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { userApi } from '@/lib/api/users';
import { roleApi } from '@/lib/api/roles';
import { userRoleApi } from '@/lib/api/user-roles';
import { menuApi } from '@/lib/api/menus';
import { useGroups } from '@/hooks/useGroups';

export default function UserManagementPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterTeam, setFilterTeam] = useState<string>('all');
  const [filterRole, setFilterRole] = useState<string>('all');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);

  // Fetch all users
  const { data: usersData, isLoading: usersLoading } = useQuery({
    queryKey: ['users', 'all'],
    queryFn: () => userApi.getUsers({ size: 1000 }),
  });

  // Fetch all roles
  const { data: rolesData, isLoading: rolesLoading } = useQuery({
    queryKey: ['roles', 'all'],
    queryFn: () => roleApi.getRoles({ size: 100 }),
  });

  // Fetch groups
  const { data: groupsData, isLoading: groupsLoading } = useGroups();

  // Fetch selected user's roles
  const { data: userRoles = [] } = useQuery({
    queryKey: ['user-roles', 'user', selectedUserId],
    queryFn: () => userRoleApi.getRolesByUser(selectedUserId!),
    enabled: !!selectedUserId,
  });

  // Fetch selected user's menus with permissions
  const { data: userMenus = [] } = useQuery({
    queryKey: ['user-menus', selectedUserId],
    queryFn: () => menuApi.getUserMenus(selectedUserId!),
    enabled: !!selectedUserId,
  });

  const users = useMemo(() => usersData?.content || [], [usersData]);
  const roles = useMemo(() => rolesData?.content || [], [rolesData]);
  const groups = useMemo(() => groupsData?.content || [], [groupsData]);

  // Set initial selected user
  useEffect(() => {
    if (!selectedUserId && users.length > 0) {
      setSelectedUserId(users[0].id);
    }
  }, [users, selectedUserId]);

  // Filter users
  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
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

      // TODO: Team filter (would need user-group data)
      // TODO: Role filter (would need user-role data)

      return true;
    });
  }, [users, searchQuery, filterStatus]);

  // Get selected user
  const selectedUser = users.find((u) => u.id === selectedUserId);

  // Get selected user's roles with details
  const selectedUserRoles = useMemo(() => {
    return userRoles
      .map((ur) => roles.find((r) => r.id === ur.roleId))
      .filter((r): r is NonNullable<typeof r> => r !== undefined);
  }, [userRoles, roles]);

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

  // Loading state
  if (usersLoading || rolesLoading || groupsLoading) {
    return (
      <div className="container mx-auto py-6 space-y-4">
        <Skeleton className="h-20 w-full" />
        <div className="grid grid-cols-12 gap-4">
          <Skeleton className="col-span-3 h-[700px]" />
          <Skeleton className="col-span-5 h-[700px]" />
          <Skeleton className="col-span-4 h-[700px]" />
        </div>
      </div>
    );
  }

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
              Search and filter users ({filteredUsers.length})
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
                  {groups.map((group) => (
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
                  {roles.map((role) => (
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
            {selectedUser ? (
              <div className="space-y-6">
                {/* User Profile */}
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <div>
                      <h3 className="text-lg font-semibold">
                        {selectedUser.firstName} {selectedUser.lastName}
                      </h3>
                      <p className="text-sm text-muted-foreground">
                        @{selectedUser.username}
                      </p>
                    </div>
                    <Badge className={getStatusColor(selectedUser.status)}>
                      {selectedUser.status}
                    </Badge>
                  </div>

                  <div className="space-y-2 text-sm">
                    <div>
                      <span className="font-medium">Email:</span>{' '}
                      {selectedUser.email}
                    </div>
                    {selectedUser.phone && (
                      <div>
                        <span className="font-medium">Phone:</span>{' '}
                        {selectedUser.phone}
                      </div>
                    )}
                    {selectedUser.lastLoginAt && (
                      <div>
                        <span className="font-medium">Last Login:</span>{' '}
                        {new Date(selectedUser.lastLoginAt).toLocaleString()}
                      </div>
                    )}
                  </div>
                </div>

                <Separator />

                {/* Teams/Groups - TODO: Fetch user groups */}
                <div>
                  <h4 className="font-semibold mb-3">Teams / Groups</h4>
                  <div className="flex flex-wrap gap-2">
                    <p className="text-sm text-muted-foreground">
                      No teams assigned
                    </p>
                  </div>
                </div>

                <Separator />

                {/* Roles */}
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <h4 className="font-semibold">Roles ({selectedUserRoles.length})</h4>
                    <Button size="sm" variant="neutral">
                      <UserPlus className="h-4 w-4 mr-1" />
                      Assign Role
                    </Button>
                  </div>
                  <div className="space-y-2">
                    {selectedUserRoles.length > 0 ? (
                      selectedUserRoles.map((role) => (
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
            {selectedUser ? (
              <div className="space-y-6">
                {/* Menus Visible */}
                <div>
                  <h4 className="font-semibold mb-3">Menus Visible ({userMenus.length})</h4>
                  <ScrollArea className="h-[250px]">
                    <div className="space-y-2">
                      {userMenus.map((menu) => (
                        <div
                          key={menu.id}
                          className="p-2 border rounded-lg"
                        >
                          <div className="text-sm font-medium">
                            {menu.name}
                          </div>
                          <div className="text-xs text-muted-foreground">
                            {menu.route}
                          </div>
                          {menu.permissions && (
                            <div className="flex flex-wrap gap-1 mt-1">
                              {Object.entries(menu.permissions)
                                .filter(([, value]) => value)
                                .map(([key]) => (
                                  <Badge key={key} variant="neutral" className="text-xs">
                                    {key.replace('can', '')}
                                  </Badge>
                                ))}
                            </div>
                          )}
                        </div>
                      ))}
                      {userMenus.length === 0 && (
                        <p className="text-sm text-muted-foreground text-center py-4">
                          No menus accessible
                        </p>
                      )}
                    </div>
                  </ScrollArea>
                </div>

                <Separator />

                {/* Role Summary */}
                <div>
                  <h4 className="font-semibold mb-3">Assigned Roles</h4>
                  <div className="space-y-2">
                    {selectedUserRoles.map((role) => (
                      <div
                        key={role.id}
                        className="p-2 border rounded-lg"
                      >
                        <div className="text-sm font-medium">{role.name}</div>
                        <div className="text-xs text-muted-foreground">
                          {role.code} • Priority: {role.priority}
                        </div>
                      </div>
                    ))}
                    {selectedUserRoles.length === 0 && (
                      <p className="text-sm text-muted-foreground text-center py-4">
                        No roles assigned
                      </p>
                    )}
                  </div>
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
