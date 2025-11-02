"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { useCourseBySlug } from "@/hooks/useCourses";
import { useModulesByCourse } from "@/hooks/useCourseModules";
import { useLessonsByModule, useMarkLessonComplete } from "@/hooks/useCourseLessons";
import { useUserEnrollment, useEnrollmentProgress } from "@/hooks/useCourseEnrollments";
import { ProgressTracker } from "@/components/course/progress-tracker";
import {
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  CheckCircle2,
  Circle,
  Lock,
  Play,
  FileText,
  ClipboardList,
  Download,
} from "lucide-react";
import { LessonType } from "@/types/course";

export default function CourseLearnPage() {
  const params = useParams();
  const slug = params.slug as string;

  const [selectedModuleId, setSelectedModuleId] = useState<string | null>(null);
  const [selectedLessonId, setSelectedLessonId] = useState<string | null>(null);
  const [showProgress, setShowProgress] = useState(false);

  // Queries
  const { data: course } = useCourseBySlug(slug);
  const { data: modules } = useModulesByCourse(course?.id || "");
  const { data: enrollment } = useUserEnrollment(course?.id || "", "current-user-id");
  const { data: progress } = useEnrollmentProgress(enrollment?.id || "");
  
  // Get lessons for selected module
  const { data: lessons } = useLessonsByModule(selectedModuleId || "");

  // Mutations
  const markCompleteMutation = useMarkLessonComplete();

  // Get current lesson
  const currentLesson = lessons?.find((l) => l.id === selectedLessonId);

  // Handle lesson completion
  const handleMarkComplete = async () => {
    if (!selectedLessonId || !enrollment) return;
    try {
      await markCompleteMutation.mutateAsync({
        lessonId: selectedLessonId,
        enrollmentId: enrollment.id,
      });
    } catch (error) {
      console.error("Failed to mark lesson complete:", error);
    }
  };

  // Auto-select first module and lesson
  useState(() => {
    if (modules && modules.length > 0 && !selectedModuleId) {
      const firstModule = modules.sort((a, b) => a.displayOrder - b.displayOrder)[0];
      setSelectedModuleId(firstModule.id);
    }
  });

  useState(() => {
    if (lessons && lessons.length > 0 && !selectedLessonId) {
      const firstLesson = lessons.sort((a, b) => a.displayOrder - b.displayOrder)[0];
      setSelectedLessonId(firstLesson.id);
    }
  });

  // Get lesson icon
  const getLessonIcon = (type: LessonType) => {
    switch (type) {
      case LessonType.VIDEO:
        return <Play className="h-4 w-4" />;
      case LessonType.TEXT:
        return <FileText className="h-4 w-4" />;
      case LessonType.QUIZ:
        return <ClipboardList className="h-4 w-4" />;
      case LessonType.RESOURCE:
        return <Download className="h-4 w-4" />;
      default:
        return <Circle className="h-4 w-4" />;
    }
  };

  if (!course || !enrollment) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <div className="text-2xl font-black uppercase">Access Denied</div>
          <p className="mt-2 text-muted-foreground">
            You need to enroll in this course first
          </p>
          <Link
            href={`/courses/${slug}`}
            className="mt-4 inline-block border-2 border-black bg-yellow-400 px-6 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
          >
            View Course Details
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col">
      {/* Header */}
      <div className="border-b-2 border-black bg-white p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link
              href={`/courses/${slug}`}
              className="border-2 border-black bg-white p-2 shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-none"
            >
              <ChevronLeft className="h-5 w-5" />
            </Link>
            <div>
              <h1 className="font-black uppercase">{course.title}</h1>
              {currentLesson && (
                <p className="text-sm text-muted-foreground">{currentLesson.title}</p>
              )}
            </div>
          </div>
          <button
            onClick={() => setShowProgress(!showProgress)}
            className="border-2 border-black bg-green-200 px-4 py-2 font-bold uppercase shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-none"
          >
            {showProgress ? "Hide" : "Show"} Progress
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar - Curriculum */}
        <div className="w-80 overflow-y-auto border-r-2 border-black bg-white">
          <div className="p-4">
            <h2 className="mb-4 text-lg font-black uppercase">Course Content</h2>
            <div className="space-y-2">
              {modules &&
                modules
                  .sort((a, b) => a.displayOrder - b.displayOrder)
                  .map((module) => {
                    const isSelected = selectedModuleId === module.id;
                    const moduleLessons = isSelected ? lessons : [];

                    return (
                      <div key={module.id} className="border-2 border-black">
                        <button
                          onClick={() =>
                            setSelectedModuleId(isSelected ? null : module.id)
                          }
                          className="flex w-full items-center justify-between p-3 transition-colors hover:bg-gray-50"
                        >
                          <div className="flex items-center gap-2">
                            <ChevronDown
                              className={`h-4 w-4 transition-transform ${
                                !isSelected ? "-rotate-90" : ""
                              }`}
                            />
                            {module.isLocked ? (
                              <Lock className="h-4 w-4" />
                            ) : (
                              <Circle className="h-4 w-4" />
                            )}
                            <span className="text-left text-sm font-bold">
                              {module.title}
                            </span>
                          </div>
                          <span className="text-xs text-muted-foreground">
                            {module.lessonCount}
                          </span>
                        </button>

                        {isSelected && moduleLessons && (
                          <div className="border-t-2 border-black bg-gray-50">
                            {moduleLessons
                              .sort((a, b) => a.displayOrder - b.displayOrder)
                              .map((lesson) => {
                                const isLessonSelected = selectedLessonId === lesson.id;
                                return (
                                  <button
                                    key={lesson.id}
                                    onClick={() => setSelectedLessonId(lesson.id)}
                                    className={`flex w-full items-center gap-2 border-t border-gray-300 p-3 text-left transition-colors hover:bg-gray-100 ${
                                      isLessonSelected ? "bg-yellow-100" : ""
                                    }`}
                                  >
                                    {getLessonIcon(lesson.type)}
                                    <span className="flex-1 text-sm">{lesson.title}</span>
                                  </button>
                                );
                              })}
                          </div>
                        )}
                      </div>
                    );
                  })}
            </div>
          </div>
        </div>

        {/* Main Content Area */}
        <div className="flex-1 overflow-y-auto">
          {showProgress && progress ? (
            <div className="p-6">
              <ProgressTracker progress={progress} showModuleBreakdown={true} />
            </div>
          ) : currentLesson ? (
            <div className="p-6">
              {/* Lesson Header */}
              <div className="mb-6">
                <div className="mb-2 flex items-center gap-2">
                  {getLessonIcon(currentLesson.type)}
                  <span className="text-sm font-bold uppercase text-muted-foreground">
                    {currentLesson.type}
                  </span>
                </div>
                <h2 className="mb-2 text-3xl font-black uppercase">
                  {currentLesson.title}
                </h2>
                {currentLesson.description && (
                  <p className="text-muted-foreground">{currentLesson.description}</p>
                )}
              </div>

              {/* Lesson Content */}
              <div className="mb-6 border-2 border-black bg-white p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                {currentLesson.type === LessonType.VIDEO && currentLesson.videoUrl && (
                  <div className="aspect-video border-2 border-black bg-black">
                    <video
                      src={currentLesson.videoUrl}
                      controls
                      className="h-full w-full"
                    >
                      Your browser does not support the video tag.
                    </video>
                  </div>
                )}

                {currentLesson.type === LessonType.TEXT && currentLesson.content && (
                  <div className="prose max-w-none">
                    <div dangerouslySetInnerHTML={{ __html: currentLesson.content }} />
                  </div>
                )}

                {currentLesson.type === LessonType.RESOURCE &&
                  currentLesson.resourceUrls && (
                    <div className="space-y-2">
                      <h3 className="font-bold uppercase">Resources:</h3>
                      {currentLesson.resourceUrls.map((url, index) => (
                        <a
                          key={index}
                          href={url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="flex items-center gap-2 border-2 border-black bg-blue-100 p-3 transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
                        >
                          <Download className="h-4 w-4" />
                          <span className="font-bold">Download Resource {index + 1}</span>
                        </a>
                      ))}
                    </div>
                  )}

                {!currentLesson.content && !currentLesson.videoUrl && (
                  <p className="text-muted-foreground">No content available for this lesson</p>
                )}
              </div>

              {/* Lesson Actions */}
              <div className="flex items-center justify-between">
                <button
                  onClick={() => {
                    // Navigate to previous lesson
                  }}
                  className="flex items-center gap-2 border-2 border-black bg-white px-4 py-2 font-bold uppercase shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-none"
                >
                  <ChevronLeft className="h-4 w-4" />
                  Previous
                </button>

                <button
                  onClick={handleMarkComplete}
                  disabled={markCompleteMutation.isPending}
                  className="border-2 border-black bg-green-400 px-6 py-3 font-bold uppercase shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-none disabled:opacity-50"
                >
                  Mark as Complete
                </button>

                <button
                  onClick={() => {
                    // Navigate to next lesson
                  }}
                  className="flex items-center gap-2 border-2 border-black bg-white px-4 py-2 font-bold uppercase shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-none"
                >
                  Next
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          ) : (
            <div className="flex h-full items-center justify-center">
              <div className="text-center">
                <Play className="mx-auto h-16 w-16 text-muted-foreground" />
                <h3 className="mt-4 text-xl font-black uppercase">Select a Lesson</h3>
                <p className="mt-2 text-muted-foreground">
                  Choose a lesson from the sidebar to begin learning
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
