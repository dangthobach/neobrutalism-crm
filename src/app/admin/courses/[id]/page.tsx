"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Save, Upload, Archive, Trash2, BookOpen, Users, BarChart3 } from "lucide-react";
import Link from "next/link";
import { CourseForm } from "@/components/course/course-form";
import { ModuleTree } from "@/components/course/module-tree";
import { CourseStatusBadge } from "@/components/course/course-status-badge";
import { CourseLevelBadge } from "@/components/course/course-level-badge";
import { useCourseById, useUpdateCourse, usePublishCourse, useUnpublishCourse, useArchiveCourse, useDeleteCourse, useCourseStats, useCourseEnrollmentStats } from "@/hooks/useCourses";
import { useModulesByCourse } from "@/hooks/useCourseModules";
import { UpdateCourseRequest, CourseStatus } from "@/types/course";
import { useForm } from "react-hook-form";

type TabType = "info" | "curriculum" | "analytics";

export default function CourseDetailPage() {
  const params = useParams();
  const router = useRouter();
  const courseId = params.id as string;
  
  const [activeTab, setActiveTab] = useState<TabType>("info");
  const [isEditing, setIsEditing] = useState(false);

  // Queries
  const { data: course, isLoading } = useCourseById(courseId);
  const { data: modules } = useModulesByCourse(courseId);
  const { data: stats } = useCourseStats();
  const { data: enrollmentStats } = useCourseEnrollmentStats(courseId);

  // Form
  const form = useForm<UpdateCourseRequest>({
    values: course ? {
      title: course.title,
      slug: course.slug,
      description: course.description,
      summary: course.summary,
      thumbnailUrl: course.thumbnailUrl,
      level: course.level,
      status: course.status,
      price: course.price,
      currency: course.currency,
      duration: course.duration,
      language: course.language,
      categoryId: course.categoryId,
      tags: course.tags,
      whatYouWillLearn: course.whatYouWillLearn,
      prerequisites: course.prerequisites,
      certificateEnabled: course.certificateEnabled,
    } : undefined,
  });

  // Mutations
  const updateMutation = useUpdateCourse();
  const publishMutation = usePublishCourse();
  const unpublishMutation = useUnpublishCourse();
  const archiveMutation = useArchiveCourse();
  const deleteMutation = useDeleteCourse();

  // Handlers
  const handleSave = async (data: UpdateCourseRequest) => {
    try {
      await updateMutation.mutateAsync({ id: courseId, data });
      setIsEditing(false);
    } catch (error) {
      console.error("Failed to update course:", error);
    }
  };

  const handlePublish = async () => {
    try {
      await publishMutation.mutateAsync(courseId);
    } catch (error) {
      console.error("Failed to publish course:", error);
    }
  };

  const handleUnpublish = async () => {
    try {
      await unpublishMutation.mutateAsync(courseId);
    } catch (error) {
      console.error("Failed to unpublish course:", error);
    }
  };

  const handleArchive = async () => {
    try {
      await archiveMutation.mutateAsync(courseId);
    } catch (error) {
      console.error("Failed to archive course:", error);
    }
  };

  const handleDelete = async () => {
    if (confirm("Are you sure you want to delete this course? This action cannot be undone.")) {
      try {
        await deleteMutation.mutateAsync(courseId);
        router.push("/admin/courses");
      } catch (error) {
        console.error("Failed to delete course:", error);
      }
    }
  };

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-2xl font-black uppercase">Loading...</div>
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

  const tabs = [
    { id: "info" as TabType, label: "Course Info", icon: BookOpen },
    { id: "curriculum" as TabType, label: "Curriculum", icon: BookOpen },
    { id: "analytics" as TabType, label: "Analytics", icon: BarChart3 },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link
            href="/admin/courses"
            className="flex items-center gap-2 border-2 border-black bg-white p-2 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-4xl font-black uppercase">{course.title}</h1>
              <CourseStatusBadge status={course.status} />
              <CourseLevelBadge level={course.level} />
            </div>
            <p className="mt-2 text-lg text-muted-foreground">
              {course.categoryName} • {course.enrollmentCount} students enrolled
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2">
          {course.status === CourseStatus.DRAFT && (
            <button
              onClick={handlePublish}
              className="flex items-center gap-2 border-2 border-black bg-green-400 px-4 py-2 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
            >
              <Upload className="h-4 w-4" />
              Publish
            </button>
          )}
          {course.status === CourseStatus.PUBLISHED && (
            <button
              onClick={handleUnpublish}
              className="flex items-center gap-2 border-2 border-black bg-gray-200 px-4 py-2 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
            >
              <Archive className="h-4 w-4" />
              Unpublish
            </button>
          )}
          <button
            onClick={handleArchive}
            className="flex items-center gap-2 border-2 border-black bg-blue-200 px-4 py-2 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
          >
            <Archive className="h-4 w-4" />
            Archive
          </button>
          <button
            onClick={handleDelete}
            className="flex items-center gap-2 border-2 border-black bg-red-200 px-4 py-2 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
          >
            <Trash2 className="h-4 w-4" />
            Delete
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="flex border-b-2 border-black">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex flex-1 items-center justify-center gap-2 border-r-2 border-black px-6 py-4 font-bold uppercase transition-colors last:border-r-0 ${
                  activeTab === tab.id
                    ? "bg-yellow-200"
                    : "bg-white hover:bg-gray-50"
                }`}
              >
                <Icon className="h-5 w-5" />
                {tab.label}
              </button>
            );
          })}
        </div>

        <div className="p-6">
          {/* Info Tab */}
          {activeTab === "info" && (
            <div className="space-y-6">
              {!isEditing ? (
                <>
                  <div className="flex items-center justify-between">
                    <h2 className="text-2xl font-black uppercase">Course Information</h2>
                    <button
                      onClick={() => setIsEditing(true)}
                      className="border-2 border-black bg-yellow-400 px-4 py-2 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
                    >
                      Edit
                    </button>
                  </div>

                  {/* Course Details Display */}
                  <div className="grid gap-6 md:grid-cols-2">
                    <div className="space-y-4">
                      <div>
                        <p className="text-sm font-bold uppercase text-muted-foreground">Title</p>
                        <p className="text-lg font-bold">{course.title}</p>
                      </div>
                      <div>
                        <p className="text-sm font-bold uppercase text-muted-foreground">Slug</p>
                        <p className="font-mono text-sm">{course.slug}</p>
                      </div>
                      <div>
                        <p className="text-sm font-bold uppercase text-muted-foreground">Summary</p>
                        <p>{course.summary || "No summary"}</p>
                      </div>
                      <div>
                        <p className="text-sm font-bold uppercase text-muted-foreground">Description</p>
                        <p>{course.description || "No description"}</p>
                      </div>
                    </div>

                    <div className="space-y-4">
                      <div>
                        <p className="text-sm font-bold uppercase text-muted-foreground">Price</p>
                        <p className="text-lg font-bold">
                          {course.price === 0 ? "Free" : `${course.currency} ${course.price.toFixed(2)}`}
                        </p>
                      </div>
                      <div>
                        <p className="text-sm font-bold uppercase text-muted-foreground">Duration</p>
                        <p className="font-bold">{course.duration} minutes</p>
                      </div>
                      <div>
                        <p className="text-sm font-bold uppercase text-muted-foreground">Language</p>
                        <p className="font-bold">{course.language}</p>
                      </div>
                      <div>
                        <p className="text-sm font-bold uppercase text-muted-foreground">Certificate</p>
                        <p className="font-bold">{course.certificateEnabled ? "Enabled" : "Disabled"}</p>
                      </div>
                      <div>
                        <p className="text-sm font-bold uppercase text-muted-foreground">Instructor</p>
                        <p className="font-bold">{course.instructorName || course.instructorId}</p>
                      </div>
                    </div>
                  </div>

                  {/* What You'll Learn */}
                  {course.whatYouWillLearn && course.whatYouWillLearn.length > 0 && (
                    <div>
                      <p className="mb-2 text-sm font-bold uppercase text-muted-foreground">What You&apos;ll Learn</p>
                      <ul className="list-inside list-disc space-y-1">
                        {course.whatYouWillLearn.map((item, index) => (
                          <li key={index}>{item}</li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {/* Prerequisites */}
                  {course.prerequisites && course.prerequisites.length > 0 && (
                    <div>
                      <p className="mb-2 text-sm font-bold uppercase text-muted-foreground">Prerequisites</p>
                      <ul className="list-inside list-disc space-y-1">
                        {course.prerequisites.map((item, index) => (
                          <li key={index}>{item}</li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {/* Tags */}
                  {course.tags && course.tags.length > 0 && (
                    <div>
                      <p className="mb-2 text-sm font-bold uppercase text-muted-foreground">Tags</p>
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
                </>
              ) : (
                <div className="space-y-6">
                  <CourseForm form={form as any} categories={[]} />
                  <div className="flex items-center justify-end gap-4 border-t-2 border-black pt-6">
                    <button
                      onClick={() => setIsEditing(false)}
                      className="border-2 border-black bg-white px-6 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={form.handleSubmit(handleSave)}
                      className="flex items-center gap-2 border-2 border-black bg-green-400 px-6 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
                    >
                      <Save className="h-5 w-5" />
                      Save Changes
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Curriculum Tab */}
          {activeTab === "curriculum" && (
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <h2 className="text-2xl font-black uppercase">Course Curriculum</h2>
                <p className="text-muted-foreground">
                  {modules?.length || 0} modules
                </p>
              </div>
              <ModuleTree modules={modules || []} courseId={courseId} />
            </div>
          )}

          {/* Analytics Tab */}
          {activeTab === "analytics" && (
            <div className="space-y-6">
              <h2 className="text-2xl font-black uppercase">Course Analytics</h2>
              
              {/* Stats Grid */}
              <div className="grid gap-6 md:grid-cols-3">
                <div className="border-2 border-black bg-blue-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                  <div className="flex items-center gap-4">
                    <div className="rounded-full border-2 border-black bg-blue-300 p-3">
                      <Users className="h-6 w-6" />
                    </div>
                    <div>
                      <p className="text-sm font-bold uppercase text-muted-foreground">Total Enrollments</p>
                      <p className="text-3xl font-black">{enrollmentStats?.totalEnrollments || 0}</p>
                    </div>
                  </div>
                </div>

                <div className="border-2 border-black bg-green-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                  <div className="flex items-center gap-4">
                    <div className="rounded-full border-2 border-black bg-green-300 p-3">
                      <Users className="h-6 w-6" />
                    </div>
                    <div>
                      <p className="text-sm font-bold uppercase text-muted-foreground">Active Students</p>
                      <p className="text-3xl font-black">{enrollmentStats?.activeEnrollments || 0}</p>
                    </div>
                  </div>
                </div>

                <div className="border-2 border-black bg-purple-100 p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                  <div className="flex items-center gap-4">
                    <div className="rounded-full border-2 border-black bg-purple-300 p-3">
                      <BarChart3 className="h-6 w-6" />
                    </div>
                    <div>
                      <p className="text-sm font-bold uppercase text-muted-foreground">Completion Rate</p>
                      <p className="text-3xl font-black">{enrollmentStats?.completionRate?.toFixed(0) || 0}%</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Additional Analytics */}
              <div className="border-2 border-black bg-white p-6">
                <h3 className="mb-4 text-xl font-black uppercase">Student Progress</h3>
                <p className="text-muted-foreground">
                  Average progress: {enrollmentStats?.averageProgress?.toFixed(1) || 0}%
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
