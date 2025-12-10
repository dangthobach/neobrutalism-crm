'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { ArrowLeft, Save, X, Loader2 } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { roleApi, Role, RoleStatus } from '@/lib/api/roles';
import { userRoleApi } from '@/lib/api/user-roles';
import { userApi, User } from '@/lib/api/users';
import RoleGeneralTab from './tabs/general-tab';
import RoleMenuAccessTab from './tabs/menu-access-tab';
import RolePermissionMatrixTab from './tabs/permission-matrix-tab';

export default function RoleDetailPage() {
  const params = useParams();
  const router = useRouter();
  const roleCode = params.roleCode as string;

  const [activeTab, setActiveTab] = useState('general');
  const [hasChanges, setHasChanges] = useState(false);

  // Fetch role by code
  const { data: role, isLoading: roleLoading, error: roleError } = useQuery({
    queryKey: ['roles', 'code', roleCode],
    queryFn: () => roleApi.getRoleByCode(roleCode),
    retry: 1,
  });

  // Fetch user-role assignments for this role
  const { data: userRoleAssignments = [], isLoading: assignmentsLoading } = useQuery({
    queryKey: ['user-roles', 'role', role?.id],
    queryFn: () => userRoleApi.getUsersByRole(role!.id),
    enabled: !!role?.id,
  });

  // Fetch users with this role
  const { data: usersWithRole = [], isLoading: usersLoading } = useQuery({
    queryKey: ['users', 'by-role', role?.id],
    queryFn: async () => {
      const userIds = userRoleAssignments.map((ur) => ur.userId);
      if (userIds.length === 0) return [];

      // Fetch all users and filter by IDs
      const allUsers = await userApi.getUsers({ size: 1000 });
      return allUsers.content.filter((u) => userIds.includes(u.id));
    },
    enabled: !!role?.id && userRoleAssignments.length > 0,
  });

  // Loading state
  if (roleLoading) {
    return (
      <div className="container mx-auto py-6 space-y-4">
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-[600px] w-full" />
      </div>
    );
  }

  // Error state
  if (roleError || !role) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold mb-4">
            {roleError ? 'Error loading role' : 'Role not found'}
          </h1>
          <p className="text-muted-foreground mb-4">
            {roleError ? 'Failed to load role details' : `Role with code "${roleCode}" does not exist`}
          </p>
          <Button onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Go Back
          </Button>
        </div>
      </div>
    );
  }

  const handleSave = () => {
    // TODO: Implement save logic
    console.log('Saving changes...');
    setHasChanges(false);
  };

  const handleCancel = () => {
    if (hasChanges) {
      if (confirm('You have unsaved changes. Are you sure you want to cancel?')) {
        router.back();
      }
    } else {
      router.back();
    }
  };

  return (
    <div className="container mx-auto py-6">
      {/* Header */}
      <Card className="mb-6">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Button variant="noShadow" size="sm" onClick={() => router.back()}>
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back
              </Button>
              <div>
                <div className="flex items-center gap-3">
                  <h1 className="text-2xl font-bold">{role.code}</h1>
                  <Badge
                    variant={
                      role.status === RoleStatus.ACTIVE ? 'default' : 'neutral'
                    }
                  >
                    {role.status === RoleStatus.ACTIVE && (
                      <span className="mr-1">●</span>
                    )}
                    {role.status}
                  </Badge>
                  {role.isSystem && (
                    <Badge variant="neutral">System Role</Badge>
                  )}
                </div>
                <p className="text-sm text-muted-foreground mt-1">
                  {role.name}
                </p>
              </div>
            </div>

            <div className="flex gap-2">
              <Button variant="neutral" onClick={handleCancel}>
                <X className="h-4 w-4 mr-2" />
                Cancel
              </Button>
              <Button onClick={handleSave} disabled={!hasChanges}>
                <Save className="h-4 w-4 mr-2" />
                Save Changes
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="w-full justify-start">
          <TabsTrigger value="general">General Info</TabsTrigger>
          <TabsTrigger value="menu">Menu Access</TabsTrigger>
          <TabsTrigger value="permissions">Permission Matrix</TabsTrigger>
        </TabsList>

        <TabsContent value="general" className="mt-6">
          <RoleGeneralTab
            role={role}
            usersWithRole={usersWithRole}
            userRoleAssignments={userRoleAssignments}
            onChangeDetected={() => setHasChanges(true)}
          />
        </TabsContent>

        <TabsContent value="menu" className="mt-6">
          <RoleMenuAccessTab
            role={role}
            onChangeDetected={() => setHasChanges(true)}
          />
        </TabsContent>

        <TabsContent value="permissions" className="mt-6">
          <RolePermissionMatrixTab
            role={role}
            onChangeDetected={() => setHasChanges(true)}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}
