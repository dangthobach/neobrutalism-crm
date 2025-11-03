"use client";

import { useState } from "react";
import { CourseCard } from "@/components/course/course-card";
import { usePublishedCourses } from "@/hooks/useCourses";
import { CourseLevel, CourseFilters } from "@/types/course";
import { Search, Filter, BookOpen } from "lucide-react";

export default function CourseCatalogPage() {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<CourseFilters>({});
  const [showFilters, setShowFilters] = useState(false);

  // Query
  const { data: coursesData, isLoading } = usePublishedCourses(page, 12);

  // Apply client-side filters (in real app, pass to API)
  const filteredCourses = coursesData?.content.filter((course) => {
    if (filters.keyword) {
      const keyword = filters.keyword.toLowerCase();
      if (
        !course.title.toLowerCase().includes(keyword) &&
        !course.summary?.toLowerCase().includes(keyword)
      ) {
        return false;
      }
    }
    if (filters.level && course.level !== filters.level) return false;
    if (filters.categoryId && course.categoryId !== filters.categoryId) return false;
    if (filters.minPrice !== undefined && course.price < filters.minPrice) return false;
    if (filters.maxPrice !== undefined && course.price > filters.maxPrice) return false;
    return true;
  });

  return (
    <div className="min-h-screen bg-yellow-50">
      <div className="container mx-auto px-4 py-12">
        {/* Header */}
        <div className="mb-12 text-center">
          <h1 className="mb-4 text-6xl font-black uppercase">Course Catalog</h1>
          <p className="text-xl text-muted-foreground">
            Explore our collection of expert-led courses
          </p>
        </div>

        {/* Search Bar */}
        <div className="mx-auto mb-8 max-w-3xl">
          <div className="flex gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground" />
              <input
                type="text"
                placeholder="Search courses..."
                value={filters.keyword || ""}
                onChange={(e) => {
                  setFilters({ ...filters, keyword: e.target.value });
                  setPage(0);
                }}
                className="w-full border-2 border-black py-3 pl-12 pr-4 text-lg font-bold shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] focus:outline-none focus:ring-2 focus:ring-black"
              />
            </div>
            <button
              onClick={() => setShowFilters(!showFilters)}
              className="flex items-center gap-2 border-2 border-black bg-white px-6 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
            >
              <Filter className="h-5 w-5" />
              Filters
            </button>
          </div>
        </div>

        {/* Filters Panel */}
        {showFilters && (
          <div className="mx-auto mb-8 max-w-3xl border-2 border-black bg-white p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="grid gap-4 md:grid-cols-3">
              {/* Level Filter */}
              <div>
                <label className="mb-2 block text-sm font-bold uppercase">Level</label>
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

              {/* Price Filter */}
              <div>
                <label className="mb-2 block text-sm font-bold uppercase">Max Price</label>
                <input
                  type="number"
                  placeholder="Enter max price"
                  value={filters.maxPrice || ""}
                  onChange={(e) => {
                    setFilters({ ...filters, maxPrice: e.target.value ? Number(e.target.value) : undefined });
                    setPage(0);
                  }}
                  className="w-full border-2 border-black px-4 py-2 font-bold focus:outline-none focus:ring-2 focus:ring-black"
                />
              </div>

              {/* Category Filter */}
              <div>
                <label className="mb-2 block text-sm font-bold uppercase">Category</label>
                <input
                  type="text"
                  placeholder="Category..."
                  value={filters.categoryId || ""}
                  onChange={(e) => {
                    setFilters({ ...filters, categoryId: e.target.value });
                    setPage(0);
                  }}
                  className="w-full border-2 border-black px-4 py-2 font-bold focus:outline-none focus:ring-2 focus:ring-black"
                />
              </div>
            </div>

            {/* Clear Filters */}
            <div className="mt-4 flex justify-end">
              <button
                onClick={() => {
                  setFilters({});
                  setPage(0);
                }}
                className="border-2 border-black bg-gray-200 px-4 py-2 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
              >
                Clear Filters
              </button>
            </div>
          </div>
        )}

        {/* Course Grid */}
        {isLoading ? (
          <div className="flex items-center justify-center py-20">
            <div className="text-2xl font-black uppercase">Loading courses...</div>
          </div>
        ) : filteredCourses && filteredCourses.length > 0 ? (
          <>
            <div className="mb-4 text-lg font-bold">
              {filteredCourses.length} {filteredCourses.length === 1 ? "course" : "courses"} found
            </div>
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {filteredCourses.map((course) => (
                <CourseCard key={course.id} course={course} />
              ))}
            </div>

            {/* Pagination */}
            {coursesData && coursesData.totalPages > 1 && (
              <div className="mt-12 flex items-center justify-center gap-4">
                <button
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                  className="border-2 border-black bg-white px-6 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none disabled:opacity-50"
                >
                  Previous
                </button>
                <span className="text-lg font-bold">
                  Page {page + 1} of {coursesData.totalPages}
                </span>
                <button
                  onClick={() => setPage(page + 1)}
                  disabled={page >= coursesData.totalPages - 1}
                  className="border-2 border-black bg-white px-6 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            )}
          </>
        ) : (
          <div className="border-2 border-black bg-white p-20 text-center shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <BookOpen className="mx-auto h-16 w-16 text-muted-foreground" />
            <h3 className="mt-6 text-2xl font-black uppercase">No courses found</h3>
            <p className="mt-2 text-lg text-muted-foreground">
              Try adjusting your search or filters
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
