"use client";

import { useState } from "react";
import Link from "next/link";
import { CourseCard } from "@/components/course/course-card";
import { useEnrollmentsByUser } from "@/hooks/useCourseEnrollments";
import { useCourseById } from "@/hooks/useCourses";
import { EnrollmentStatus } from "@/types/course";
import { BookOpen, TrendingUp, Award, Clock, Filter } from "lucide-react";

export default function MyCoursesPage() {
  const [statusFilter, setStatusFilter] = useState<EnrollmentStatus | "">("");

  // Replace with actual user ID
  const userId = "current-user-id";

  // Query
  const { data: enrollmentsData, isLoading } = useEnrollmentsByUser(userId);

  // Filter enrollments
  const enrollments = enrollmentsData?.content || [];
  const filteredEnrollments = enrollments.filter((enrollment: any) => {
    if (statusFilter && enrollment.status !== statusFilter) return false;
    return true;
  });

  // Calculate stats
  const totalEnrollments = enrollments.length;
  const activeCount = enrollments.filter((e: any) => e.status === EnrollmentStatus.ACTIVE).length;
  const completedCount = enrollments.filter((e: any) => e.status === EnrollmentStatus.COMPLETED).length;
  const avgProgress = enrollments.length
    ? (enrollments.reduce((sum: number, e: any) => sum + e.progress, 0) / enrollments.length).toFixed(0)
    : "0";

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-50 to-blue-50">
      <div className="container mx-auto px-4 py-12">
        {/* Header */}
        <div className="mb-12">
          <h1 className="mb-4 text-6xl font-black uppercase">My Courses</h1>
          <p className="text-xl text-muted-foreground">
            Track your learning progress and continue where you left off
          </p>
        </div>

        {/* Stats Cards */}
        <div className="mb-12 grid gap-6 md:grid-cols-4">
          {/* Total Courses */}
          <div className="border-2 border-black bg-blue-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="flex items-center gap-4">
              <div className="rounded-full border-2 border-black bg-blue-300 p-3">
                <BookOpen className="h-6 w-6" />
              </div>
              <div>
                <p className="text-sm font-bold uppercase text-muted-foreground">
                  Total Courses
                </p>
                <p className="text-3xl font-black">{totalEnrollments}</p>
              </div>
            </div>
          </div>

          {/* Active */}
          <div className="border-2 border-black bg-green-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="flex items-center gap-4">
              <div className="rounded-full border-2 border-black bg-green-300 p-3">
                <TrendingUp className="h-6 w-6" />
              </div>
              <div>
                <p className="text-sm font-bold uppercase text-muted-foreground">
                  In Progress
                </p>
                <p className="text-3xl font-black">{activeCount}</p>
              </div>
            </div>
          </div>

          {/* Completed */}
          <div className="border-2 border-black bg-purple-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="flex items-center gap-4">
              <div className="rounded-full border-2 border-black bg-purple-300 p-3">
                <Award className="h-6 w-6" />
              </div>
              <div>
                <p className="text-sm font-bold uppercase text-muted-foreground">
                  Completed
                </p>
                <p className="text-3xl font-black">{completedCount}</p>
              </div>
            </div>
          </div>

          {/* Average Progress */}
          <div className="border-2 border-black bg-yellow-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="flex items-center gap-4">
              <div className="rounded-full border-2 border-black bg-yellow-300 p-3">
                <Clock className="h-6 w-6" />
              </div>
              <div>
                <p className="text-sm font-bold uppercase text-muted-foreground">
                  Avg Progress
                </p>
                <p className="text-3xl font-black">{avgProgress}%</p>
              </div>
            </div>
          </div>
        </div>

        {/* Filter */}
        <div className="mb-8 border-2 border-black bg-white p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center gap-4">
            <Filter className="h-5 w-5" />
            <label className="text-sm font-bold uppercase">Filter by Status:</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as EnrollmentStatus | "")}
              className="border-2 border-black px-4 py-2 font-bold focus:outline-none focus:ring-2 focus:ring-black"
            >
              <option value="">All Courses</option>
              <option value={EnrollmentStatus.ACTIVE}>In Progress</option>
              <option value={EnrollmentStatus.COMPLETED}>Completed</option>
              <option value={EnrollmentStatus.EXPIRED}>Expired</option>
            </select>
            {statusFilter && (
              <button
                onClick={() => setStatusFilter("")}
                className="border-2 border-black bg-gray-200 px-4 py-2 font-bold uppercase shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-none"
              >
                Clear
              </button>
            )}
          </div>
        </div>

        {/* Course Grid */}
        {isLoading ? (
          <div className="flex items-center justify-center py-20">
            <div className="text-2xl font-black uppercase">Loading your courses...</div>
          </div>
        ) : filteredEnrollments.length > 0 ? (
          <>
            <div className="mb-4 text-lg font-bold">
              {filteredEnrollments.length}{" "}
              {filteredEnrollments.length === 1 ? "course" : "courses"}
            </div>
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {filteredEnrollments.map((enrollment: any) => (
                <EnrolledCourseCard
                  key={enrollment.id}
                  enrollment={enrollment}
                />
              ))}
            </div>
          </>
        ) : (
          <div className="border-2 border-black bg-white p-20 text-center shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <BookOpen className="mx-auto h-16 w-16 text-muted-foreground" />
            <h3 className="mt-6 text-2xl font-black uppercase">
              {statusFilter ? "No courses found" : "No courses yet"}
            </h3>
            <p className="mt-2 text-lg text-muted-foreground">
              {statusFilter
                ? `You don't have any courses with status "${statusFilter}"`
                : "Start learning by enrolling in a course"}
            </p>
            {!statusFilter && (
              <Link
                href="/courses"
                className="mt-6 inline-block border-2 border-black bg-yellow-400 px-6 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
              >
                Browse Courses
              </Link>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// Helper component to fetch and display course with enrollment data
function EnrolledCourseCard({ enrollment }: { enrollment: any }) {
  const { data: course } = useCourseById(enrollment.courseId);

  if (!course) {
    return (
      <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="text-center text-muted-foreground">Loading course...</div>
      </div>
    );
  }

  return (
    <Link href={`/courses/${course.slug}/learn`}>
      <CourseCard course={course} showProgress={true} progress={enrollment.progress} />
    </Link>
  );
}
