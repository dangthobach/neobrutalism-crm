"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { ArrowLeft, Users, TrendingUp, Award, Filter } from "lucide-react";
import Link from "next/link";
import { EnrollmentCard } from "@/components/course/enrollment-card";
import { useEnrollmentsByCourse, useIssueCertificate } from "@/hooks/useCourseEnrollments";
import { useCourseById } from "@/hooks/useCourses";
import { EnrollmentStatus } from "@/types/course";

export default function CourseStudentsPage() {
  const params = useParams();
  const courseId = params.id as string;

  const [statusFilter, setStatusFilter] = useState<EnrollmentStatus | "">("");

  // Queries
  const { data: course } = useCourseById(courseId);
  const { data: enrollments, isLoading } = useEnrollmentsByCourse(courseId);

  // Mutations
  const issueCertificateMutation = useIssueCertificate();

  // Handlers
  const handleViewDetails = (enrollment: any) => {
    // Navigate to enrollment detail or student profile
    console.log("View enrollment:", enrollment.id);
  };

  const handleIssueCertificate = async (enrollment: any) => {
    try {
      await issueCertificateMutation.mutateAsync(enrollment.id);
    } catch (error) {
      console.error("Failed to issue certificate:", error);
    }
  };

  // Get enrollment list from PageResponse
  const enrollmentList = enrollments?.content || [];

  // Filter enrollments
  const filteredEnrollments = enrollmentList.filter((enrollment: any) => {
    if (statusFilter && enrollment.status !== statusFilter) return false;
    return true;
  });

  // Calculate stats
  const totalEnrollments = enrollmentList.length || 0;
  const activeCount = enrollmentList.filter((e: any) => e.status === EnrollmentStatus.ACTIVE).length || 0;
  const completedCount = enrollmentList.filter((e: any) => e.status === EnrollmentStatus.COMPLETED).length || 0;
  const avgProgress = enrollmentList.length
    ? (enrollmentList.reduce((sum: number, e: any) => sum + e.progress, 0) / enrollmentList.length).toFixed(0)
    : "0";

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link
            href={`/admin/courses/${courseId}`}
            className="flex items-center gap-2 border-2 border-black bg-white p-2 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-4xl font-black uppercase">Course Students</h1>
            <p className="mt-2 text-lg text-muted-foreground">
              {course?.title}
            </p>
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-6 md:grid-cols-4">
        {/* Total Enrollments */}
        <div className="border-2 border-black bg-blue-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center gap-4">
            <div className="rounded-full border-2 border-black bg-blue-300 p-3">
              <Users className="h-6 w-6" />
            </div>
            <div>
              <p className="text-sm font-bold uppercase text-muted-foreground">Total Students</p>
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
              <p className="text-sm font-bold uppercase text-muted-foreground">Active</p>
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
              <p className="text-sm font-bold uppercase text-muted-foreground">Completed</p>
              <p className="text-3xl font-black">{completedCount}</p>
            </div>
          </div>
        </div>

        {/* Average Progress */}
        <div className="border-2 border-black bg-yellow-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center gap-4">
            <div className="rounded-full border-2 border-black bg-yellow-300 p-3">
              <TrendingUp className="h-6 w-6" />
            </div>
            <div>
              <p className="text-sm font-bold uppercase text-muted-foreground">Avg Progress</p>
              <p className="text-3xl font-black">{avgProgress}%</p>
            </div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="flex items-center gap-4">
          <Filter className="h-5 w-5" />
          <label className="text-sm font-bold uppercase">Filter by Status:</label>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as EnrollmentStatus | "")}
            className="border-2 border-black px-4 py-2 font-bold focus:outline-none focus:ring-2 focus:ring-black"
          >
            <option value="">All Statuses</option>
            <option value={EnrollmentStatus.ACTIVE}>Active</option>
            <option value={EnrollmentStatus.COMPLETED}>Completed</option>
            <option value={EnrollmentStatus.EXPIRED}>Expired</option>
            <option value={EnrollmentStatus.CANCELLED}>Cancelled</option>
          </select>
        </div>
      </div>

      {/* Enrollment Cards */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <div className="text-xl font-black uppercase">Loading...</div>
        </div>
      ) : filteredEnrollments && filteredEnrollments.length > 0 ? (
        <div className="grid gap-6 md:grid-cols-2">
          {filteredEnrollments.map((enrollment) => (
            <EnrollmentCard
              key={enrollment.id}
              enrollment={enrollment}
              onViewDetails={handleViewDetails}
              onIssueCertificate={handleIssueCertificate}
              showActions={true}
            />
          ))}
        </div>
      ) : (
        <div className="border-2 border-black bg-white p-12 text-center shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <Users className="mx-auto h-12 w-12 text-muted-foreground" />
          <h3 className="mt-4 text-xl font-black uppercase">No students found</h3>
          <p className="mt-2 text-muted-foreground">
            {statusFilter
              ? `No students with status "${statusFilter}"`
              : "No students have enrolled in this course yet."}
          </p>
        </div>
      )}
    </div>
  );
}
