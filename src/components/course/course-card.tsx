'use client'

import Image from 'next/image'
import { Card } from '@/components/ui/card'
import { CourseStatusBadge } from './course-status-badge'
import { CourseLevelBadge } from './course-level-badge'
import { Star, Users, Clock, DollarSign } from 'lucide-react'
import type { Course } from '@/types/course'

interface CourseCardProps {
  course: Course
  showProgress?: boolean
  progress?: number
}

export function CourseCard({ course, showProgress = false, progress = 0 }: CourseCardProps) {
  const formatPrice = (price: number, currency: string) => {
    if (price === 0) return 'Free'
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD',
    }).format(price)
  }

  const formatDuration = (minutes: number) => {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    if (hours === 0) return `${mins}m`
    if (mins === 0) return `${hours}h`
    return `${hours}h ${mins}m`
  }

  return (
    <Card className="group overflow-hidden border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none">
      {/* Thumbnail */}
      {course.thumbnailUrl && (
        <div className="relative h-48 overflow-hidden border-b-2 border-black">
          <Image
            src={course.thumbnailUrl}
            alt={course.title}
            fill
            className="object-cover transition-transform duration-300 group-hover:scale-110"
          />
          <div className="absolute right-2 top-2 flex flex-col gap-2">
            <CourseStatusBadge status={course.status} />
            <CourseLevelBadge level={course.level} />
          </div>
        </div>
      )}

      {/* Content */}
      <div className="p-4">
        {/* Category */}
        {course.categoryName && (
          <span className="mb-2 inline-block rounded border-2 border-black bg-yellow-200 px-2 py-1 text-xs font-bold">
            {course.categoryName}
          </span>
        )}

        {/* Title */}
        <h3 className="mb-2 line-clamp-2 text-lg font-black">{course.title}</h3>

        {/* Summary */}
        {course.summary && (
          <p className="mb-3 line-clamp-2 text-sm text-muted-foreground">{course.summary}</p>
        )}

        {/* Instructor */}
        {course.instructorName && (
          <p className="mb-3 text-xs text-muted-foreground">
            By <span className="font-bold">{course.instructorName}</span>
          </p>
        )}

        {/* Meta Info */}
        <div className="mb-3 flex flex-wrap gap-3 text-xs text-muted-foreground">
          {/* Rating */}
          {course.rating && course.rating > 0 && (
            <div className="flex items-center gap-1">
              <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
              <span className="font-bold">{course.rating.toFixed(1)}</span>
              {course.reviewCount > 0 && <span>({course.reviewCount})</span>}
            </div>
          )}

          {/* Enrollments */}
          <div className="flex items-center gap-1">
            <Users className="h-3 w-3" />
            <span>{course.enrollmentCount.toLocaleString()}</span>
          </div>

          {/* Duration */}
          {course.duration > 0 && (
            <div className="flex items-center gap-1">
              <Clock className="h-3 w-3" />
              <span>{formatDuration(course.duration)}</span>
            </div>
          )}
        </div>

        {/* Progress Bar (for enrolled courses) */}
        {showProgress && (
          <div className="mb-3">
            <div className="mb-1 flex items-center justify-between text-xs">
              <span className="font-bold">Progress</span>
              <span className="font-bold">{progress}%</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full border-2 border-black bg-gray-200">
              <div
                className="h-full bg-green-400 transition-all duration-300"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        )}

        {/* Price */}
        <div className="flex items-center justify-between border-t-2 border-black pt-3">
          <div className="flex items-center gap-1">
            <DollarSign className="h-4 w-4" />
            <span className="text-lg font-black">{formatPrice(course.price, course.currency)}</span>
          </div>
          {course.certificateEnabled && (
            <span className="rounded border-2 border-black bg-purple-200 px-2 py-1 text-xs font-bold">
              Certificate
            </span>
          )}
        </div>

        {/* Tags */}
        {course.tags && course.tags.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-1">
            {course.tags.slice(0, 3).map((tag) => (
              <span
                key={tag}
                className="rounded border border-black bg-gray-100 px-2 py-0.5 text-xs"
              >
                #{tag}
              </span>
            ))}
            {course.tags.length > 3 && (
              <span className="rounded border border-black bg-gray-100 px-2 py-0.5 text-xs">
                +{course.tags.length - 3}
              </span>
            )}
          </div>
        )}
      </div>
    </Card>
  )
}
