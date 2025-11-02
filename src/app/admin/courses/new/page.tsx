"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Save, Upload } from "lucide-react";
import Link from "next/link";
import { CourseForm } from "@/components/course/course-form";
import { useCreateCourse, usePublishCourse } from "@/hooks/useCourses";
import { CreateCourseRequest, CourseStatus } from "@/types/course";
import { useForm } from "react-hook-form";

export default function NewCoursePage() {
  const router = useRouter();
  const [isPublishing, setIsPublishing] = useState(false);

  // Form
  const form = useForm<CreateCourseRequest>({
    defaultValues: {
      status: CourseStatus.DRAFT,
      price: 0,
      currency: "USD",
      duration: 0,
      language: "English",
      certificateEnabled: false,
      tags: [],
      whatYouWillLearn: [],
      prerequisites: [],
    },
  });

  // Mutations
  const createMutation = useCreateCourse();
  const publishMutation = usePublishCourse();

  // Handlers
  const handleSaveDraft = async (data: CreateCourseRequest) => {
    try {
      const course = await createMutation.mutateAsync({
        ...data,
        status: CourseStatus.DRAFT,
      });
      router.push(`/admin/courses/${course.id}`);
    } catch (error) {
      console.error("Failed to save course:", error);
    }
  };

  const handlePublish = async (data: CreateCourseRequest) => {
    try {
      setIsPublishing(true);
      // First create as draft
      const course = await createMutation.mutateAsync({
        ...data,
        status: CourseStatus.DRAFT,
      });
      // Then publish
      await publishMutation.mutateAsync(course.id);
      router.push(`/admin/courses/${course.id}`);
    } catch (error) {
      console.error("Failed to publish course:", error);
    } finally {
      setIsPublishing(false);
    }
  };

  const isLoading = createMutation.isPending || isPublishing;

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
            <h1 className="text-4xl font-black uppercase">New Course</h1>
            <p className="mt-2 text-lg text-muted-foreground">
              Create a new learning course
            </p>
          </div>
        </div>
      </div>

      {/* Form */}
      <form className="space-y-6">
        <CourseForm form={form as any} categories={[]} />

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-4 border-t-2 border-black pt-6">
          <Link
            href="/admin/courses"
            className="border-2 border-black bg-white px-6 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
          >
            Cancel
          </Link>
          <button
            type="button"
            onClick={form.handleSubmit(handleSaveDraft)}
            disabled={isLoading}
            className="flex items-center gap-2 border-2 border-black bg-gray-200 px-6 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none disabled:opacity-50"
          >
            <Save className="h-5 w-5" />
            Save Draft
          </button>
          <button
            type="button"
            onClick={form.handleSubmit(handlePublish)}
            disabled={isLoading}
            className="flex items-center gap-2 border-2 border-black bg-green-400 px-6 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none disabled:opacity-50"
          >
            <Upload className="h-5 w-5" />
            {isPublishing ? "Publishing..." : "Publish"}
          </button>
        </div>
      </form>
    </div>
  );
}
