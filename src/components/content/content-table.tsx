/**
 * Content Table Component
 * Displays content list with Neobrutalism styling
 */

'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
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
import { MoreVertical, Eye, Edit, Archive, Trash2, Globe } from 'lucide-react'
import { Content, ContentStatus } from '@/types/content'
import { ContentStatusBadge } from './content-status-badge'
import {
  useDeleteContent,
  usePublishContent,
  useUnpublishContent,
  useArchiveContent,
} from '@/hooks/useContent'

interface ContentTableProps {
  contents: Content[]
  isLoading?: boolean
}

export function ContentTable({ contents, isLoading }: ContentTableProps) {
  const router = useRouter()
  const [deleteId, setDeleteId] = useState<string | null>(null)

  const deleteContent = useDeleteContent()
  const publishContent = usePublishContent()
  const unpublishContent = useUnpublishContent()
  const archiveContent = useArchiveContent()

  const handleView = (id: string) => {
    router.push(`/admin/content/${id}`)
  }

  const handleEdit = (id: string) => {
    router.push(`/admin/content/${id}`)
  }

  const handlePublish = async (id: string) => {
    publishContent.mutate(id)
  }

  const handleUnpublish = async (id: string) => {
    unpublishContent.mutate(id)
  }

  const handleArchive = async (id: string) => {
    archiveContent.mutate(id)
  }

  const handleDelete = async () => {
    if (deleteId) {
      deleteContent.mutate(deleteId)
      setDeleteId(null)
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
      </div>
    )
  }

  if (contents.length === 0) {
    return (
      <div className="flex h-64 items-center justify-center rounded-lg border-2 border-black bg-gray-50">
        <p className="text-lg font-bold text-gray-500">No content found</p>
      </div>
    )
  }

  return (
    <>
      <div className="overflow-hidden rounded-lg border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <Table>
          <TableHeader>
            <TableRow className="border-b-2 border-black bg-yellow-200 hover:bg-yellow-300">
              <TableHead className="font-black">Title</TableHead>
              <TableHead className="font-black">Type</TableHead>
              <TableHead className="font-black">Author</TableHead>
              <TableHead className="font-black">Category</TableHead>
              <TableHead className="font-black">Status</TableHead>
              <TableHead className="font-black">Views</TableHead>
              <TableHead className="font-black">Published</TableHead>
              <TableHead className="w-[50px] font-black">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {contents.map((content) => (
              <TableRow
                key={content.id}
                className="cursor-pointer border-b-2 border-black hover:bg-gray-50"
                onClick={() => handleView(content.id)}
              >
                <TableCell className="font-bold">
                  <div className="flex items-center gap-2">
                    {content.featuredImageUrl && (
                      <span className="rounded border-2 border-black bg-yellow-300 px-2 py-0.5 text-xs font-black">
                        ★
                      </span>
                    )}
                    <span className="max-w-[300px] truncate">{content.title}</span>
                  </div>
                </TableCell>
                <TableCell>
                  <span className="rounded border-2 border-black bg-blue-200 px-2 py-1 text-xs font-bold">
                    {content.contentType}
                  </span>
                </TableCell>
                <TableCell className="text-sm">{content.authorName || 'N/A'}</TableCell>
                <TableCell className="text-sm">
                  {content.categories?.[0]?.name || 'N/A'}
                </TableCell>
                <TableCell>
                  <ContentStatusBadge status={content.status} />
                </TableCell>
                <TableCell className="font-mono text-sm">{content.viewCount || 0}</TableCell>
                <TableCell className="text-sm">
                  {content.publishedAt ? formatDate(content.publishedAt) : '-'}
                </TableCell>
                <TableCell onClick={(e) => e.stopPropagation()}>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="neutral" size="sm">
                        <MoreVertical className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                      align="end"
                      className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]"
                    >
                      <DropdownMenuItem onClick={() => handleView(content.id)}>
                        <Eye className="mr-2 h-4 w-4" />
                        View
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => handleEdit(content.id)}>
                        <Edit className="mr-2 h-4 w-4" />
                        Edit
                      </DropdownMenuItem>
                      {content.status === ContentStatus.DRAFT && (
                        <DropdownMenuItem onClick={() => handlePublish(content.id)}>
                          <Globe className="mr-2 h-4 w-4" />
                          Publish
                        </DropdownMenuItem>
                      )}
                      {content.status === ContentStatus.PUBLISHED && (
                        <DropdownMenuItem onClick={() => handleUnpublish(content.id)}>
                          <Globe className="mr-2 h-4 w-4" />
                          Unpublish
                        </DropdownMenuItem>
                      )}
                      <DropdownMenuItem onClick={() => handleArchive(content.id)}>
                        <Archive className="mr-2 h-4 w-4" />
                        Archive
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        onClick={() => setDeleteId(content.id)}
                        className="text-red-600"
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        Delete
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteId !== null} onOpenChange={() => setDeleteId(null)}>
        <AlertDialogContent className="border-2 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently delete this content. This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-red-500 hover:bg-red-600">
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
