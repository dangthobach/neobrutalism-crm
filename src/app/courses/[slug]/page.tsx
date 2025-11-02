"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Image from "next/image";
import { useCourseBySlug, useCourseStats } from "@/hooks/useCourses";
import { useModulesByCourse } from "@/hooks/useCourseModules";
import { useCreateEnrollment } from "@/hooks/useCourseEnrollments";
import { CourseStatusBadge } from "@/components/course/course-status-badge";
import { CourseLevelBadge } from "@/components/course/course-level-badge";
import {
  Clock,
  Users,
  Star,
  Award,
  BookOpen,
  Play,
  CheckCircle2,
  Lock,
  DollarSign,
  Globe,
  ChevronDown,
  ChevronRight,
} from "lucide-react";

export default function CourseDetailPage() {
  const params = useParams();
  const router = useRouter();
  const slug = params.slug as string;

  const [expandedModules, setExpandedModules] = useState<Set<string>>(new Set());

  // Queries
  const { data: course, isLoading } = useCourseBySlug(slug);
  const { data: modules } = useModulesByCourse(course?.id || "");

  // Mutations
  const enrollMutation = useCreateEnrollment();

  // Handlers
  const handleEnroll = async () => {
    if (!course) return;

    try {
      const enrollment = await enrollMutation.mutateAsync({
        courseId: course.id,
        userId: "current-user-id", // Replace with actual user ID
      });
      router.push(`/courses/${course.slug}/learn`);
    } catch (error) {
      console.error("Failed to enroll:", error);
    }
  };

  const toggleModule = (moduleId: string) => {
    const newExpanded = new Set(expandedModules);
    if (newExpanded.has(moduleId)) {
      newExpanded.delete(moduleId);
    } else {
      newExpanded.add(moduleId);
    }
    setExpandedModules(newExpanded);
  };

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-2xl font-black uppercase">Loading course...</div>
      </div>
    );
  }

  if (!course) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-2xl font-black uppercase">Course not found</div>
      </div>
    );
  }

  const formatPrice = () => {
    if (course.price === 0) return "Free";
    return `${course.currency} ${course.price.toFixed(2)}`;
  };

  const formatDuration = () => {
    const hours = Math.floor(course.duration / 60);
    const minutes = course.duration % 60;
    return `${hours}h ${minutes}m`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-yellow-50 to-green-50">
      <div className="container mx-auto px-4 py-12">
        {/* Hero Section */}
        <div className="mb-12 grid gap-8 lg:grid-cols-3">
          {/* Left Column - Course Info */}
          <div className="lg:col-span-2">
            <div className="mb-4 flex items-center gap-3">
              <CourseStatusBadge status={course.status} />
              <CourseLevelBadge level={course.level} />
              {course.categoryName && (
                <span className="rounded-full border-2 border-black bg-yellow-200 px-3 py-1 text-xs font-bold uppercase">
                  {course.categoryName}
                </span>
              )}
            </div>

            <h1 className="mb-4 text-5xl font-black uppercase leading-tight">
              {course.title}
            </h1>

            {course.summary && (
              <p className="mb-6 text-xl text-muted-foreground">{course.summary}</p>
            )}

            {/* Meta Info */}
            <div className="mb-6 flex flex-wrap items-center gap-6">
              <div className="flex items-center gap-2">
                <Star className="h-5 w-5 fill-yellow-400 text-yellow-400" />
                <span className="font-bold">{course.rating?.toFixed(1) || "N/A"}</span>
                <span className="text-sm text-muted-foreground">
                  ({course.reviewCount} reviews)
                </span>
              </div>
              <div className="flex items-center gap-2">
                <Users className="h-5 w-5" />
                <span className="font-bold">{course.enrollmentCount} students</span>
              </div>
              <div className="flex items-center gap-2">
                <Clock className="h-5 w-5" />
                <span className="font-bold">{formatDuration()}</span>
              </div>
              <div className="flex items-center gap-2">
                <Globe className="h-5 w-5" />
                <span className="font-bold">{course.language}</span>
              </div>
            </div>

            {/* Instructor */}
            {course.instructorName && (
              <div className="mb-8">
                <p className="text-sm font-bold uppercase text-muted-foreground">Instructor</p>
                <p className="text-lg font-black">{course.instructorName}</p>
              </div>
            )}
          </div>

          {/* Right Column - Enrollment Card */}
          <div className="lg:col-span-1">
            <div className="sticky top-8 border-2 border-black bg-white p-6 shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
              {/* Thumbnail */}
              {course.thumbnailUrl && (
                <div className="mb-6 overflow-hidden border-2 border-black">
                  <Image
                    src={course.thumbnailUrl}
                    alt={course.title}
                    width={400}
                    height={225}
                    className="h-auto w-full"
                  />
                </div>
              )}

              {/* Price */}
              <div className="mb-6 flex items-center justify-between">
                <span className="text-4xl font-black">{formatPrice()}</span>
                {course.certificateEnabled && (
                  <Award className="h-8 w-8 text-yellow-600" />
                )}
              </div>

              {/* Enroll Button */}
              <button
                onClick={handleEnroll}
                disabled={enrollMutation.isPending}
                className="mb-4 w-full border-2 border-black bg-green-400 py-4 font-black uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none disabled:opacity-50"
              >
                {enrollMutation.isPending ? "Enrolling..." : "Enroll Now"}
              </button>

              {/* Course Includes */}
              <div className="space-y-3 border-t-2 border-black pt-4">
                <p className="font-bold uppercase">This course includes:</p>
                <div className="flex items-center gap-2">
                  <Play className="h-4 w-4" />
                  <span className="text-sm">{formatDuration()} on-demand content</span>
                </div>
                <div className="flex items-center gap-2">
                  <BookOpen className="h-4 w-4" />
                  <span className="text-sm">{modules?.length || 0} modules</span>
                </div>
                {course.certificateEnabled && (
                  <div className="flex items-center gap-2">
                    <Award className="h-4 w-4" />
                    <span className="text-sm">Certificate of completion</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Tabs Content */}
        <div className="grid gap-8 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-8">
            {/* What You'll Learn */}
            {course.whatYouWillLearn && course.whatYouWillLearn.length > 0 && (
              <div className="border-2 border-black bg-green-50 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                <h2 className="mb-4 text-2xl font-black uppercase">What You&apos;ll Learn</h2>
                <ul className="grid gap-3 md:grid-cols-2">
                  {course.whatYouWillLearn.map((item, index) => (
                    <li key={index} className="flex items-start gap-2">
                      <CheckCircle2 className="mt-1 h-5 w-5 flex-shrink-0 text-green-600" />
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Course Description */}
            {course.description && (
              <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                <h2 className="mb-4 text-2xl font-black uppercase">About This Course</h2>
                <div className="prose max-w-none">
                  <p className="whitespace-pre-wrap">{course.description}</p>
                </div>
              </div>
            )}

            {/* Course Curriculum */}
            <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <h2 className="mb-4 text-2xl font-black uppercase">Course Curriculum</h2>
              <div className="space-y-2">
                {modules && modules.length > 0 ? (
                  modules
                    .sort((a, b) => a.displayOrder - b.displayOrder)
                    .map((module) => {
                      const isExpanded = expandedModules.has(module.id);
                      return (
                        <div
                          key={module.id}
                          className="border-2 border-black bg-white"
                        >
                          <button
                            onClick={() => toggleModule(module.id)}
                            className="flex w-full items-center justify-between p-4 transition-colors hover:bg-gray-50"
                          >
                            <div className="flex items-center gap-3">
                              {isExpanded ? (
                                <ChevronDown className="h-5 w-5" />
                              ) : (
                                <ChevronRight className="h-5 w-5" />
                              )}
                              {module.isLocked && <Lock className="h-4 w-4" />}
                              <span className="font-bold">{module.title}</span>
                            </div>
                            <div className="flex items-center gap-4">
                              <span className="text-sm text-muted-foreground">
                                {module.lessonCount} lessons
                              </span>
                              <span className="text-sm text-muted-foreground">
                                {Math.floor(module.duration / 60)}h {module.duration % 60}m
                              </span>
                            </div>
                          </button>
                          {isExpanded && (
                            <div className="border-t-2 border-black bg-gray-50 p-4">
                              {module.description && (
                                <p className="text-sm text-muted-foreground">
                                  {module.description}
                                </p>
                              )}
                              <p className="mt-2 text-sm text-muted-foreground">
                                Lesson content will be available after enrollment
                              </p>
                            </div>
                          )}
                        </div>
                      );
                    })
                ) : (
                  <p className="text-muted-foreground">No modules available yet</p>
                )}
              </div>
            </div>

            {/* Prerequisites */}
            {course.prerequisites && course.prerequisites.length > 0 && (
              <div className="border-2 border-black bg-yellow-50 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                <h2 className="mb-4 text-2xl font-black uppercase">Prerequisites</h2>
                <ul className="list-inside list-disc space-y-2">
                  {course.prerequisites.map((item, index) => (
                    <li key={index}>{item}</li>
                  ))}
                </ul>
              </div>
            )}

            {/* Tags */}
            {course.tags && course.tags.length > 0 && (
              <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                <h2 className="mb-4 text-2xl font-black uppercase">Tags</h2>
                <div className="flex flex-wrap gap-2">
                  {course.tags.map((tag) => (
                    <span
                      key={tag}
                      className="rounded-full border-2 border-black bg-purple-200 px-3 py-1 text-xs font-bold uppercase"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
