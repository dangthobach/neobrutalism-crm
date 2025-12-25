package com.neobrutalism.crm.domain.content.handler;

import com.neobrutalism.crm.domain.content.event.*;
import com.neobrutalism.crm.domain.content.model.Content;
import com.neobrutalism.crm.domain.content.repository.ContentRepository;
import com.neobrutalism.crm.domain.content.service.ContentReadModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event handler for Content domain events
 * Handles CQRS read model synchronization
 */
@Component
@Slf4j
public class ContentEventHandler {

    private final ContentRepository contentRepository;
    private final ContentReadModelService readModelService;

    public ContentEventHandler(ContentRepository contentRepository,
                              ContentReadModelService readModelService) {
        this.contentRepository = contentRepository;
        this.readModelService = readModelService;
    }

    /**
     * Handle content created event
     * Sync to read model
     */
    @EventListener
    @Async
    @Transactional
    public void handleContentCreated(ContentCreatedEvent event) {
        log.info("Handling ContentCreatedEvent for content: {}", event.getContentId());

        try {
            Content content = contentRepository.findById(event.getContentId())
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + event.getContentId()));

            readModelService.syncFromContent(content);
            log.info("Read model synced for created content: {}", event.getContentId());
        } catch (Exception e) {
            log.error("Error handling ContentCreatedEvent for content: {}", event.getContentId(), e);
        }
    }

    /**
     * Handle content updated event
     * Update read model
     */
    @EventListener
    @Async
    @Transactional
    public void handleContentUpdated(ContentUpdatedEvent event) {
        log.info("Handling ContentUpdatedEvent for content: {}", event.getContentId());

        try {
            Content content = contentRepository.findById(event.getContentId())
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + event.getContentId()));

            readModelService.syncFromContent(content);
            log.info("Read model synced for updated content: {}", event.getContentId());
        } catch (Exception e) {
            log.error("Error handling ContentUpdatedEvent for content: {}", event.getContentId(), e);
        }
    }

    /**
     * Handle content published event
     * Update read model and potentially send notifications
     */
    @EventListener
    @Async
    @Transactional
    public void handleContentPublished(ContentPublishedEvent event) {
        log.info("Handling ContentPublishedEvent for content: {}", event.getContentId());

        try {
            Content content = contentRepository.findById(event.getContentId())
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + event.getContentId()));

            // Sync read model
            readModelService.syncFromContent(content);

            // TODO: Send notifications to subscribers
            // notificationService.notifyContentPublished(content);

            log.info("Content published event handled: {}", event.getContentId());
        } catch (Exception e) {
            log.error("Error handling ContentPublishedEvent for content: {}", event.getContentId(), e);
        }
    }

    /**
     * Handle content status changed event
     * Update read model
     */
    @EventListener
    @Async
    @Transactional
    public void handleContentStatusChanged(ContentStatusChangedEvent event) {
        log.info("Handling ContentStatusChangedEvent for content: {} - {} to {}",
            event.getContentId(), event.getOldStatus(), event.getNewStatus());

        try {
            Content content = contentRepository.findById(event.getContentId())
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + event.getContentId()));

            readModelService.syncFromContent(content);
            log.info("Read model synced for status change: {}", event.getContentId());
        } catch (Exception e) {
            log.error("Error handling ContentStatusChangedEvent for content: {}", event.getContentId(), e);
        }
    }
}
