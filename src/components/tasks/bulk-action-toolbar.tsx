/**
 * Bulk Action Toolbar Component
 * Provides bulk operations for multiple selected tasks
 */

'use client'

import { useState } from 'react'
import { Users, GitBranch, Trash2, X, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'CANCELLED'

interface BulkActionToolbarProps {
  selectedCount: number
  onClearSelection: () => void
  onBulkAssign: (userId: string) => void
  onBulkStatusChange: (status: TaskStatus) => void
  onBulkDelete: () => void
  users?: Array<{ id: string; fullName: string }>
  isLoading?: boolean
}

export function BulkActionToolbar({
  selectedCount,
  onClearSelection,
  onBulkAssign,
  onBulkStatusChange,
  onBulkDelete,
  users = [],
  isLoading = false,
}: BulkActionToolbarProps) {
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [selectedUser, setSelectedUser] = useState<string>('')
  const [selectedStatus, setSelectedStatus] = useState<TaskStatus | ''>('')

  const handleBulkAssign = () => {
    if (!selectedUser) return
    onBulkAssign(selectedUser)
    setSelectedUser('')
  }

  const handleBulkStatusChange = () => {
    if (!selectedStatus) return
    onBulkStatusChange(selectedStatus as TaskStatus)
    setSelectedStatus('')
  }

  const handleBulkDelete = () => {
    onBulkDelete()
    setShowDeleteDialog(false)
  }

  if (selectedCount === 0) {
    return null
  }

  return (
    <>
      <div className="fixed bottom-6 left-1/2 z-50 -translate-x-1/2 transform">
        <div className="border-4 border-black bg-white shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center gap-4 p-4">
            {/* Selection Info */}
            <div className="flex items-center gap-2 border-r-2 border-black pr-4">
              <div className="flex h-10 w-10 items-center justify-center border-2 border-black bg-purple-200 font-black">
                {selectedCount}
              </div>
              <span className="font-bold">task được chọn</span>
            </div>

            {/* Bulk Assign */}
            <div className="flex items-center gap-2">
              <Users className="h-5 w-5" />
              <Select
                value={selectedUser}
                onValueChange={setSelectedUser}
                disabled={isLoading}
              >
                <SelectTrigger className="w-[180px] border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                  <SelectValue placeholder="Giao cho..." />
                </SelectTrigger>
                <SelectContent className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                  {users.map((user) => (
                    <SelectItem key={user.id} value={user.id} className="font-medium">
                      {user.fullName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {selectedUser && (
                <Button
                  size="sm"
                  onClick={handleBulkAssign}
                  disabled={isLoading}
                  className="border-2 border-black bg-purple-400 font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:bg-purple-500"
                >
                  Giao
                </Button>
              )}
            </div>

            {/* Bulk Status Change */}
            <div className="flex items-center gap-2 border-l-2 border-black pl-4">
              <GitBranch className="h-5 w-5" />
              <Select
                value={selectedStatus}
                onValueChange={(value) => setSelectedStatus(value as TaskStatus)}
                disabled={isLoading}
              >
                <SelectTrigger className="w-[180px] border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                  <SelectValue placeholder="Đổi trạng thái..." />
                </SelectTrigger>
                <SelectContent className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                  <SelectItem value="TODO" className="font-medium">
                    📋 Chưa làm
                  </SelectItem>
                  <SelectItem value="IN_PROGRESS" className="font-medium">
                    ⚡ Đang làm
                  </SelectItem>
                  <SelectItem value="IN_REVIEW" className="font-medium">
                    👀 Review
                  </SelectItem>
                  <SelectItem value="COMPLETED" className="font-medium">
                    ✅ Hoàn thành
                  </SelectItem>
                  <SelectItem value="CANCELLED" className="font-medium">
                    ❌ Hủy
                  </SelectItem>
                </SelectContent>
              </Select>
              {selectedStatus && (
                <Button
                  size="sm"
                  onClick={handleBulkStatusChange}
                  disabled={isLoading}
                  className="border-2 border-black bg-blue-400 font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:bg-blue-500"
                >
                  Đổi
                </Button>
              )}
            </div>

            {/* Bulk Delete */}
            <Button
              size="sm"
              variant="noShadow"
              onClick={() => setShowDeleteDialog(true)}
              disabled={isLoading}
              className="bg-red-500 hover:bg-red-600 text-white border-2 border-black font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            >
              <Trash2 className="mr-1 h-4 w-4" />
              Xóa
            </Button>

            {/* Clear Selection */}
            <div className="border-l-2 border-black pl-4">
              <Button
                size="sm"
                variant="neutral"
                onClick={onClearSelection}
                disabled={isLoading}
                className="border-2 border-black font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              >
                <X className="mr-1 h-4 w-4" />
                Bỏ chọn
              </Button>
            </div>

            {/* Loading Indicator */}
            {isLoading && (
              <Loader2 className="h-5 w-5 animate-spin text-purple-600" />
            )}
          </div>
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent className="border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-xl font-black uppercase">
              Xác nhận xóa
            </AlertDialogTitle>
            <AlertDialogDescription className="text-base font-medium">
              Bạn có chắc muốn xóa {selectedCount} task đã chọn?
              <br />
              <span className="font-bold text-red-600">
                Hành động này không thể hoàn tác!
              </span>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel
              className="border-2 border-black font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              disabled={isLoading}
            >
              Hủy
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleBulkDelete}
              disabled={isLoading}
              className="border-2 border-black bg-red-500 font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:bg-red-600"
            >
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang xóa...
                </>
              ) : (
                'Xóa ngay'
              )}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
