"use client"

import { useState, useMemo } from "react"
import { useParams, useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Loader2, Trash2, Users, ArrowLeft, Plus, Star } from "lucide-react"
import { useGroupsByUser, useAssignUserToGroup, useRemoveUserFromGroup, useSetPrimaryGroup } from "@/hooks/useUserGroups"
import { useUser } from "@/hooks/useUsers"
import { useGroups } from "@/hooks/useGroups"
import { format } from "date-fns"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"

export default function UserGroupsPage() {
  const params = useParams()
  const router = useRouter()
  const userId = params?.userId as string

  const [assignDialogOpen, setAssignDialogOpen] = useState(false)
  const [selectedGroupId, setSelectedGroupId] = useState<string>("")
  const [isPrimary, setIsPrimary] = useState(false)

  // Fetch user details
  const { data: user, isLoading: userLoading } = useUser(userId)

  // Fetch user's groups
  const { data: userGroups, isLoading: groupsLoading, refetch } = useGroupsByUser(userId)

  // Fetch all available groups
  const { data: allGroupsData } = useGroups({ page: 0, size: 100 })
  const allGroups = allGroupsData?.content || []

  // Mutations
  const assignMutation = useAssignUserToGroup()
  const removeMutation = useRemoveUserFromGroup()
  const setPrimaryMutation = useSetPrimaryGroup()

  // Filter out groups that user is already in
  const availableGroups = useMemo(() => {
    const assignedGroupIds = new Set(userGroups?.map(ug => ug.groupId) || [])
    return allGroups.filter(group => !assignedGroupIds.has(group.id))
  }, [allGroups, userGroups])

  async function handleAssignGroup() {
    if (!selectedGroupId) return

    await assignMutation.mutateAsync({
      userId,
      groupId: selectedGroupId,
      isPrimary,
    })

    setAssignDialogOpen(false)
    setSelectedGroupId("")
    setIsPrimary(false)
    refetch()
  }

  async function handleRemoveFromGroup(userGroupId: string) {
    if (!confirm("Are you sure you want to remove this user from this group?")) return
    await removeMutation.mutateAsync(userGroupId)
    refetch()
  }

  async function handleSetPrimary(userGroupId: string) {
    await setPrimaryMutation.mutateAsync(userGroupId)
    refetch()
  }

  if (userLoading || groupsLoading) {
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
            onClick={() => router.push("/admin/users")}
            className="bg-background text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div className="flex-1">
            <h1 className="text-2xl font-heading">User Groups</h1>
            <p className="text-sm font-base mt-1 opacity-90">
              {user?.fullName} ({user?.username})
            </p>
          </div>
          <Button
            onClick={() => setAssignDialogOpen(true)}
            className="bg-background text-foreground border-2 border-black shadow-[4px_4px_0_#000]"
            disabled={availableGroups.length === 0}
          >
            <Plus className="h-4 w-4 mr-2" />
            Add to Group
          </Button>
        </div>
      </header>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <h2 className="text-lg font-heading mb-4">Group Memberships</h2>

        {!userGroups || userGroups.length === 0 ? (
          <div className="text-center py-12">
            <Users className="h-12 w-12 mx-auto mb-4 text-foreground/30" />
            <p className="text-lg font-base text-foreground/60">Not in any groups</p>
            <p className="text-sm font-base text-foreground/40 mt-2">
              Click "Add to Group" to assign this user to groups
            </p>
          </div>
        ) : (
          <div className="overflow-auto">
            <Table>
              <TableHeader className="font-heading">
                <TableRow className="bg-secondary-background border-b-2 border-black">
                  <TableHead className="font-heading">Group</TableHead>
                  <TableHead className="font-heading">Primary</TableHead>
                  <TableHead className="font-heading">Joined At</TableHead>
                  <TableHead className="font-heading">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {userGroups.map((userGroup) => {
                  const group = allGroups.find(g => g.id === userGroup.groupId)
                  return (
                    <TableRow key={userGroup.id} className="border-b-2 border-black">
                      <TableCell className="font-base">
                        <div className="flex items-center gap-2">
                          {userGroup.isPrimary && (
                            <Star className="h-4 w-4 fill-yellow-500 text-yellow-500" />
                          )}
                          <div>
                            <div className="font-bold">{group?.name || 'Unknown'}</div>
                            <div className="text-sm text-foreground/60">{group?.code}</div>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        {userGroup.isPrimary ? (
                          <span className="px-2 py-1 border-2 border-black text-xs font-base bg-yellow-500 text-white">
                            PRIMARY
                          </span>
                        ) : (
                          <Button
                            variant="noShadow"
                            size="sm"
                            onClick={() => handleSetPrimary(userGroup.id)}
                            disabled={setPrimaryMutation.isPending}
                            className="text-xs"
                          >
                            Set Primary
                          </Button>
                        )}
                      </TableCell>
                      <TableCell className="font-base">
                        {format(new Date(userGroup.joinedAt), "PPp")}
                      </TableCell>
                      <TableCell>
                        <Button
                          variant="noShadow"
                          size="sm"
                          onClick={() => handleRemoveFromGroup(userGroup.id)}
                          disabled={removeMutation.isPending || userGroup.isPrimary}
                          className="bg-red-500 text-white border-2 border-black disabled:opacity-50"
                          title={userGroup.isPrimary ? "Cannot remove primary group" : "Remove from group"}
                        >
                          {removeMutation.isPending ? (
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
            <DialogTitle className="font-heading text-2xl">Add to Group</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="group" className="font-base">Select Group *</Label>
              <Select value={selectedGroupId} onValueChange={setSelectedGroupId}>
                <SelectTrigger className="border-2 border-black">
                  <SelectValue placeholder="Choose a group" />
                </SelectTrigger>
                <SelectContent>
                  {availableGroups.map((group) => (
                    <SelectItem key={group.id} value={group.id}>
                      {group.name} ({group.code})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex items-center space-x-2">
              <Checkbox
                id="isPrimary"
                checked={isPrimary}
                onCheckedChange={(checked) => setIsPrimary(checked as boolean)}
              />
              <Label htmlFor="isPrimary" className="font-base cursor-pointer">
                Set as primary group
              </Label>
            </div>
          </div>
          <DialogFooter className="gap-2">
            <Button
              variant="noShadow"
              onClick={() => {
                setAssignDialogOpen(false)
                setSelectedGroupId("")
                setIsPrimary(false)
              }}
              className="border-2 border-black"
            >
              Cancel
            </Button>
            <Button
              variant="noShadow"
              onClick={handleAssignGroup}
              disabled={!selectedGroupId || assignMutation.isPending}
              className="border-2 border-black bg-main text-main-foreground"
            >
              {assignMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Adding...
                </>
              ) : (
                "Add to Group"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
