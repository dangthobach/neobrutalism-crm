"use client"

import { useState, useMemo } from "react"
import { useParams, useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Loader2, Trash2, Shield, ArrowLeft, Plus } from "lucide-react"
import { useRolesByGroup, useAssignRoleToGroup, useRevokeRoleFromGroup } from "@/hooks/useGroupRoles"
import { useGroup } from "@/hooks/useGroups"
import { useRoles } from "@/hooks/useRoles"
import { format } from "date-fns"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

export default function GroupRolesPage() {
  const params = useParams()
  const router = useRouter()
  const groupId = params?.groupId as string

  const [assignDialogOpen, setAssignDialogOpen] = useState(false)
  const [selectedRoleId, setSelectedRoleId] = useState<string>("")

  // Fetch group details
  const { data: group, isLoading: groupLoading } = useGroup(groupId)

  // Fetch group's roles
  const { data: groupRoles, isLoading: rolesLoading, refetch } = useRolesByGroup(groupId)

  // Fetch all available roles
  const { data: allRolesData } = useRoles({ page: 0, size: 100 })
  const allRoles = allRolesData?.content || []

  // Mutations
  const assignMutation = useAssignRoleToGroup()
  const revokeMutation = useRevokeRoleFromGroup()

  // Filter out roles that are already assigned
  const availableRoles = useMemo(() => {
    const assignedRoleIds = new Set(groupRoles?.map(gr => gr.roleId) || [])
    return allRoles.filter(role => !assignedRoleIds.has(role.id))
  }, [allRoles, groupRoles])

  async function handleAssignRole() {
    if (!selectedRoleId) return

    await assignMutation.mutateAsync({
      groupId,
      roleId: selectedRoleId,
    })

    setAssignDialogOpen(false)
    setSelectedRoleId("")
    refetch()
  }

  async function handleRevokeRole(groupRoleId: string) {
    if (!confirm("Are you sure you want to revoke this role from the group?")) return
    await revokeMutation.mutateAsync(groupRoleId)
    refetch()
  }

  if (groupLoading || rolesLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
        <div className="flex items-center gap-4">
          <Button
            variant="noShadow"
            size="sm"
            onClick={() => router.push("/admin/groups")}
            className="bg-background text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div className="flex-1">
            <h1 className="text-2xl font-heading">Group Roles</h1>
            <p className="text-sm font-base mt-1 opacity-90">
              {group?.name} ({group?.code})
            </p>
          </div>
          <Button
            onClick={() => setAssignDialogOpen(true)}
            className="bg-background text-foreground border-2 border-black shadow-[4px_4px_0_#000]"
            disabled={availableRoles.length === 0}
          >
            <Plus className="h-4 w-4 mr-2" />
            Assign Role
          </Button>
        </div>
      </header>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <h2 className="text-lg font-heading mb-4">Assigned Roles</h2>

        {!groupRoles || groupRoles.length === 0 ? (
          <div className="text-center py-12">
            <Shield className="h-12 w-12 mx-auto mb-4 text-foreground/30" />
            <p className="text-lg font-base text-foreground/60">No roles assigned</p>
            <p className="text-sm font-base text-foreground/40 mt-2">
              Click "Assign Role" to add roles to this group
            </p>
          </div>
        ) : (
          <div className="overflow-auto">
            <Table>
              <TableHeader className="font-heading">
                <TableRow className="bg-secondary-background border-b-2 border-black">
                  <TableHead className="font-heading">Role</TableHead>
                  <TableHead className="font-heading">Priority</TableHead>
                  <TableHead className="font-heading">Granted At</TableHead>
                  <TableHead className="font-heading">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {groupRoles.map((groupRole) => {
                  const role = allRoles.find(r => r.id === groupRole.roleId)
                  return (
                    <TableRow key={groupRole.id} className="border-b-2 border-black">
                      <TableCell className="font-base">
                        <div>
                          <div className="font-bold">{role?.name || 'Unknown'}</div>
                          <div className="text-sm text-foreground/60">{role?.code}</div>
                          {role?.description && (
                            <div className="text-sm text-foreground/50 mt-1">{role.description}</div>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="px-2 py-1 border-2 border-black text-xs font-base bg-blue-100">
                          {role?.priority || 0}
                        </span>
                      </TableCell>
                      <TableCell className="font-base">
                        {format(new Date(groupRole.grantedAt), "PPp")}
                      </TableCell>
                      <TableCell>
                        <Button
                          variant="noShadow"
                          size="sm"
                          onClick={() => handleRevokeRole(groupRole.id)}
                          disabled={revokeMutation.isPending}
                          className="bg-red-500 text-white border-2 border-black"
                        >
                          {revokeMutation.isPending ? (
                            <Loader2 className="h-3 w-3 animate-spin" />
                          ) : (
                            <Trash2 className="h-3 w-3" />
                          )}
                        </Button>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      <Dialog open={assignDialogOpen} onOpenChange={setAssignDialogOpen}>
        <DialogContent className="border-4 border-black shadow-[8px_8px_0_#000]">
          <DialogHeader>
            <DialogTitle className="font-heading text-2xl">Assign Role to Group</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="role" className="font-base">Select Role *</Label>
              <Select value={selectedRoleId} onValueChange={setSelectedRoleId}>
                <SelectTrigger className="border-2 border-black">
                  <SelectValue placeholder="Choose a role" />
                </SelectTrigger>
                <SelectContent>
                  {availableRoles.map((role) => (
                    <SelectItem key={role.id} value={role.id}>
                      {role.name} ({role.code}) - Priority: {role.priority}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter className="gap-2">
            <Button
              variant="noShadow"
              onClick={() => setAssignDialogOpen(false)}
              className="border-2 border-black"
            >
              Cancel
            </Button>
            <Button
              variant="noShadow"
              onClick={handleAssignRole}
              disabled={!selectedRoleId || assignMutation.isPending}
              className="border-2 border-black bg-main text-main-foreground"
            >
              {assignMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Assigning...
                </>
              ) : (
                "Assign Role"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
