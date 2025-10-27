'use client';

import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Role, User, RoleStatus } from '@/types/permission';
import { X, UserPlus } from 'lucide-react';

interface RoleGeneralTabProps {
  role: Role;
  usersWithRole: User[];
  onChangeDetected: () => void;
}

export default function RoleGeneralTab({
  role,
  usersWithRole,
  onChangeDetected,
}: RoleGeneralTabProps) {
  const [roleName, setRoleName] = useState(role.name);
  const [description, setDescription] = useState(role.description || '');
  const [status, setStatus] = useState(role.status);
  const [dataScopes, setDataScopes] = useState<string[]>(['WH_NORTH', 'WH_SOUTH']);

  const handleChange = () => {
    onChangeDetected();
  };

  const handleRemoveScope = (scope: string) => {
    setDataScopes(dataScopes.filter((s) => s !== scope));
    handleChange();
  };

  const handleRemoveUser = (userId: string) => {
    // TODO: Implement user removal
    console.log('Remove user:', userId);
    handleChange();
  };

  return (
    <div className="space-y-6">
      {/* Role Info */}
      <Card>
        <CardHeader>
          <CardTitle>Role Information</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Role Code (read-only) */}
          <div className="space-y-2">
            <Label htmlFor="roleCode">Role Code</Label>
            <Input
              id="roleCode"
              value={role.code}
              disabled
              className="bg-muted"
            />
            <p className="text-xs text-muted-foreground">
              Role code cannot be changed
            </p>
          </div>

          {/* Role Name */}
          <div className="space-y-2">
            <Label htmlFor="roleName">Role Name</Label>
            <Input
              id="roleName"
              value={roleName}
              onChange={(e) => {
                setRoleName(e.target.value);
                handleChange();
              }}
              placeholder="Enter role name"
              disabled={role.isSystem}
            />
          </div>

          {/* Description */}
          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Textarea
              id="description"
              value={description}
              onChange={(e) => {
                setDescription(e.target.value);
                handleChange();
              }}
              placeholder="Describe the purpose and responsibilities of this role"
              rows={4}
              disabled={role.isSystem}
            />
          </div>

          {/* Priority */}
          <div className="space-y-2">
            <Label htmlFor="priority">Priority</Label>
            <Input
              id="priority"
              type="number"
              value={role.priority}
              disabled
              className="bg-muted"
            />
            <p className="text-xs text-muted-foreground">
              Higher priority roles take precedence in conflict resolution
            </p>
          </div>

          {/* Data Scope */}
          <div className="space-y-2">
            <Label>Data Scope</Label>
            <div className="flex flex-wrap gap-2">
              {dataScopes.map((scope) => (
                <Badge key={scope} variant="secondary" className="pr-1">
                  {scope}
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-auto p-1 ml-1"
                    onClick={() => handleRemoveScope(scope)}
                  >
                    <X className="h-3 w-3" />
                  </Button>
                </Badge>
              ))}
              <Button variant="outline" size="sm">
                + Add Scope
              </Button>
            </div>
            <p className="text-xs text-muted-foreground">
              Define data visibility boundaries (e.g., warehouse regions, branches)
            </p>
          </div>

          {/* Status */}
          <div className="space-y-2">
            <Label>Status</Label>
            <RadioGroup
              value={status}
              onValueChange={(value) => {
                setStatus(value as RoleStatus);
                handleChange();
              }}
              disabled={role.isSystem}
            >
              <div className="flex items-center space-x-2">
                <RadioGroupItem value={RoleStatus.ACTIVE} id="active" />
                <Label htmlFor="active" className="font-normal cursor-pointer">
                  Active
                </Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value={RoleStatus.INACTIVE} id="inactive" />
                <Label htmlFor="inactive" className="font-normal cursor-pointer">
                  Inactive
                </Label>
              </div>
            </RadioGroup>
          </div>

          {role.isSystem && (
            <div className="bg-yellow-50 dark:bg-yellow-950 border border-yellow-200 dark:border-yellow-800 rounded-lg p-3">
              <p className="text-sm text-yellow-800 dark:text-yellow-200">
                This is a system role. Some fields are read-only to prevent
                accidental modification.
              </p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Users in this Role */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Users in this Role ({usersWithRole.length})</CardTitle>
            <Button size="sm">
              <UserPlus className="h-4 w-4 mr-2" />
              Add User
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {usersWithRole.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>User</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="w-[100px]">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {usersWithRole.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell>
                      <div>
                        <div className="font-medium">
                          {user.firstName} {user.lastName}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          @{user.username}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>
                      <Badge
                        variant={
                          user.status === 'ACTIVE' ? 'default' : 'secondary'
                        }
                      >
                        {user.status}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleRemoveUser(user.id)}
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <div className="text-center py-8 text-muted-foreground">
              No users assigned to this role
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
