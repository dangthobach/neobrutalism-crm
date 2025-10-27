'use client';

import { useState, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  mockPermissions,
  mockRoles,
  mockUsers,
  mockRolePermissions,
  mockUserRoles,
  mockMenuScreens,
} from '@/data/mock-permissions';
import { Search, ChevronRight, Download } from 'lucide-react';

interface AuditResult {
  permissionCode: string;
  permissionLabel: string;
  featureName: string;
  roles: {
    roleCode: string;
    roleName: string;
    userCount: number;
    riskLevel: string;
  }[];
}

export default function PermissionAuditPage() {
  const [selectedPermission, setSelectedPermission] = useState<string>('');
  const [selectedModule, setSelectedModule] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [selectedAuditResult, setSelectedAuditResult] =
    useState<AuditResult | null>(null);

  // Get unique modules from permissions
  const modules = useMemo(() => {
    const moduleSet = new Set<string>();
    mockPermissions.forEach((perm) => {
      const screen = mockMenuScreens.find((s) => s.code === perm.featureId);
      if (screen) {
        moduleSet.add(perm.featureId);
      }
    });
    return Array.from(moduleSet);
  }, []);

  // Build audit results
  const auditResults = useMemo(() => {
    const results: AuditResult[] = [];

    mockPermissions.forEach((permission) => {
      // Filter by module
      if (selectedModule !== 'all' && permission.featureId !== selectedModule) {
        return;
      }

      // Filter by permission code
      if (
        selectedPermission &&
        permission.actionCode !== selectedPermission
      ) {
        return;
      }

      // Filter by search
      if (searchQuery) {
        const query = searchQuery.toLowerCase();
        const matchesSearch =
          permission.actionCode.toLowerCase().includes(query) ||
          permission.actionLabel.toLowerCase().includes(query) ||
          (permission.description &&
            permission.description.toLowerCase().includes(query));
        if (!matchesSearch) return;
      }

      // Find roles with this permission
      const rolesWithPermission: AuditResult['roles'] = [];
      mockRolePermissions.forEach((permissions, roleCode) => {
        if (permissions.includes(permission.actionCode)) {
          const role = mockRoles.find((r) => r.code === roleCode);
          if (!role) return;

          // Count users with this role
          let userCount = 0;
          mockUserRoles.forEach((roles) => {
            if (roles.includes(roleCode)) {
              userCount++;
            }
          });

          rolesWithPermission.push({
            roleCode: role.code,
            roleName: role.name,
            userCount,
            riskLevel: permission.riskLevel || 'LOW',
          });
        }
      });

      if (rolesWithPermission.length > 0) {
        const screen = mockMenuScreens.find(
          (s) => s.code === permission.featureId
        );

        results.push({
          permissionCode: permission.actionCode,
          permissionLabel: permission.actionLabel,
          featureName: screen?.name || permission.featureId,
          roles: rolesWithPermission,
        });
      }
    });

    return results;
  }, [selectedPermission, selectedModule, searchQuery]);

  const handleViewDetails = (result: AuditResult) => {
    setSelectedAuditResult(result);
    setDetailsOpen(true);
  };

  const handleExportCSV = () => {
    // TODO: Implement CSV export
    console.log('Exporting to CSV...');
  };

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
    <div className="container mx-auto py-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">Permission Audit</h1>
        <p className="text-muted-foreground">
          Track and audit permission assignments across roles and users
        </p>
      </div>

      {/* Filters */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Search & Filter</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-3 gap-4">
            {/* Permission Code */}
            <div className="space-y-2">
              <label className="text-sm font-medium">Permission Code</label>
              <Select
                value={selectedPermission}
                onValueChange={setSelectedPermission}
              >
                <SelectTrigger>
                  <SelectValue placeholder="All Permissions" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">All Permissions</SelectItem>
                  {mockPermissions.map((perm) => (
                    <SelectItem key={perm.id} value={perm.actionCode}>
                      {perm.actionCode}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Module */}
            <div className="space-y-2">
              <label className="text-sm font-medium">Module / Feature</label>
              <Select value={selectedModule} onValueChange={setSelectedModule}>
                <SelectTrigger>
                  <SelectValue placeholder="All Modules" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Modules</SelectItem>
                  {modules.map((module) => {
                    const screen = mockMenuScreens.find(
                      (s) => s.code === module
                    );
                    return (
                      <SelectItem key={module} value={module}>
                        {screen?.name || module}
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
            </div>

            {/* Search */}
            <div className="space-y-2">
              <label className="text-sm font-medium">Search</label>
              <div className="relative">
                <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search permissions..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>
          </div>

          <div className="flex justify-end mt-4">
            <Button variant="noShadow" size="sm" onClick={handleExportCSV}>
              <Download className="h-4 w-4 mr-2" />
              Export CSV
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Results */}
      <Card>
        <CardHeader>
          <CardTitle>
            Audit Results ({auditResults.length} permissions)
          </CardTitle>
        </CardHeader>
        <CardContent>
          <ScrollArea className="h-[600px]">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Permission</TableHead>
                  <TableHead>Feature</TableHead>
                  <TableHead>Role(s)</TableHead>
                  <TableHead className="text-center">#Users</TableHead>
                  <TableHead className="text-center">Risk</TableHead>
                  <TableHead className="text-center">Details</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {auditResults.length > 0 ? (
                  auditResults.map((result, idx) => {
                    const totalUsers = result.roles.reduce(
                      (sum, role) => sum + role.userCount,
                      0
                    );
                    const maxRiskLevel =
                      result.roles.reduce((max, role) => {
                        const riskLevels = [
                          'LOW',
                          'MEDIUM',
                          'HIGH',
                          'CRITICAL',
                        ];
                        return riskLevels.indexOf(role.riskLevel) >
                          riskLevels.indexOf(max)
                          ? role.riskLevel
                          : max;
                      }, 'LOW');

                    return (
                      <TableRow key={idx}>
                        <TableCell>
                          <div>
                            <div className="font-medium">
                              {result.permissionCode}
                            </div>
                            <div className="text-sm text-muted-foreground">
                              {result.permissionLabel}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm">{result.featureName}</div>
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-wrap gap-1">
                            {result.roles.slice(0, 2).map((role) => (
                              <Badge key={role.roleCode} variant="neutral">
                                {role.roleName}
                              </Badge>
                            ))}
                            {result.roles.length > 2 && (
                              <Badge variant="neutral">
                                +{result.roles.length - 2} more
                              </Badge>
                            )}
                          </div>
                        </TableCell>
                        <TableCell className="text-center">
                          {totalUsers}
                        </TableCell>
                        <TableCell className="text-center">
                          <Badge
                            className={getRiskColor(maxRiskLevel)}
                            variant="neutral"
                          >
                            {maxRiskLevel}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-center">
                          <Button
                            variant="noShadow"
                            size="sm"
                            onClick={() => handleViewDetails(result)}
                          >
                            <ChevronRight className="h-4 w-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })
                ) : (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-12">
                      <p className="text-muted-foreground">
                        No audit results found. Try adjusting your filters.
                      </p>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </ScrollArea>
        </CardContent>
      </Card>

      {/* Details Sheet */}
      <Sheet open={detailsOpen} onOpenChange={setDetailsOpen}>
        <SheetContent className="w-[600px] sm:max-w-[600px]">
          <SheetHeader>
            <SheetTitle>Permission Details</SheetTitle>
            <SheetDescription>
              {selectedAuditResult?.permissionCode}
            </SheetDescription>
          </SheetHeader>

          {selectedAuditResult && (
            <div className="mt-6 space-y-6">
              {/* Permission Info */}
              <div>
                <h3 className="font-semibold mb-2">Permission Information</h3>
                <div className="space-y-2 text-sm">
                  <div>
                    <span className="font-medium">Code:</span>{' '}
                    {selectedAuditResult.permissionCode}
                  </div>
                  <div>
                    <span className="font-medium">Label:</span>{' '}
                    {selectedAuditResult.permissionLabel}
                  </div>
                  <div>
                    <span className="font-medium">Feature:</span>{' '}
                    {selectedAuditResult.featureName}
                  </div>
                </div>
              </div>

              {/* Roles */}
              <div>
                <h3 className="font-semibold mb-2">
                  Roles ({selectedAuditResult.roles.length})
                </h3>
                <ScrollArea className="h-[300px]">
                  <div className="space-y-4">
                    {selectedAuditResult.roles.map((role) => {
                      // Get users with this role
                      const usersWithRole: string[] = [];
                      mockUserRoles.forEach((roles, userId) => {
                        if (roles.includes(role.roleCode)) {
                          usersWithRole.push(userId);
                        }
                      });

                      const users = mockUsers.filter((u) =>
                        usersWithRole.includes(u.id)
                      );

                      return (
                        <Card key={role.roleCode}>
                          <CardHeader>
                            <div className="flex items-center justify-between">
                              <CardTitle className="text-base">
                                {role.roleName}
                              </CardTitle>
                              <Badge
                                className={getRiskColor(role.riskLevel)}
                                variant="neutral"
                              >
                                {role.riskLevel}
                              </Badge>
                            </div>
                            <p className="text-sm text-muted-foreground">
                              {role.roleCode}
                            </p>
                          </CardHeader>
                          <CardContent>
                            <div className="space-y-2">
                              <div className="text-sm font-medium">
                                Users ({users.length}):
                              </div>
                              {users.map((user, idx) => (
                                <div
                                  key={user.id}
                                  className="text-sm text-muted-foreground"
                                >
                                  {idx + 1}. {user.firstName} {user.lastName} (
                                  {user.username})
                                </div>
                              ))}
                            </div>
                          </CardContent>
                        </Card>
                      );
                    })}
                  </div>
                </ScrollArea>
              </div>

              {/* Export Button */}
              <div className="flex justify-end">
                <Button variant="neutral" onClick={handleExportCSV}>
                  <Download className="h-4 w-4 mr-2" />
                  Export CSV
                </Button>
              </div>
            </div>
          )}
        </SheetContent>
      </Sheet>
    </div>
  );
}
