'use client';

import { useState, useMemo } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowLeft, Save, X } from 'lucide-react';
import { mockRoles, mockUsers, mockUserRoles } from '@/data/mock-permissions';
import { RoleStatus } from '@/types/permission';
import RoleGeneralTab from './tabs/general-tab';
import RoleMenuAccessTab from './tabs/menu-access-tab';
import RolePermissionMatrixTab from './tabs/permission-matrix-tab';

export default function RoleDetailPage() {
  const params = useParams();
  const router = useRouter();
  const roleCode = params.roleCode as string;

  const [activeTab, setActiveTab] = useState('general');
  const [hasChanges, setHasChanges] = useState(false);

  // Find role
  const role = useMemo(
    () => mockRoles.find((r) => r.code === roleCode),
    [roleCode]
  );

  // Get users with this role
  const usersWithRole = useMemo(() => {
    const userIds: string[] = [];
    mockUserRoles.forEach((roles, userId) => {
      if (roles.includes(roleCode)) {
        userIds.push(userId);
      }
    });
    return mockUsers.filter((u) => userIds.includes(u.id));
  }, [roleCode]);

  if (!role) {
    return (
      <div className="container mx-auto py-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold mb-4">Role not found</h1>
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
              <Button variant="ghost" size="sm" onClick={() => router.back()}>
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back
              </Button>
              <div>
                <div className="flex items-center gap-3">
                  <h1 className="text-2xl font-bold">{role.code}</h1>
                  <Badge
                    variant={
                      role.status === RoleStatus.ACTIVE ? 'default' : 'secondary'
                    }
                  >
                    {role.status === RoleStatus.ACTIVE && (
                      <span className="mr-1">●</span>
                    )}
                    {role.status}
                  </Badge>
                  {role.isSystem && (
                    <Badge variant="outline">System Role</Badge>
                  )}
                </div>
                <p className="text-sm text-muted-foreground mt-1">
                  {role.name}
                </p>
              </div>
            </div>

            <div className="flex gap-2">
              <Button variant="outline" onClick={handleCancel}>
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
