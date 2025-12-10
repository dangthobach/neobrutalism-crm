'use client';

import { useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Role } from '@/lib/api/roles';
import { useQuery } from '@tanstack/react-query';
import { menuApi, Menu } from '@/lib/api/menus';
import { useMenuPermissionsByRole } from '@/hooks/useRoleMenus';
import { Download } from 'lucide-react';
import { toast } from 'sonner';

interface RolePermissionMatrixTabProps {
  role: Role;
  onChangeDetected: () => void;
}

interface MenuPermissions {
  canView: boolean;
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
  canExport: boolean;
  canImport: boolean;
}

interface MenuWithPermissions extends Menu {
  permissions?: MenuPermissions;
}

export default function RolePermissionMatrixTab({
  role,
  onChangeDetected,
}: RolePermissionMatrixTabProps) {
  // Fetch all menus
  const { data: allMenus = [], isLoading: menusLoading } = useQuery({
    queryKey: ['menus', 'visible'],
    queryFn: () => menuApi.getVisibleMenus(),
  });

  // Fetch role's current menu permissions
  const { data: roleMenuPerms = [], isLoading: permsLoading } = useMenuPermissionsByRole(role.id);

  // Group menus by tag/parent
  const groupedMenus = useMemo(() => {
    const groups = new Map<string, MenuWithPermissions[]>();

    // Create permission map
    const permsMap = new Map<string, MenuPermissions>();
    roleMenuPerms.forEach((rp) => {
      permsMap.set(rp.menuId, {
        canView: rp.canView,
        canCreate: rp.canCreate,
        canEdit: rp.canEdit,
        canDelete: rp.canDelete,
        canExport: rp.canExport,
        canImport: rp.canImport,
      });
    });

    // Group menus
    allMenus.forEach((menu) => {
      // Determine group name (use parent menu or tag)
      const groupName = menu.parentId
        ? allMenus.find(m => m.id === menu.parentId)?.name || 'Other'
        : menu.name;

      if (!groups.has(groupName)) {
        groups.set(groupName, []);
      }

      groups.get(groupName)!.push({
        ...menu,
        permissions: permsMap.get(menu.id),
      });
    });

    return Array.from(groups.entries()).map(([groupName, menus]) => ({
      groupName,
      menus: menus.sort((a, b) => a.displayOrder - b.displayOrder),
    }));
  }, [allMenus, roleMenuPerms]);

  const handleExportExcel = () => {
    // TODO: Implement Excel export
    toast.info('Excel export coming soon');
  };

  // Calculate statistics
  const stats = useMemo(() => {
    const totalMenus = allMenus.length;
    const menusWithView = roleMenuPerms.filter(p => p.canView).length;
    const menusWithCreate = roleMenuPerms.filter(p => p.canCreate).length;
    const menusWithEdit = roleMenuPerms.filter(p => p.canEdit).length;
    const menusWithDelete = roleMenuPerms.filter(p => p.canDelete).length;
    const menusWithExport = roleMenuPerms.filter(p => p.canExport).length;
    const menusWithImport = roleMenuPerms.filter(p => p.canImport).length;

    return {
      totalMenus,
      menusWithView,
      menusWithCreate,
      menusWithEdit,
      menusWithDelete,
      menusWithExport,
      menusWithImport,
      totalPermissions: menusWithView + menusWithCreate + menusWithEdit + menusWithDelete + menusWithExport + menusWithImport,
    };
  }, [allMenus, roleMenuPerms]);

  if (menusLoading || permsLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-[100px] w-full" />
        <Skeleton className="h-[600px] w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Toolbar */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-semibold">Permission Matrix</h3>
              <p className="text-sm text-muted-foreground">
                View all menu permissions for {role.name} in a matrix format
              </p>
            </div>

            <Button variant="neutral" size="sm" onClick={handleExportExcel}>
              <Download className="h-4 w-4 mr-2" />
              Export Excel
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Permission Matrix */}
      <ScrollArea className="h-[600px]">
        <div className="space-y-6 pr-4">
          {groupedMenus.map((group) => {
            const menusWithPerms = group.menus.filter(m => m.permissions?.canView);

            return (
              <Card key={group.groupName}>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-lg">{group.groupName}</CardTitle>
                    <Badge variant="neutral">
                      {menusWithPerms.length} / {group.menus.length} menus
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="overflow-x-auto">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead className="w-[250px]">Menu / Screen</TableHead>
                          <TableHead className="text-center w-[80px]">View</TableHead>
                          <TableHead className="text-center w-[80px]">Create</TableHead>
                          <TableHead className="text-center w-[80px]">Edit</TableHead>
                          <TableHead className="text-center w-[80px]">Delete</TableHead>
                          <TableHead className="text-center w-[80px]">Export</TableHead>
                          <TableHead className="text-center w-[80px]">Import</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {group.menus.map((menu) => {
                          const perms = menu.permissions;
                          const hasAnyPermission = perms?.canView;

                          return (
                            <TableRow
                              key={menu.id}
                              className={hasAnyPermission ? 'bg-primary/5' : ''}
                            >
                              <TableCell>
                                <div>
                                  <div className="font-medium">{menu.name}</div>
                                  <div className="text-xs text-muted-foreground">
                                    {menu.route || menu.code}
                                  </div>
                                </div>
                              </TableCell>
                              <TableCell className="text-center">
                                {perms?.canView ? (
                                  <Badge variant="default" className="text-xs">✓</Badge>
                                ) : (
                                  <span className="text-muted-foreground">-</span>
                                )}
                              </TableCell>
                              <TableCell className="text-center">
                                {perms?.canCreate ? (
                                  <Badge variant="default" className="text-xs">✓</Badge>
                                ) : (
                                  <span className="text-muted-foreground">-</span>
                                )}
                              </TableCell>
                              <TableCell className="text-center">
                                {perms?.canEdit ? (
                                  <Badge variant="default" className="text-xs">✓</Badge>
                                ) : (
                                  <span className="text-muted-foreground">-</span>
                                )}
                              </TableCell>
                              <TableCell className="text-center">
                                {perms?.canDelete ? (
                                  <Badge variant="default" className="text-xs">✓</Badge>
                                ) : (
                                  <span className="text-muted-foreground">-</span>
                                )}
                              </TableCell>
                              <TableCell className="text-center">
                                {perms?.canExport ? (
                                  <Badge variant="default" className="text-xs">✓</Badge>
                                ) : (
                                  <span className="text-muted-foreground">-</span>
                                )}
                              </TableCell>
                              <TableCell className="text-center">
                                {perms?.canImport ? (
                                  <Badge variant="default" className="text-xs">✓</Badge>
                                ) : (
                                  <span className="text-muted-foreground">-</span>
                                )}
                              </TableCell>
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  </div>
                </CardContent>
              </Card>
            );
          })}

          {groupedMenus.length === 0 && (
            <div className="text-center py-12 text-muted-foreground">
              No menus found
            </div>
          )}
        </div>
      </ScrollArea>

      {/* Summary */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Permission Summary</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-4 gap-4 text-sm">
            <div className="space-y-1">
              <div className="text-muted-foreground">Total Menus</div>
              <div className="text-2xl font-bold">{stats.totalMenus}</div>
            </div>
            <div className="space-y-1">
              <div className="text-muted-foreground">Menus with View</div>
              <div className="text-2xl font-bold">{stats.menusWithView}</div>
            </div>
            <div className="space-y-1">
              <div className="text-muted-foreground">Total Permissions</div>
              <div className="text-2xl font-bold">{stats.totalPermissions}</div>
            </div>
            <div className="space-y-1">
              <div className="text-muted-foreground">Coverage</div>
              <div className="text-2xl font-bold">
                {stats.totalMenus > 0
                  ? Math.round((stats.menusWithView / stats.totalMenus) * 100)
                  : 0}%
              </div>
            </div>
          </div>

          <div className="mt-4 pt-4 border-t">
            <div className="grid grid-cols-5 gap-4 text-xs">
              <div>
                <div className="text-muted-foreground mb-1">Create</div>
                <Badge variant="neutral">{stats.menusWithCreate}</Badge>
              </div>
              <div>
                <div className="text-muted-foreground mb-1">Edit</div>
                <Badge variant="neutral">{stats.menusWithEdit}</Badge>
              </div>
              <div>
                <div className="text-muted-foreground mb-1">Delete</div>
                <Badge variant="neutral">{stats.menusWithDelete}</Badge>
              </div>
              <div>
                <div className="text-muted-foreground mb-1">Export</div>
                <Badge variant="neutral">{stats.menusWithExport}</Badge>
              </div>
              <div>
                <div className="text-muted-foreground mb-1">Import</div>
                <Badge variant="neutral">{stats.menusWithImport}</Badge>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
