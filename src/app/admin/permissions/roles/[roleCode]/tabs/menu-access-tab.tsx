'use client';

import { useState, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Role, MenuTreeNode } from '@/types/permission';
import {
  buildMenuTree,
  mockRoleMenus,
  mockRolePermissions,
  mockMenuTabs,
  mockMenuScreens,
} from '@/data/mock-permissions';
import { ChevronRight, ChevronDown, Search, AlertTriangle } from 'lucide-react';

interface RoleMenuAccessTabProps {
  role: Role;
  onChangeDetected: () => void;
}

export default function RoleMenuAccessTab({
  role,
  onChangeDetected,
}: RoleMenuAccessTabProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedMenus, setExpandedMenus] = useState<Set<string>>(new Set());
  const [selectedMenus, setSelectedMenus] = useState<Set<string>>(
    new Set(mockRoleMenus.get(role.code) || [])
  );

  // Build menu tree
  const menuTree = useMemo(() => buildMenuTree(), []);

  // Get warnings (permissions without menu)
  const warnings = useMemo(() => {
    const rolePermissions = mockRolePermissions.get(role.code) || [];
    const roleMenuCodes = mockRoleMenus.get(role.code) || [];

    const orphanPermissions: string[] = [];

    rolePermissions.forEach((permCode) => {
      // Check if permission is granted but menu is not visible
      // This is a simplified check
      if (!roleMenuCodes.includes('MENU_DEBT_SALE') && permCode.includes('DEBT')) {
        orphanPermissions.push(permCode);
      }
    });

    return orphanPermissions;
  }, [role.code]);

  const toggleExpand = (menuId: string) => {
    const newExpanded = new Set(expandedMenus);
    if (newExpanded.has(menuId)) {
      newExpanded.delete(menuId);
    } else {
      newExpanded.add(menuId);
    }
    setExpandedMenus(newExpanded);
  };

  const handleMenuToggle = (menuCode: string, checked: boolean) => {
    const newSelected = new Set(selectedMenus);
    if (checked) {
      newSelected.add(menuCode);
    } else {
      newSelected.delete(menuCode);
    }
    setSelectedMenus(newSelected);
    onChangeDetected();
  };

  const expandAll = () => {
    setExpandedMenus(new Set(menuTree.map((m) => m.id)));
  };

  const collapseAll = () => {
    setExpandedMenus(new Set());
  };

  return (
    <div className="grid grid-cols-2 gap-6">
      {/* Left: Menu Tree */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Menu Tree</CardTitle>
            <div className="flex gap-2">
              <Button variant="neutral" size="sm" onClick={expandAll}>
                Expand All
              </Button>
              <Button variant="neutral" size="sm" onClick={collapseAll}>
                Collapse All
              </Button>
            </div>
          </div>
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
              {menuTree.map((menu) => (
                <MenuTreeItem
                  key={menu.id}
                  menu={menu}
                  level={0}
                  expanded={expandedMenus.has(menu.id)}
                  selected={selectedMenus.has(menu.code)}
                  onToggleExpand={() => toggleExpand(menu.id)}
                  onToggleSelect={(checked) =>
                    handleMenuToggle(menu.code, checked)
                  }
                />
              ))}
            </div>
          </ScrollArea>
        </CardContent>
      </Card>

      {/* Right: Access Preview / Validation */}
      <Card>
        <CardHeader>
          <CardTitle>Access Preview & Validation</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {/* Selected Menus Summary */}
            <div>
              <h4 className="font-semibold mb-2">Selected Menus</h4>
              <div className="space-y-2">
                {Array.from(selectedMenus).map((menuCode) => {
                  const menu = menuTree.find((m) => m.code === menuCode);
                  return menu ? (
                    <div
                      key={menu.id}
                      className="flex items-center justify-between p-2 border rounded-lg"
                    >
                      <div>
                        <div className="text-sm font-medium">{menu.name}</div>
                        <div className="text-xs text-muted-foreground">
                          {menu.route}
                        </div>
                      </div>
                      <Badge variant="neutral">OK</Badge>
                    </div>
                  ) : null;
                })}
                {selectedMenus.size === 0 && (
                  <p className="text-sm text-muted-foreground">
                    No menus selected
                  </p>
                )}
              </div>
            </div>

            {/* Warnings */}
            {warnings.length > 0 && (
              <Alert variant="destructive">
                <AlertTriangle className="h-4 w-4" />
                <AlertDescription>
                  <div className="font-semibold mb-1">Warnings</div>
                  <ul className="list-disc list-inside space-y-1">
                    {warnings.map((warning, idx) => (
                      <li key={idx} className="text-sm">
                        Permission <code className="font-mono">{warning}</code>{' '}
                        granted but menu hidden
                      </li>
                    ))}
                  </ul>
                </AlertDescription>
              </Alert>
            )}

            {/* Screens Available */}
            <div>
              <h4 className="font-semibold mb-2">Accessible Screens</h4>
              <ScrollArea className="h-[400px]">
                <div className="space-y-2">
                  {mockMenuScreens
                    .filter((screen) => {
                      const menu = menuTree.find(
                        (m) => m.id === screen.menuId
                      );
                      return menu && selectedMenus.has(menu.code);
                    })
                    .map((screen) => (
                      <div
                        key={screen.id}
                        className="p-2 border rounded-lg text-sm"
                      >
                        <div className="font-medium">{screen.name}</div>
                        <div className="text-xs text-muted-foreground">
                          {screen.route}
                        </div>
                      </div>
                    ))}
                </div>
              </ScrollArea>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// Menu Tree Item Component
interface MenuTreeItemProps {
  menu: MenuTreeNode;
  level: number;
  expanded: boolean;
  selected: boolean;
  onToggleExpand: () => void;
  onToggleSelect: (checked: boolean) => void;
}

function MenuTreeItem({
  menu,
  level,
  expanded,
  selected,
  onToggleExpand,
  onToggleSelect,
}: MenuTreeItemProps) {
  const hasChildren =
    (menu.children && menu.children.length > 0) ||
    (menu.tabs && menu.tabs.length > 0);

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
          checked={selected}
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

      {/* Children (tabs/screens) */}
      {expanded && menu.tabs && menu.tabs.length > 0 && (
        <div className="ml-4">
          {menu.tabs.map((tab) => (
            <div
              key={tab.id}
              className="flex items-center gap-2 p-2 text-sm text-muted-foreground"
              style={{ paddingLeft: `${(level + 1) * 20 + 8}px` }}
            >
              <div className="w-5" />
              <span>└ {tab.name}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
