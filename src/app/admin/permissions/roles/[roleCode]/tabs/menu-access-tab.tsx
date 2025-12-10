'use client';

import { useState, useMemo, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Skeleton } from '@/components/ui/skeleton';
import { Role } from '@/lib/api/roles';
import { useMenuPermissionsByRole, useBulkUpdateMenuPermissions } from '@/hooks/useRoleMenus';
import { useQuery } from '@tanstack/react-query';
import { menuApi, Menu } from '@/lib/api/menus';
import { RoleMenuRequest } from '@/lib/api/role-menus';
import { ChevronRight, ChevronDown, Search, Loader2, Save } from 'lucide-react';
import { toast } from 'sonner';

interface RoleMenuAccessTabProps {
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
  children?: MenuWithPermissions[];
}

export default function RoleMenuAccessTab({
  role,
  onChangeDetected,
}: RoleMenuAccessTabProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedMenus, setExpandedMenus] = useState<Set<string>>(new Set());
  const [menuPermissions, setMenuPermissions] = useState<Map<string, MenuPermissions>>(new Map());
  const [hasChanges, setHasChanges] = useState(false);

  // Fetch all menus
  const { data: allMenus = [], isLoading: menusLoading } = useQuery({
    queryKey: ['menus', 'visible'],
    queryFn: () => menuApi.getVisibleMenus(),
  });

  // Fetch role's current menu permissions
  const { data: roleMenuPerms = [], isLoading: permsLoading } = useMenuPermissionsByRole(role.id);

  // Bulk update mutation
  const bulkUpdateMutation = useBulkUpdateMenuPermissions();

  // Build menu tree
  const menuTree = useMemo(() => {
    const buildTree = (parentId: string | undefined): MenuWithPermissions[] => {
      return allMenus
        .filter((m) => m.parentId === parentId)
        .sort((a, b) => a.displayOrder - b.displayOrder)
        .map((menu) => ({
          ...menu,
          permissions: menuPermissions.get(menu.id),
          children: buildTree(menu.id),
        }));
    };
    return buildTree(undefined);
  }, [allMenus, menuPermissions]);

  // Initialize permissions from backend
  useEffect(() => {
    if (roleMenuPerms.length > 0) {
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
      setMenuPermissions(permsMap);
    }
  }, [roleMenuPerms]);

  const toggleExpand = (menuId: string) => {
    const newExpanded = new Set(expandedMenus);
    if (newExpanded.has(menuId)) {
      newExpanded.delete(menuId);
    } else {
      newExpanded.add(menuId);
    }
    setExpandedMenus(newExpanded);
  };

  const handlePermissionChange = (menuId: string, permission: keyof MenuPermissions, value: boolean) => {
    setMenuPermissions((prev) => {
      const newMap = new Map(prev);
      const current = newMap.get(menuId) || {
        canView: false,
        canCreate: false,
        canEdit: false,
        canDelete: false,
        canExport: false,
        canImport: false,
      };
      newMap.set(menuId, { ...current, [permission]: value });
      return newMap;
    });
    setHasChanges(true);
    onChangeDetected();
  };

  const handleMenuToggle = (menuId: string, checked: boolean) => {
    // When checking/unchecking menu, set canView permission
    handlePermissionChange(menuId, 'canView', checked);
  };

  const handleSavePermissions = async () => {
    try {
      const permissions: RoleMenuRequest[] = [];
      menuPermissions.forEach((perms, menuId) => {
        // Only save if at least canView is true
        if (perms.canView) {
          permissions.push({
            roleId: role.id,
            menuId,
            ...perms,
          });
        }
      });

      await bulkUpdateMutation.mutateAsync({ roleId: role.id, permissions });
      setHasChanges(false);
      toast.success('Menu permissions saved successfully');
    } catch (error) {
      toast.error('Failed to save menu permissions');
    }
  };

  const expandAll = () => {
    const allIds = new Set<string>();
    const collectIds = (menus: MenuWithPermissions[]) => {
      menus.forEach((m) => {
        allIds.add(m.id);
        if (m.children && m.children.length > 0) {
          collectIds(m.children);
        }
      });
    };
    collectIds(menuTree);
    setExpandedMenus(allIds);
  };

  const collapseAll = () => {
    setExpandedMenus(new Set());
  };

  // Filter menus by search
  const filteredTree = useMemo(() => {
    if (!searchQuery) return menuTree;

    const filterMenus = (menus: MenuWithPermissions[]): MenuWithPermissions[] => {
      const filtered: MenuWithPermissions[] = [];

      for (const menu of menus) {
        const matchesSearch =
          menu.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          menu.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
          menu.route?.toLowerCase().includes(searchQuery.toLowerCase());

        const filteredChildren = menu.children ? filterMenus(menu.children) : [];

        if (matchesSearch || filteredChildren.length > 0) {
          filtered.push({
            ...menu,
            children: filteredChildren,
          });
        }
      }

      return filtered;
    };

    return filterMenus(menuTree);
  }, [menuTree, searchQuery]);

  // Count selected menus
  const selectedCount = useMemo(() => {
    return Array.from(menuPermissions.values()).filter((p) => p.canView).length;
  }, [menuPermissions]);

  if (menusLoading || permsLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-[600px] w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header Actions */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Badge variant="neutral">{selectedCount} menus selected</Badge>
          {hasChanges && <Badge variant="default">Unsaved changes</Badge>}
        </div>
        <div className="flex gap-2">
          <Button variant="neutral" size="sm" onClick={expandAll}>
            Expand All
          </Button>
          <Button variant="neutral" size="sm" onClick={collapseAll}>
            Collapse All
          </Button>
          <Button
            size="sm"
            onClick={handleSavePermissions}
            disabled={!hasChanges || bulkUpdateMutation.isPending}
          >
            {bulkUpdateMutation.isPending ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Save className="h-4 w-4 mr-2" />
                Save Permissions
              </>
            )}
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6">
        {/* Left: Menu Tree with Checkboxes */}
        <Card>
          <CardHeader>
            <CardTitle>Menu Tree</CardTitle>
          </CardHeader>
          <CardContent>
            {/* Search */}
            <div className="relative mb-4">
              <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Filter menus..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-8"
              />
            </div>

            {/* Menu Tree */}
            <ScrollArea className="h-[600px]">
              <div className="space-y-1">
                {filteredTree.map((menu) => (
                  <MenuTreeItem
                    key={menu.id}
                    menu={menu}
                    level={0}
                    expanded={expandedMenus.has(menu.id)}
                    permissions={menuPermissions.get(menu.id)}
                    onToggleExpand={() => toggleExpand(menu.id)}
                    onToggleSelect={(checked) => handleMenuToggle(menu.id, checked)}
                    onPermissionChange={(perm, value) =>
                      handlePermissionChange(menu.id, perm, value)
                    }
                  />
                ))}
                {filteredTree.length === 0 && (
                  <p className="text-sm text-muted-foreground text-center py-8">
                    No menus found
                  </p>
                )}
              </div>
            </ScrollArea>
          </CardContent>
        </Card>

        {/* Right: Permission Details */}
        <Card>
          <CardHeader>
            <CardTitle>Permission Details</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {/* Permission Legend */}
              <div className="bg-muted p-3 rounded-lg">
                <h4 className="font-semibold text-sm mb-2">Permission Types</h4>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div><strong>View:</strong> Can see the menu/screen</div>
                  <div><strong>Create:</strong> Can create new records</div>
                  <div><strong>Edit:</strong> Can modify records</div>
                  <div><strong>Delete:</strong> Can remove records</div>
                  <div><strong>Export:</strong> Can export data</div>
                  <div><strong>Import:</strong> Can import data</div>
                </div>
              </div>

              {/* Selected Menus Summary */}
              <div>
                <h4 className="font-semibold mb-2">Selected Menus ({selectedCount})</h4>
                <ScrollArea className="h-[500px]">
                  <div className="space-y-2">
                    {Array.from(menuPermissions.entries())
                      .filter(([, perms]) => perms.canView)
                      .map(([menuId, perms]) => {
                        const menu = allMenus.find((m) => m.id === menuId);
                        if (!menu) return null;

                        const activePerms = Object.entries(perms)
                          .filter(([, value]) => value)
                          .map(([key]) => key.replace('can', ''));

                        return (
                          <div
                            key={menuId}
                            className="p-3 border rounded-lg space-y-2"
                          >
                            <div>
                              <div className="text-sm font-medium">{menu.name}</div>
                              <div className="text-xs text-muted-foreground">
                                {menu.route || menu.code}
                              </div>
                            </div>
                            <div className="flex flex-wrap gap-1">
                              {activePerms.map((perm) => (
                                <Badge key={perm} variant="neutral" className="text-xs">
                                  {perm}
                                </Badge>
                              ))}
                            </div>
                          </div>
                        );
                      })}
                    {selectedCount === 0 && (
                      <p className="text-sm text-muted-foreground text-center py-8">
                        No menus selected
                      </p>
                    )}
                  </div>
                </ScrollArea>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

// Menu Tree Item Component
interface MenuTreeItemProps {
  menu: MenuWithPermissions;
  level: number;
  expanded: boolean;
  permissions?: MenuPermissions;
  onToggleExpand: () => void;
  onToggleSelect: (checked: boolean) => void;
  onPermissionChange: (permission: keyof MenuPermissions, value: boolean) => void;
}

function MenuTreeItem({
  menu,
  level,
  expanded,
  permissions,
  onToggleExpand,
  onToggleSelect,
  onPermissionChange,
}: MenuTreeItemProps) {
  const hasChildren = menu.children && menu.children.length > 0;
  const isSelected = permissions?.canView || false;

  return (
    <div>
      <div
        className="flex items-center gap-2 p-2 hover:bg-muted rounded-lg"
        style={{ paddingLeft: `${level * 20 + 8}px` }}
      >
        {/* Expand/Collapse Icon */}
        {hasChildren ? (
          <button
            onClick={onToggleExpand}
            className="hover:bg-muted-foreground/10 rounded p-0.5"
          >
            {expanded ? (
              <ChevronDown className="h-4 w-4" />
            ) : (
              <ChevronRight className="h-4 w-4" />
            )}
          </button>
        ) : (
          <div className="w-5" />
        )}

        {/* Checkbox */}
        <Checkbox
          checked={isSelected}
          onCheckedChange={onToggleSelect}
          id={menu.id}
        />

        {/* Menu Name */}
        <label
          htmlFor={menu.id}
          className="flex-1 text-sm font-medium cursor-pointer"
        >
          {menu.name}
        </label>

        {/* Route Badge */}
        {menu.route && (
          <Badge variant="neutral" className="text-xs">
            {menu.route}
          </Badge>
        )}
      </div>

      {/* Permission Checkboxes (when selected) */}
      {isSelected && (
        <div
          className="flex flex-wrap gap-2 py-2 text-xs"
          style={{ paddingLeft: `${level * 20 + 32}px` }}
        >
          {(['canCreate', 'canEdit', 'canDelete', 'canExport', 'canImport'] as const).map(
            (perm) => (
              <label key={perm} className="flex items-center gap-1 cursor-pointer">
                <Checkbox
                  checked={permissions?.[perm] || false}
                  onCheckedChange={(checked) =>
                    onPermissionChange(perm, checked as boolean)
                  }
                  id={`${menu.id}-${perm}`}
                />
                <span className="text-muted-foreground">
                  {perm.replace('can', '')}
                </span>
              </label>
            )
          )}
        </div>
      )}

      {/* Children */}
      {expanded && hasChildren && (
        <div>
          {menu.children!.map((child) => (
            <MenuTreeItem
              key={child.id}
              menu={child}
              level={level + 1}
              expanded={false}
              permissions={child.permissions}
              onToggleExpand={() => {}}
              onToggleSelect={() => {}}
              onPermissionChange={() => {}}
            />
          ))}
        </div>
      )}
    </div>
  );
}
