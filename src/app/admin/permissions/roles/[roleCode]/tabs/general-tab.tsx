'use client';

import { useState, useEffect } from 'react';
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
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Role, RoleStatus } from '@/lib/api/roles';
import { User } from '@/lib/api/users';
import { useUpdateRole } from '@/hooks/useRoles';
import { useRevokeRoleFromUser } from '@/hooks/useUserRoles';
import { X, UserPlus, Loader2, Save } from 'lucide-react';
import { toast } from 'sonner';

interface RoleGeneralTabProps {
  role: Role;
  usersWithRole: User[];
  userRoleAssignments: Array<{ id: string; userId: string; roleId: string }>;
  onChangeDetected: () => void;
}

export default function RoleGeneralTab({
  role,
  usersWithRole,
  userRoleAssignments,
  onChangeDetected,
}: RoleGeneralTabProps) {
  const [roleName, setRoleName] = useState(role.name);
  const [description, setDescription] = useState(role.description || '');
  const [status, setStatus] = useState(role.status);
  const [hasChanges, setHasChanges] = useState(false);
  const [userToRemove, setUserToRemove] = useState<{ userId: string; userRoleId: string } | null>(null);

  // Update mutation
  const updateRoleMutation = useUpdateRole();
  const revokeRoleMutation = useRevokeRoleFromUser();

  // Reset form when role changes
  useEffect(() => {
    setRoleName(role.name);
    setDescription(role.description || '');
    setStatus(role.status);
    setHasChanges(false);
  }, [role]);

  const handleChange = () => {
    setHasChanges(true);
    onChangeDetected();
  };

  const handleSaveChanges = async () => {
    try {
      await updateRoleMutation.mutateAsync({
        id: role.id,
        data: {
          code: role.code,
          name: roleName,
          description,
          organizationId: role.organizationId,
          isSystem: role.isSystem,
          priority: role.priority,
        },
      });
      setHasChanges(false);
      toast.success('Role updated successfully');
    } catch (error) {
      toast.error('Failed to update role');
    }
  };

  const handleRemoveUser = (userId: string) => {
    // Find the user-role assignment ID
    const assignment = userRoleAssignments.find(
      (ur) => ur.userId === userId && ur.roleId === role.id
    );
    if (assignment) {
      setUserToRemove({ userId, userRoleId: assignment.id });
    }
  };

  const confirmRemoveUser = async () => {
    if (!userToRemove) return;

    try {
      await revokeRoleMutation.mutateAsync(userToRemove.userRoleId);
      toast.success('User removed from role successfully');
      setUserToRemove(null);
    } catch (error) {
      toast.error('Failed to remove user from role');
    }
  };

  const getRemovedUserName = () => {
    if (!userToRemove) return '';
    const user = usersWithRole.find((u) => u.id === userToRemove.userId);
    return user ? `${user.firstName} ${user.lastName}` : 'this user';
  };

  return (
    <div className="space-y-6">
      {/* Role Info */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Role Information</CardTitle>
            {hasChanges && (
              <Button
                onClick={handleSaveChanges}
                disabled={updateRoleMutation.isPending}
                size="sm"
              >
                {updateRoleMutation.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Saving...
                  </>
                ) : (
                  <>
                    <Save className="h-4 w-4 mr-2" />
                    Save Changes
                  </>
                )}
              </Button>
            )}
          </div>
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

          {/* Priority (read-only) */}
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

          {/* Metadata */}
          <div className="space-y-2 pt-4 border-t">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="font-medium text-muted-foreground">Created At:</span>
                <p>{new Date(role.createdAt).toLocaleString()}</p>
              </div>
              {role.createdBy && (
                <div>
                  <span className="font-medium text-muted-foreground">Created By:</span>
                  <p>{role.createdBy}</p>
                </div>
              )}
              {role.updatedAt && (
                <div>
                  <span className="font-medium text-muted-foreground">Updated At:</span>
                  <p>{new Date(role.updatedAt).toLocaleString()}</p>
                </div>
              )}
              {role.updatedBy && (
                <div>
                  <span className="font-medium text-muted-foreground">Updated By:</span>
                  <p>{role.updatedBy}</p>
                </div>
              )}
            </div>
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
                          user.status === 'ACTIVE' ? 'default' : 'neutral'
                        }
                      >
                        {user.status}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="noShadow"
                        size="sm"
                        onClick={() => handleRemoveUser(user.id)}
                        disabled={revokeRoleMutation.isPending}
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

      {/* Remove User Confirmation Dialog */}
      <AlertDialog open={!!userToRemove} onOpenChange={(open) => !open && setUserToRemove(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Remove User from Role</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to remove {getRemovedUserName()} from the role "{role.name}"?
              This will revoke all permissions associated with this role for the user.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={confirmRemoveUser}
              disabled={revokeRoleMutation.isPending}
            >
              {revokeRoleMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Removing...
                </>
              ) : (
                'Remove'
              )}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
