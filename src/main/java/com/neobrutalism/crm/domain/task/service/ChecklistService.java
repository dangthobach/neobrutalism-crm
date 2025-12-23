package com.neobrutalism.crm.domain.task.service;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ErrorCode;
import com.neobrutalism.crm.domain.task.model.ChecklistItem;
import com.neobrutalism.crm.domain.task.model.Task;
import com.neobrutalism.crm.domain.task.repository.ChecklistItemRepository;
import com.neobrutalism.crm.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Service for managing task checklist items
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistItemRepository checklistItemRepository;
    private final TaskRepository taskRepository;

    /**
     * Add checklist item to task
     */
    @Transactional
    public ChecklistItem addItem(UUID taskId, String title) {
        log.info("Adding checklist item to task: {}", taskId);

        // Verify task exists
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Task not found"));

        // Get next position
        Integer maxPosition = checklistItemRepository.getMaxPosition(taskId);
        
        ChecklistItem item = new ChecklistItem();
        item.setTaskId(taskId);
        item.setTitle(title);
        item.setPosition(maxPosition + 1);
        // tenantId will be auto-set by @PrePersist

        ChecklistItem saved = checklistItemRepository.save(item);

        log.info("Checklist item added: {} to task: {}", saved.getId(), taskId);
        return saved;
    }

    /**
     * Update checklist item
     */
    @Transactional
    public ChecklistItem updateItem(UUID itemId, String title, Boolean completed) {
        log.info("Updating checklist item: {}", itemId);

        ChecklistItem item = findById(itemId);

        if (title != null) {
            item.setTitle(title);
        }
        
        if (completed != null) {
            if (completed) {
                item.markCompleted();
            } else {
                item.markIncomplete();
            }
        }

        ChecklistItem updated = checklistItemRepository.save(item);

        log.info("Checklist item updated: {}", itemId);
        return updated;
    }

    /**
     * Toggle checklist item completion
     */
    @Transactional
    public ChecklistItem toggleItem(UUID itemId) {
        log.info("Toggling checklist item: {}", itemId);

        ChecklistItem item = findById(itemId);
        item.toggleCompleted();

        ChecklistItem updated = checklistItemRepository.save(item);

        log.info("Checklist item toggled: {} to {}", itemId, updated.getCompleted());
        return updated;
    }

    /**
     * Delete checklist item
     */
    @Transactional
    public void deleteItem(UUID itemId) {
        log.info("Deleting checklist item: {}", itemId);

        ChecklistItem item = findById(itemId);
        item.softDelete();
        
        checklistItemRepository.save(item);

        log.info("Checklist item deleted: {}", itemId);
    }

    /**
     * Reorder checklist items
     */
    @Transactional
    public List<ChecklistItem> reorderItems(UUID taskId, List<UUID> itemIds) {
        log.info("Reordering checklist items for task: {}", taskId);

        // Get all items for the task
        List<ChecklistItem> items = checklistItemRepository.findByTaskIdAndDeletedFalseOrderByPositionAsc(taskId);

        // Create a map for quick lookup
        Map<UUID, ChecklistItem> itemMap = new java.util.HashMap<>();
        for (ChecklistItem item : items) {
            itemMap.put(item.getId(), item);
        }

        // Update positions based on new order
        IntStream.range(0, itemIds.size()).forEach(index -> {
            UUID itemId = itemIds.get(index);
            ChecklistItem item = itemMap.get(itemId);
            if (item != null) {
                item.setPosition(index);
            }
        });

        List<ChecklistItem> reordered = checklistItemRepository.saveAll(items);

        log.info("Reordered {} checklist items for task: {}", itemIds.size(), taskId);
        return reordered;
    }

    /**
     * Get all checklist items for a task
     */
    @Transactional(readOnly = true)
    public List<ChecklistItem> getItems(UUID taskId) {
        return checklistItemRepository.findByTaskIdAndDeletedFalseOrderByPositionAsc(taskId);
    }

    /**
     * Calculate checklist progress
     */
    @Transactional(readOnly = true)
    public Map<String, Object> calculateProgress(UUID taskId) {
        long total = checklistItemRepository.countByTaskIdAndDeletedFalse(taskId);
        long completed = checklistItemRepository.countByTaskIdAndCompletedAndDeletedFalse(taskId, true);
        
        int percentage = total > 0 ? (int) ((completed * 100) / total) : 0;

        Map<String, Object> progress = new java.util.HashMap<>();
        progress.put("total", total);
        progress.put("completed", completed);
        progress.put("remaining", total - completed);
        progress.put("percentage", percentage);

        return progress;
    }

    /**
     * Find checklist item by ID
     */
    @Transactional(readOnly = true)
    public ChecklistItem findById(UUID itemId) {
        return checklistItemRepository.findById(itemId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Checklist item not found"));
    }
}
