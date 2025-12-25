package com.neobrutalism.crm.domain.course.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.course.dto.LessonDTO;
import com.neobrutalism.crm.domain.course.model.Lesson;
import com.neobrutalism.crm.domain.course.model.LessonProgress;
import com.neobrutalism.crm.domain.course.repository.LessonRepository;
import com.neobrutalism.crm.domain.course.service.LessonProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Lesson and Lesson Progress management
 */
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
@Tag(name = "Lessons", description = "Lesson and progress tracking APIs")
public class LessonController {

    private final LessonProgressService lessonProgressService;
    private final LessonRepository lessonRepository;

    /**
     * Start a lesson
     */
    @PostMapping("/{lessonId}/start")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Start lesson", description = "Mark lesson as started and track progress")
    public ApiResponse<Void> startLesson(
            @PathVariable UUID lessonId,
            @RequestParam UUID enrollmentId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        lessonProgressService.startLesson(userId, lessonId, enrollmentId);
        return ApiResponse.success("Lesson started successfully");
    }

    /**
     * Complete a lesson
     */
    @PostMapping("/{lessonId}/complete")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Complete lesson", description = "Mark lesson as completed")
    public ApiResponse<Void> completeLesson(
            @PathVariable UUID lessonId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        lessonProgressService.completeLesson(userId, lessonId);
        return ApiResponse.success("Lesson completed successfully");
    }

    /**
     * Update video position
     */
    @PutMapping("/{lessonId}/video-position")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update video position", description = "Save current video playback position")
    public ApiResponse<Void> updateVideoPosition(
            @PathVariable UUID lessonId,
            @RequestParam Integer seconds,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        lessonProgressService.updateVideoPosition(userId, lessonId, seconds);
        return ApiResponse.success("Video position updated");
    }

    /**
     * Update completion percentage
     */
    @PutMapping("/{lessonId}/completion")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update completion", description = "Update lesson completion percentage")
    public ApiResponse<Void> updateCompletion(
            @PathVariable UUID lessonId,
            @RequestParam Integer percentage,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        lessonProgressService.updateCompletionPercentage(userId, lessonId, percentage);
        return ApiResponse.success("Completion updated");
    }

    /**
     * Add time spent on lesson
     */
    @PostMapping("/{lessonId}/time-spent")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Add time spent", description = "Track time spent on lesson")
    public ApiResponse<Void> addTimeSpent(
            @PathVariable UUID lessonId,
            @RequestParam Integer seconds,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        lessonProgressService.addTimeSpent(userId, lessonId, seconds);
        return ApiResponse.success("Time tracked");
    }

    /**
     * Get lesson progress
     */
    @GetMapping("/{lessonId}/progress")
    @Operation(summary = "Get lesson progress", description = "Get user's progress for a specific lesson")
    public ApiResponse<LessonProgress> getLessonProgress(
            @PathVariable UUID lessonId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        LessonProgress progress = lessonProgressService.getLessonProgress(userId, lessonId);
        return ApiResponse.success(progress);
    }

    /**
     * Get progress for all lessons in an enrollment
     */
    @GetMapping("/enrollment/{enrollmentId}/progress")
    @Operation(summary = "Get enrollment progress", description = "Get progress for all lessons in enrollment")
    public ApiResponse<List<LessonProgress>> getEnrollmentProgress(
            @PathVariable UUID enrollmentId) {

        List<LessonProgress> progressList = lessonProgressService.getProgressForEnrollment(enrollmentId);
        return ApiResponse.success(progressList);
    }

    /**
     * Get progress for a course
     */
    @GetMapping("/course/{courseId}/progress")
    @Operation(summary = "Get course progress", description = "Get user's progress across entire course")
    public ApiResponse<List<LessonProgress>> getCourseProgress(
            @PathVariable UUID courseId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        List<LessonProgress> progressList = lessonProgressService.getProgressForCourse(userId, courseId);
        return ApiResponse.success(progressList);
    }

    /**
     * Get completed lessons count
     */
    @GetMapping("/enrollment/{enrollmentId}/completed-count")
    @Operation(summary = "Get completed count", description = "Count completed lessons in enrollment")
    public ApiResponse<Long> getCompletedCount(@PathVariable UUID enrollmentId) {

        long count = lessonProgressService.getCompletedLessonsCount(enrollmentId);
        return ApiResponse.success(count);
    }

    /**
     * Get total time spent on course
     */
    @GetMapping("/course/{courseId}/time-spent")
    @Operation(summary = "Get time spent", description = "Get total time spent on course")
    public ApiResponse<Integer> getTotalTimeSpent(
            @PathVariable UUID courseId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        int totalSeconds = lessonProgressService.getTotalTimeSpent(userId, courseId);
        return ApiResponse.success(totalSeconds);
    }

    /**
     * Check if lesson is completed
     */
    @GetMapping("/{lessonId}/is-completed")
    @Operation(summary = "Check completion", description = "Check if user completed the lesson")
    public ApiResponse<Boolean> isLessonCompleted(
            @PathVariable UUID lessonId,
            @RequestHeader(value = "X-User-Id") UUID userId) {

        boolean isCompleted = lessonProgressService.isLessonCompleted(userId, lessonId);
        return ApiResponse.success(isCompleted);
    }
}
