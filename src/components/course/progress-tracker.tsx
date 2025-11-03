'use client'

import { Card } from '@/components/ui/card'
import { CheckCircle2, Circle, Clock, Award, Target } from 'lucide-react'
import type { CourseProgressResponse } from '@/types/course'

interface ProgressTrackerProps {
  progress: CourseProgressResponse
  showModuleBreakdown?: boolean
}

export function ProgressTracker({ progress, showModuleBreakdown = true }: ProgressTrackerProps) {
  const progressPercentage = Math.round(progress.progress)
  
  const formatTime = (minutes: number) => {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    if (hours === 0) return `${mins}m`
    if (mins === 0) return `${hours}h`
    return `${hours}h ${mins}m`
  }

  return (
    <div className="space-y-6">
      {/* Overall Progress Card */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-green-200 px-6 py-4">
          <h3 className="text-lg font-black">Your Progress</h3>
        </div>
        <div className="p-6">
          {/* Progress Bar */}
          <div className="mb-6">
            <div className="mb-2 flex items-center justify-between">
              <span className="text-2xl font-black">{progressPercentage}%</span>
              <span className="text-sm text-muted-foreground">Complete</span>
            </div>
            <div className="h-4 overflow-hidden rounded-full border-2 border-black bg-gray-200">
              <div
                className="h-full bg-gradient-to-r from-green-400 to-green-500 transition-all duration-500"
                style={{ width: `${progressPercentage}%` }}
              />
            </div>
          </div>

          {/* Stats Grid */}
          <div className="grid gap-4 md:grid-cols-2">
            {/* Lessons */}
            <div className="rounded border-2 border-black bg-blue-50 p-4">
              <div className="flex items-center gap-3">
                <CheckCircle2 className="h-8 w-8 text-blue-600" />
                <div>
                  <p className="text-sm text-muted-foreground">Lessons</p>
                  <p className="text-xl font-black">
                    {progress.completedLessons} / {progress.totalLessons}
                  </p>
                </div>
              </div>
            </div>

            {/* Modules */}
            <div className="rounded border-2 border-black bg-purple-50 p-4">
              <div className="flex items-center gap-3">
                <Target className="h-8 w-8 text-purple-600" />
                <div>
                  <p className="text-sm text-muted-foreground">Modules</p>
                  <p className="text-xl font-black">
                    {progress.completedModules} / {progress.totalModules}
                  </p>
                </div>
              </div>
            </div>

            {/* Time Spent */}
            <div className="rounded border-2 border-black bg-yellow-50 p-4">
              <div className="flex items-center gap-3">
                <Clock className="h-8 w-8 text-yellow-600" />
                <div>
                  <p className="text-sm text-muted-foreground">Time Spent</p>
                  <p className="text-xl font-black">{formatTime(progress.timeSpent)}</p>
                </div>
              </div>
            </div>

            {/* Quizzes */}
            <div className="rounded border-2 border-black bg-green-50 p-4">
              <div className="flex items-center gap-3">
                <Award className="h-8 w-8 text-green-600" />
                <div>
                  <p className="text-sm text-muted-foreground">Quizzes Passed</p>
                  <p className="text-xl font-black">
                    {progress.quizzesPassed} / {progress.quizzesTotal}
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Certificate Eligibility */}
          {progress.certificateEligible && (
            <div className="mt-4 rounded border-2 border-black bg-gradient-to-r from-yellow-200 to-yellow-300 p-4">
              <div className="flex items-center gap-3">
                <Award className="h-8 w-8" />
                <div>
                  <p className="font-black">🎉 Congratulations!</p>
                  <p className="text-sm">You are eligible to receive your certificate!</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </Card>

      {/* Module Breakdown (Optional) */}
      {showModuleBreakdown && (
        <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
            <h3 className="text-lg font-black">Module Breakdown</h3>
          </div>
          <div className="divide-y-2 divide-black">
            {/* Placeholder - would map through actual modules */}
            <div className="p-4">
              <p className="text-sm text-muted-foreground">
                Detailed module progress will be displayed here.
              </p>
            </div>
          </div>
        </Card>
      )}

      {/* Next Steps */}
      {progress.lastAccessedLesson && (
        <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
            <h3 className="text-lg font-black">Continue Learning</h3>
          </div>
          <div className="p-6">
            <p className="text-sm text-muted-foreground">
              Pick up where you left off and continue your learning journey!
            </p>
          </div>
        </Card>
      )}
    </div>
  )
}
