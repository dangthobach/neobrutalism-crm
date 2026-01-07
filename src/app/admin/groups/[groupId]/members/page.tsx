"use client"

import { useState, useMemo } from "react"
import { useParams, useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Loader2, Trash2, Users, ArrowLeft, Plus, Star } from "lucide-react"
import { useUsersByGroup, useAssignUserToGroup, useRemoveUserFromGroup, useSetPrimaryGroup } from "@/hooks/useUserGroups"
import { useGroup } from "@/hooks/useGroups"
import { useUsers } from "@/hooks/useUsers"
import { format } from "date-fns"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"

export default function GroupMembersPage() {
  const params = useParams()
  const router = useRouter()
  const groupId = params?.groupId as string

  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [selectedUserId, setSelectedUserId] = useState<string>("")
  const [isPrimary, setIsPrimary] = useState(false)

  // Fetch group details
  const { data: group, isLoading: groupLoading } = useGroup(groupId)

  // Fetch group's members
  const { data: userGroups, isLoading: membersLoading, refetch } = useUsersByGroup(groupId)

  // Fetch all available users
  const { data: allUsersData } = useUsers({ page: 0, size: 100 })
  const allUsers = useMemo(() => allUsersData?.content || [], [allUsersData])

  // Mutations
  const addMutation = useAssignUserToGroup()
  const removeMutation = useRemoveUserFromGroup()
  const setPrimaryMutation = useSetPrimaryGroup()

  // Filter out users that are already members
  const availableUsers = useMemo(() => {
    const memberUserIds = new Set(userGroups?.map(ug => ug.userId) || [])
    return allUsers.filter(user => !memberUserIds.has(user.id))
  }, [allUsers, userGroups])

  async function handleAddMember() {
    if (!selectedUserId) return

    await addMutation.mutateAsync({
      userId: selectedUserId,
      groupId,
      isPrimary,
    })

    setAddDialogOpen(false)
    setSelectedUserId("")
    setIsPrimary(false)
    refetch()
  }

  async function handleRemoveMember(userGroupId: string) {
    if (!confirm("Are you sure you want to remove this member from the group?")) return
    await removeMutation.mutateAsync(userGroupId)
    refetch()
  }

  async function handleSetPrimary(userGroupId: string) {
    await setPrimaryMutation.mutateAsync(userGroupId)
    refetch()
  }

  if (groupLoading || membersLoading) {
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
            <h1 className="text-2xl font-heading">Group Members</h1>
            <p className="text-sm font-base mt-1 opacity-90">
              {group?.name} ({group?.code})
            </p>
          </div>
          <Button
            onClick={() => setAddDialogOpen(true)}
            className="bg-background text-foreground border-2 border-black shadow-[4px_4px_0_#000]"
            disabled={availableUsers.length === 0}
          >
            <Plus className="h-4 w-4 mr-2" />
            Add Member
          </Button>
        </div>
      </header>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <h2 className="text-lg font-heading mb-4">Members ({userGroups?.length || 0})</h2>

        {!userGroups || userGroups.length === 0 ? (
          <div className="text-center py-12">
            <Users className="h-12 w-12 mx-auto mb-4 text-foreground/30" />
            <p className="text-lg font-base text-foreground/60">No members in this group</p>
            <p className="text-sm font-base text-foreground/40 mt-2">
              Click "Add Member" to add users to this group
            </p>
          </div>
        ) : (
          <div className="overflow-auto">
            <Table>
              <TableHeader className="font-heading">
                <TableRow className="bg-secondary-background border-b-2 border-black">
                  <TableHead className="font-heading">User</TableHead>
                  <TableHead className="font-heading">Primary Group</TableHead>
                  <TableHead className="font-heading">Joined At</TableHead>
                  <TableHead className="font-heading">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {userGroups.map((userGroup) => {
                  const user = allUsers.find(u => u.id === userGroup.userId)
                  return (
                    <TableRow key={userGroup.id} className="border-b-2 border-black">
                      <TableCell className="font-base">
                        <div className="flex items-center gap-2">
                          {userGroup.isPrimary && (
                            <Star className="h-4 w-4 fill-yellow-500 text-yellow-500" />
                          )}
                          <div>
                            <div className="font-bold">{user?.fullName || 'Unknown'}</div>
                            <div className="text-sm text-foreground/60">{user?.username} - {user?.email}</div>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        {userGroup.isPrimary ? (
                          <span className="px-2 py-1 border-2 border-black text-xs font-base bg-yellow-500 text-white">
                            YES
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
                          onClick={() => handleRemoveMember(userGroup.id)}
                          disabled={removeMutation.isPending || userGroup.isPrimary}
                          className="bg-red-500 text-white border-2 border-black disabled:opacity-50"
                          title={userGroup.isPrimary ? "Cannot remove user from primary group" : "Remove from group"}
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

      <Dialog open={addDialogOpen} onOpenChange={setAddDialogOpen}>
        <DialogContent className="border-4 border-black shadow-[8px_8px_0_#000]">
          <DialogHeader>
            <DialogTitle className="font-heading text-2xl">Add Member to Group</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="user" className="font-base">Select User *</Label>
              <Select value={selectedUserId} onValueChange={setSelectedUserId}>
                <SelectTrigger className="border-2 border-black">
                  <SelectValue placeholder="Choose a user" />
                </SelectTrigger>
                <SelectContent>
                  {availableUsers.map((user) => (
                    <SelectItem key={user.id} value={user.id}>
                      {user.fullName} ({user.username})
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
                Set as user's primary group
              </Label>
            </div>
          </div>
          <DialogFooter className="gap-2">
            <Button
              variant="noShadow"
              onClick={() => {
                setAddDialogOpen(false)
                setSelectedUserId("")
                setIsPrimary(false)
              }}
              className="border-2 border-black"
            >
              Cancel
            </Button>
            <Button
              variant="noShadow"
              onClick={handleAddMember}
              disabled={!selectedUserId || addMutation.isPending}
              className="border-2 border-black bg-main text-main-foreground"
            >
              {addMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Adding...
                </>
              ) : (
                "Add Member"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
