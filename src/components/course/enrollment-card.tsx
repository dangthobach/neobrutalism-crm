'use client'

import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { User, Calendar, Clock, Award, TrendingUp } from 'lucide-react'
import type { CourseEnrollment } from '@/types/course'
import { EnrollmentStatus } from '@/types/course'

interface EnrollmentCardProps {
  enrollment: CourseEnrollment
  onViewDetails?: (enrollment: CourseEnrollment) => void
  onIssueCertificate?: (enrollment: CourseEnrollment) => void
  showActions?: boolean
}

export function EnrollmentCard({
  enrollment,
  onViewDetails,
  onIssueCertificate,
  showActions = true,
}: EnrollmentCardProps) {
  const getStatusColor = (status: EnrollmentStatus) => {
    switch (status) {
      case EnrollmentStatus.ACTIVE:
        return 'bg-blue-200 border-blue-400 text-blue-800'
      case EnrollmentStatus.COMPLETED:
        return 'bg-green-200 border-green-400 text-green-800'
      case EnrollmentStatus.EXPIRED:
        return 'bg-gray-200 border-gray-400 text-gray-800'
      case EnrollmentStatus.CANCELLED:
        return 'bg-red-200 border-red-400 text-red-800'
      default:
        return 'bg-gray-200 border-gray-400 text-gray-800'
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  const progressPercentage = Math.round(enrollment.progress)
  const canIssueCertificate =
    enrollment.status === EnrollmentStatus.COMPLETED &&
    !enrollment.certificateIssued &&
    onIssueCertificate

  return (
    <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none">
      <div className="p-6">
        {/* Header */}
        <div className="mb-4 flex items-start justify-between">
          <div className="flex items-start gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-full border-2 border-black bg-purple-200">
              <User className="h-6 w-6" />
            </div>
            <div>
              <h3 className="font-black">{enrollment.userName || 'Unknown Student'}</h3>
              {enrollment.userEmail && (
                <p className="text-sm text-muted-foreground">{enrollment.userEmail}</p>
              )}
            </div>
          </div>

          {/* Status Badge */}
          <span
            className={`rounded-full border-2 border-black px-3 py-1 text-xs font-black uppercase ${getStatusColor(enrollment.status)}`}
          >
            {enrollment.status}
          </span>
        </div>

        {/* Course Name */}
        {enrollment.courseName && (
          <div className="mb-4 rounded border-2 border-black bg-yellow-50 p-3">
            <p className="text-sm text-muted-foreground">Enrolled in</p>
            <p className="font-bold">{enrollment.courseName}</p>
          </div>
        )}

        {/* Progress Bar */}
        <div className="mb-4">
          <div className="mb-2 flex items-center justify-between">
            <span className="text-sm font-bold">Progress</span>
            <span className="text-lg font-black">{progressPercentage}%</span>
          </div>
          <div className="h-3 overflow-hidden rounded-full border-2 border-black bg-gray-200">
            <div
              className="h-full bg-green-400 transition-all duration-300"
              style={{ width: `${progressPercentage}%` }}
            />
          </div>
          <div className="mt-1 flex items-center justify-between text-xs text-muted-foreground">
            <span>
              {enrollment.completedLessons} / {enrollment.totalLessons} lessons
            </span>
            {enrollment.status === EnrollmentStatus.COMPLETED && enrollment.completedAt && (
              <span>Completed {formatDate(enrollment.completedAt)}</span>
            )}
          </div>
        </div>

        {/* Meta Information */}
        <div className="mb-4 space-y-2 text-sm">
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-muted-foreground" />
            <span className="text-muted-foreground">
              Enrolled: <span className="font-bold">{formatDate(enrollment.enrolledAt)}</span>
            </span>
          </div>

          {enrollment.lastAccessedAt && (
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 text-muted-foreground" />
              <span className="text-muted-foreground">
                Last active: <span className="font-bold">{formatDate(enrollment.lastAccessedAt)}</span>
              </span>
            </div>
          )}

          {enrollment.expiresAt && enrollment.status === EnrollmentStatus.ACTIVE && (
            <div className="flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
              <span className="text-muted-foreground">
                Expires: <span className="font-bold">{formatDate(enrollment.expiresAt)}</span>
              </span>
            </div>
          )}

          {enrollment.certificateIssued && (
            <div className="flex items-center gap-2 rounded border-2 border-black bg-yellow-200 p-2">
              <Award className="h-4 w-4" />
              <span className="font-bold">Certificate Issued</span>
            </div>
          )}
        </div>

        {/* Actions */}
        {showActions && (
          <div className="flex gap-2 border-t-2 border-black pt-4">
            {onViewDetails && (
              <Button
                variant="default"
                size="sm"
                onClick={() => onViewDetails(enrollment)}
                className="flex-1"
              >
                View Details
              </Button>
            )}

            {canIssueCertificate && (
              <Button
                variant="default"
                size="sm"
                onClick={() => onIssueCertificate(enrollment)}
                className="flex-1 gap-1 bg-yellow-400 hover:bg-yellow-500"
              >
                <Award className="h-4 w-4" />
                Issue Certificate
              </Button>
            )}
          </div>
        )}
      </div>
    </Card>
  )
}
