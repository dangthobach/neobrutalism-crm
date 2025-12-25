package com.neobrutalism.crm.domain.task.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.security.UserPrincipal;
import com.neobrutalism.crm.domain.task.dto.CommentRequest;
import com.neobrutalism.crm.domain.task.dto.CommentResponse;
import com.neobrutalism.crm.domain.task.model.Comment;
import com.neobrutalism.crm.domain.task.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for task comments
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Comments", description = "Task comment management APIs")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add comment to task", description = "Create a new comment or reply")
    public ApiResponse<CommentResponse> addComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Add comment to task: {} by user: {}", taskId, userPrincipal.getId());

        Comment comment = commentService.addComment(
            taskId,
            userPrincipal.getId(),
            request.getContent(),
            request.getParentId()
        );

        return ApiResponse.success("Comment added successfully", CommentResponse.from(comment));
    }

    @GetMapping("/{taskId}/comments")
    @Operation(summary = "Get task comments", description = "Get all comments for a task")
    public ApiResponse<List<CommentResponse>> getComments(
            @PathVariable UUID taskId
    ) {
        List<Comment> comments = commentService.getComments(taskId);
        List<CommentResponse> responses = comments.stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
        
        return ApiResponse.success(responses);
    }

    @GetMapping("/{taskId}/comments/paginated")
    @Operation(summary = "Get task comments (paginated)", description = "Get comments with pagination")
    public ApiResponse<Page<CommentResponse>> getCommentsPaginated(
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentService.getComments(taskId, pageable);
        Page<CommentResponse> responsePage = commentPage.map(CommentResponse::from);
        
        return ApiResponse.success(responsePage);
    }

    @GetMapping("/{taskId}/comments/count")
    @Operation(summary = "Count comments", description = "Get comment count for a task")
    public ApiResponse<Long> countComments(@PathVariable UUID taskId) {
        long count = commentService.countComments(taskId);
        return ApiResponse.success(count);
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "Update comment", description = "Edit comment content")
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Update comment: {} by user: {}", commentId, userPrincipal.getId());

        Comment comment = commentService.updateComment(
            commentId,
            userPrincipal.getId(),
            request.getContent()
        );

        return ApiResponse.success("Comment updated successfully", CommentResponse.from(comment));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete comment", description = "Soft delete a comment")
    public ApiResponse<Void> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Delete comment: {} by user: {}", commentId, userPrincipal.getId());

        commentService.deleteComment(commentId, userPrincipal.getId());

        return ApiResponse.success("Comment deleted successfully");
    }
}
