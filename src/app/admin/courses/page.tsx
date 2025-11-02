"use client";

import { useState } from "react";
import Link from "next/link";
import { Plus, BookOpen, Users, Award, TrendingUp } from "lucide-react";
import { CourseTable } from "@/components/course/course-table";
import { useAllCourses, useDeleteCourse, usePublishCourse, useUnpublishCourse, useArchiveCourse, useDuplicateCourse } from "@/hooks/useCourses";
import { CourseFilters, CourseLevel, CourseStatus } from "@/types/course";

export default function CoursesPage() {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<CourseFilters>({});

  // Queries
  const { data: coursesData, isLoading } = useAllCourses(page, 10, filters);
  
  // Mutations
  const deleteMutation = useDeleteCourse();
  const publishMutation = usePublishCourse();
  const unpublishMutation = useUnpublishCourse();
  const archiveMutation = useArchiveCourse();
  const duplicateMutation = useDuplicateCourse();

  // Handlers
  const handleView = (course: any) => {
    window.location.href = `/admin/courses/${course.id}`;
  };

  const handleEdit = (course: any) => {
    window.location.href = `/admin/courses/${course.id}`;
  };

  const handleDelete = async (course: any) => {
    await deleteMutation.mutateAsync(course.id);
  };

  const handlePublish = async (course: any) => {
    await publishMutation.mutateAsync(course.id);
  };

  const handleUnpublish = async (course: any) => {
    await unpublishMutation.mutateAsync(course.id);
  };

  const handleArchive = async (course: any) => {
    await archiveMutation.mutateAsync(course.id);
  };

  const handleDuplicate = async (course: any) => {
    await duplicateMutation.mutateAsync(course.id);
  };

  // Calculate stats
  const totalCourses = coursesData?.totalElements || 0;
  const publishedCount = coursesData?.content.filter(c => c.status === CourseStatus.PUBLISHED).length || 0;
  const totalEnrollments = coursesData?.content.reduce((sum, c) => sum + c.enrollmentCount, 0) || 0;
  const avgRating = coursesData?.content.length 
    ? (coursesData.content.reduce((sum, c) => sum + (c.rating || 0), 0) / coursesData.content.length).toFixed(1)
    : "0.0";

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-4xl font-black uppercase">Courses</h1>
          <p className="mt-2 text-lg text-muted-foreground">
            Manage your learning courses
          </p>
        </div>
        <Link
          href="/admin/courses/new"
          className="group flex items-center gap-2 border-2 border-black bg-yellow-400 px-6 py-3 font-black uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
        >
          <Plus className="h-5 w-5" />
          New Course
        </Link>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-6 md:grid-cols-4">
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
              <p className="text-3xl font-black">{totalCourses}</p>
            </div>
          </div>
        </div>

        {/* Published */}
        <div className="border-2 border-black bg-green-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center gap-4">
            <div className="rounded-full border-2 border-black bg-green-300 p-3">
              <TrendingUp className="h-6 w-6" />
            </div>
            <div>
              <p className="text-sm font-bold uppercase text-muted-foreground">
                Published
              </p>
              <p className="text-3xl font-black">{publishedCount}</p>
            </div>
          </div>
        </div>

        {/* Total Enrollments */}
        <div className="border-2 border-black bg-purple-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center gap-4">
            <div className="rounded-full border-2 border-black bg-purple-300 p-3">
              <Users className="h-6 w-6" />
            </div>
            <div>
              <p className="text-sm font-bold uppercase text-muted-foreground">
                Enrollments
              </p>
              <p className="text-3xl font-black">{totalEnrollments}</p>
            </div>
          </div>
        </div>

        {/* Average Rating */}
        <div className="border-2 border-black bg-yellow-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center gap-4">
            <div className="rounded-full border-2 border-black bg-yellow-300 p-3">
              <Award className="h-6 w-6" />
            </div>
            <div>
              <p className="text-sm font-bold uppercase text-muted-foreground">
                Avg Rating
              </p>
              <p className="text-3xl font-black">{avgRating} ⭐</p>
            </div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="grid gap-4 md:grid-cols-4">
          {/* Search */}
          <div>
            <label className="mb-2 block text-sm font-bold uppercase">
              Search
            </label>
            <input
              type="text"
              placeholder="Search courses..."
              value={filters.keyword || ""}
              onChange={(e) => {
                setFilters({ ...filters, keyword: e.target.value });
                setPage(0);
              }}
              className="w-full border-2 border-black px-4 py-2 font-bold focus:outline-none focus:ring-2 focus:ring-black"
            />
          </div>

          {/* Level */}
          <div>
            <label className="mb-2 block text-sm font-bold uppercase">
              Level
            </label>
            <select
              value={filters.level || ""}
              onChange={(e) => {
                setFilters({ ...filters, level: e.target.value as CourseLevel || undefined });
                setPage(0);
              }}
              className="w-full border-2 border-black px-4 py-2 font-bold focus:outline-none focus:ring-2 focus:ring-black"
            >
              <option value="">All Levels</option>
              <option value={CourseLevel.BEGINNER}>🌱 Beginner</option>
              <option value={CourseLevel.INTERMEDIATE}>📚 Intermediate</option>
              <option value={CourseLevel.ADVANCED}>🎯 Advanced</option>
              <option value={CourseLevel.EXPERT}>🏆 Expert</option>
            </select>
          </div>

          {/* Status */}
          <div>
            <label className="mb-2 block text-sm font-bold uppercase">
              Status
            </label>
            <select
              value={filters.status || ""}
              onChange={(e) => {
                setFilters({ ...filters, status: e.target.value as CourseStatus || undefined });
                setPage(0);
              }}
              className="w-full border-2 border-black px-4 py-2 font-bold focus:outline-none focus:ring-2 focus:ring-black"
            >
              <option value="">All Statuses</option>
              <option value={CourseStatus.DRAFT}>Draft</option>
              <option value={CourseStatus.PUBLISHED}>Published</option>
              <option value={CourseStatus.ARCHIVED}>Archived</option>
            </select>
          </div>

          {/* Category */}
          <div>
            <label className="mb-2 block text-sm font-bold uppercase">
              Category
            </label>
            <input
              type="text"
              placeholder="Category ID..."
              value={filters.categoryId || ""}
              onChange={(e) => {
                setFilters({ ...filters, categoryId: e.target.value });
                setPage(0);
              }}
              className="w-full border-2 border-black px-4 py-2 font-bold focus:outline-none focus:ring-2 focus:ring-black"
            />
          </div>
        </div>
      </div>

      {/* Course Table */}
      <CourseTable
        courses={coursesData?.content || []}
        isLoading={isLoading}
        onView={handleView}
        onEdit={handleEdit}
        onDelete={handleDelete}
        onPublish={handlePublish}
        onUnpublish={handleUnpublish}
        onArchive={handleArchive}
        onDuplicate={handleDuplicate}
      />

      {/* Pagination */}
      {coursesData && coursesData.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="border-2 border-black bg-white px-4 py-2 font-bold uppercase disabled:opacity-50 hover:bg-gray-100"
          >
            Previous
          </button>
          <span className="px-4 py-2 font-bold">
            Page {page + 1} of {coursesData.totalPages}
          </span>
          <button
            onClick={() => setPage(page + 1)}
            disabled={page >= coursesData.totalPages - 1}
            className="border-2 border-black bg-white px-4 py-2 font-bold uppercase disabled:opacity-50 hover:bg-gray-100"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
