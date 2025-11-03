'use client'

import { useState } from 'react'
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
import { CourseStatusBadge } from './course-status-badge'
import { CourseLevelBadge } from './course-level-badge'
import { MoreVertical, Eye, Edit, Trash2, Archive, Upload, Copy, Users, Star } from 'lucide-react'
import type { Course } from '@/types/course'
import { CourseStatus } from '@/types/course'

interface CourseTableProps {
  courses: Course[]
  onView?: (course: Course) => void
  onEdit?: (course: Course) => void
  onDelete?: (course: Course) => void
  onPublish?: (course: Course) => void
  onUnpublish?: (course: Course) => void
  onArchive?: (course: Course) => void
  onDuplicate?: (course: Course) => void
  isLoading?: boolean
}

export function CourseTable({
  courses,
  onView,
  onEdit,
  onDelete,
  onPublish,
  onUnpublish,
  onArchive,
  onDuplicate,
  isLoading = false,
}: CourseTableProps) {
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [courseToDelete, setCourseToDelete] = useState<Course | null>(null)

  const handleDeleteClick = (course: Course) => {
    setCourseToDelete(course)
    setDeleteDialogOpen(true)
  }

  const handleDeleteConfirm = () => {
    if (courseToDelete && onDelete) {
      onDelete(courseToDelete)
    }
    setDeleteDialogOpen(false)
    setCourseToDelete(null)
  }

  const formatPrice = (price: number, currency: string) => {
    if (price === 0) return 'Free'
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD',
    }).format(price)
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="text-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
          <p className="mt-2 text-sm font-bold">Loading courses...</p>
        </div>
      </div>
    )
  }

  if (courses.length === 0) {
    return (
      <div className="rounded border-2 border-black bg-white p-8 text-center shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <p className="text-lg font-bold text-muted-foreground">No courses found</p>
      </div>
    )
  }

  return (
    <>
      <div className="overflow-hidden rounded border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <Table>
          <TableHeader>
            <TableRow className="border-b-2 border-black bg-gray-100 hover:bg-gray-100">
              <TableHead className="font-black">Course</TableHead>
              <TableHead className="font-black">Instructor</TableHead>
              <TableHead className="font-black">Level</TableHead>
              <TableHead className="font-black">Status</TableHead>
              <TableHead className="font-black">Price</TableHead>
              <TableHead className="font-black">Enrollments</TableHead>
              <TableHead className="font-black">Rating</TableHead>
              <TableHead className="font-black">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {courses.map((course) => (
              <TableRow
                key={course.id}
                className="cursor-pointer border-b border-black hover:bg-gray-50"
                onClick={() => onView && onView(course)}
              >
                <TableCell>
                  <div>
                    <p className="font-bold">{course.title}</p>
                    {course.categoryName && (
                      <span className="mt-1 inline-block rounded border border-black bg-yellow-100 px-2 py-0.5 text-xs">
                        {course.categoryName}
                      </span>
                    )}
                  </div>
                </TableCell>
                <TableCell>
                  <span className="text-sm">{course.instructorName || 'N/A'}</span>
                </TableCell>
                <TableCell>
                  <CourseLevelBadge level={course.level} />
                </TableCell>
                <TableCell>
                  <CourseStatusBadge status={course.status} />
                </TableCell>
                <TableCell>
                  <span className="font-bold">{formatPrice(course.price, course.currency)}</span>
                </TableCell>
                <TableCell>
                  <div className="flex items-center gap-1">
                    <Users className="h-4 w-4 text-muted-foreground" />
                    <span className="font-bold">{course.enrollmentCount.toLocaleString()}</span>
                  </div>
                </TableCell>
                <TableCell>
                  {course.rating && course.rating > 0 ? (
                    <div className="flex items-center gap-1">
                      <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                      <span className="font-bold">{course.rating.toFixed(1)}</span>
                      <span className="text-xs text-muted-foreground">({course.reviewCount})</span>
                    </div>
                  ) : (
                    <span className="text-sm text-muted-foreground">No ratings</span>
                  )}
                </TableCell>
                <TableCell onClick={(e) => e.stopPropagation()}>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="neutral" size="sm">
                        <MoreVertical className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="border-2 border-black">
                      {onView && (
                        <DropdownMenuItem onClick={() => onView(course)}>
                          <Eye className="mr-2 h-4 w-4" />
                          View
                        </DropdownMenuItem>
                      )}
                      {onEdit && (
                        <DropdownMenuItem onClick={() => onEdit(course)}>
                          <Edit className="mr-2 h-4 w-4" />
                          Edit
                        </DropdownMenuItem>
                      )}
                      {onPublish && course.status === CourseStatus.DRAFT && (
                        <DropdownMenuItem onClick={() => onPublish(course)}>
                          <Upload className="mr-2 h-4 w-4" />
                          Publish
                        </DropdownMenuItem>
                      )}
                      {onUnpublish && course.status === CourseStatus.PUBLISHED && (
                        <DropdownMenuItem onClick={() => onUnpublish(course)}>
                          <Archive className="mr-2 h-4 w-4" />
                          Unpublish
                        </DropdownMenuItem>
                      )}
                      {onArchive && course.status !== CourseStatus.ARCHIVED && (
                        <DropdownMenuItem onClick={() => onArchive(course)}>
                          <Archive className="mr-2 h-4 w-4" />
                          Archive
                        </DropdownMenuItem>
                      )}
                      {onDuplicate && (
                        <DropdownMenuItem onClick={() => onDuplicate(course)}>
                          <Copy className="mr-2 h-4 w-4" />
                          Duplicate
                        </DropdownMenuItem>
                      )}
                      {onDelete && (
                        <DropdownMenuItem
                          onClick={() => handleDeleteClick(course)}
                          className="text-red-600"
                        >
                          <Trash2 className="mr-2 h-4 w-4" />
                          Delete
                        </DropdownMenuItem>
                      )}
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent className="border-2 border-black">
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently delete the course &quot;{courseToDelete?.title}&quot;. This action cannot
              be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteConfirm} className="bg-red-500">
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
