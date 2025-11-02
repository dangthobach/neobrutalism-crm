'use client';

import { useState, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Role, PermissionMatrix } from '@/types/permission';
import {
  getPermissionMatrixForRole,
  mockRolePermissions,
  mockRoles,
} from '@/data/mock-permissions';
import { Download, Copy, AlertCircle } from 'lucide-react';

interface RolePermissionMatrixTabProps {
  role: Role;
  onChangeDetected: () => void;
}

export default function RolePermissionMatrixTab({
  role,
  onChangeDetected,
}: RolePermissionMatrixTabProps) {
  const [cloneFromRole, setCloneFromRole] = useState<string>('');

  // Get permission matrix
  const permissionMatrix = useMemo(
    () => getPermissionMatrixForRole(role.code),
    [role.code]
  );

  // State for granted permissions
  const [grantedPermissions, setGrantedPermissions] = useState<Set<string>>(
    new Set(mockRolePermissions.get(role.code) || [])
  );

  const handlePermissionToggle = (actionCode: string, checked: boolean) => {
    const newGranted = new Set(grantedPermissions);
    if (checked) {
      newGranted.add(actionCode);
    } else {
      newGranted.delete(actionCode);
    }
    setGrantedPermissions(newGranted);
    onChangeDetected();
  };

  const handleSelectAllForFeature = (featureId: string, checked: boolean) => {
    const feature = permissionMatrix.find((f) => f.featureId === featureId);
    if (!feature) return;

    const newGranted = new Set(grantedPermissions);
    feature.actions.forEach((action) => {
      if (checked) {
        newGranted.add(action.permission.actionCode);
      } else {
        newGranted.delete(action.permission.actionCode);
      }
    });
    setGrantedPermissions(newGranted);
    onChangeDetected();
  };

  const handleCloneFromRole = () => {
    if (!cloneFromRole) return;

    const sourcePermissions = mockRolePermissions.get(cloneFromRole);
    if (sourcePermissions) {
      setGrantedPermissions(new Set(sourcePermissions));
      onChangeDetected();
    }
  };

  const handleExportExcel = () => {
    // TODO: Implement Excel export
    console.log('Exporting to Excel...');
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

  // Check for auto-enable warnings
  const getWarnings = (featureId: string) => {
    const warnings: string[] = [];
    const feature = permissionMatrix.find((f) => f.featureId === featureId);
    if (!feature) return warnings;

    feature.actions.forEach((action) => {
      if (
        grantedPermissions.has(action.permission.actionCode) &&
        action.permission.requires
      ) {
        action.permission.requires.forEach((requiredCode) => {
          if (!grantedPermissions.has(requiredCode)) {
            warnings.push(
              `${action.permission.actionCode} requires ${requiredCode}`
            );
          }
        });
      }
    });

    return warnings;
  };

  return (
    <div className="space-y-6">
      {/* Toolbar */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium">Clone from Role:</span>
                <Select value={cloneFromRole} onValueChange={setCloneFromRole}>
                  <SelectTrigger className="w-[250px]">
                    <SelectValue placeholder="Select a role" />
                  </SelectTrigger>
                  <SelectContent>
                    {mockRoles
                      .filter((r) => r.code !== role.code)
                      .map((r) => (
                        <SelectItem key={r.id} value={r.code}>
                          {r.name}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
                <Button
                  variant="neutral"
                  size="sm"
                  onClick={handleCloneFromRole}
                  disabled={!cloneFromRole}
                >
                  <Copy className="h-4 w-4 mr-2" />
                  Clone
                </Button>
              </div>
            </div>

            <Button variant="neutral" size="sm" onClick={handleExportExcel}>
              <Download className="h-4 w-4 mr-2" />
              Export Excel
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Permission Matrix */}
      <ScrollArea className="h-[700px]">
        <div className="space-y-6 pr-4">
          {permissionMatrix.map((feature) => {
            const warnings = getWarnings(feature.featureId);
            const allSelected = feature.actions.every((action) =>
              grantedPermissions.has(action.permission.actionCode)
            );
            const someSelected = feature.actions.some((action) =>
              grantedPermissions.has(action.permission.actionCode)
            );

            return (
              <Card key={feature.featureId}>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <Checkbox
                        checked={allSelected}
                        onCheckedChange={(checked) =>
                          handleSelectAllForFeature(
                            feature.featureId,
                            checked as boolean
                          )
                        }
                      />
                      <div>
                        <CardTitle className="text-lg">
                          {feature.featureName}
                        </CardTitle>
                        <p className="text-sm text-muted-foreground">
                          {feature.featureId}
                        </p>
                      </div>
                    </div>
                    <Badge variant="neutral">
                      {
                        feature.actions.filter((a) =>
                          grantedPermissions.has(a.permission.actionCode)
                        ).length
                      }{' '}
                      / {feature.actions.length}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent>
                  {/* Warnings */}
                  {warnings.length > 0 && (
                    <Alert variant="destructive" className="mb-4">
                      <AlertCircle className="h-4 w-4" />
                      <AlertDescription>
                        <ul className="list-disc list-inside space-y-1">
                          {warnings.map((warning, idx) => (
                            <li key={idx} className="text-sm">
                              {warning}
                            </li>
                          ))}
                        </ul>
                      </AlertDescription>
                    </Alert>
                  )}

                  {/* Permissions Table */}
                  <div className="space-y-2">
                    {feature.actions.map((action) => {
                      const isGranted = grantedPermissions.has(
                        action.permission.actionCode
                      );

                      return (
                        <div
                          key={action.permission.id}
                          className={`flex items-center justify-between p-3 border rounded-lg transition-colors ${
                            isGranted
                              ? 'bg-primary/5 border-primary/20'
                              : 'hover:bg-muted'
                          }`}
                        >
                          <div className="flex items-center gap-3 flex-1">
                            <Checkbox
                              checked={isGranted}
                              onCheckedChange={(checked) =>
                                handlePermissionToggle(
                                  action.permission.actionCode,
                                  checked as boolean
                                )
                              }
                              id={action.permission.id}
                            />
                            <label
                              htmlFor={action.permission.id}
                              className="flex-1 cursor-pointer"
                            >
                              <div className="font-medium">
                                {action.permission.actionCode}
                              </div>
                              <div className="text-sm text-muted-foreground">
                                {action.permission.description ||
                                  action.permission.actionLabel}
                              </div>
                              {action.permission.requires &&
                                action.permission.requires.length > 0 && (
                                  <div className="text-xs text-muted-foreground mt-1">
                                    Requires:{' '}
                                    {action.permission.requires.join(', ')}
                                  </div>
                                )}
                            </label>
                          </div>

                          {action.permission.riskLevel && (
                            <Badge
                              className={getRiskColor(
                                action.permission.riskLevel
                              )}
                              variant="neutral"
                            >
                              {action.permission.riskLevel}
                            </Badge>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </ScrollArea>

      {/* Summary */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center justify-between text-sm">
            <div>
              <span className="font-medium">Total Permissions:</span>{' '}
              {permissionMatrix.reduce(
                (acc, feature) => acc + feature.actions.length,
                0
              )}
            </div>
            <div>
              <span className="font-medium">Granted:</span>{' '}
              {grantedPermissions.size}
            </div>
            <div>
              <span className="font-medium">High Risk:</span>{' '}
              {Array.from(grantedPermissions).filter((code) => {
                const perm = permissionMatrix
                  .flatMap((f) => f.actions)
                  .find((a) => a.permission.actionCode === code);
                return (
                  perm?.permission.riskLevel === 'HIGH' ||
                  perm?.permission.riskLevel === 'CRITICAL'
                );
              }).length}
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
