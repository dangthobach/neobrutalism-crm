package com.neobrutalism.crm.domain.content.service;

import com.neobrutalism.crm.common.service.SoftDeleteService;
import com.neobrutalism.crm.domain.attachment.model.Attachment;
import com.neobrutalism.crm.domain.attachment.repository.AttachmentRepository;
import com.neobrutalism.crm.domain.content.dto.SeriesRequest;
import com.neobrutalism.crm.domain.content.model.ContentSeries;
import com.neobrutalism.crm.domain.content.repository.ContentSeriesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for ContentSeries entity
 */
@Service
@Slf4j
public class ContentSeriesService extends SoftDeleteService<ContentSeries> {

    private final ContentSeriesRepository seriesRepository;
    private final AttachmentRepository attachmentRepository;

    public ContentSeriesService(ContentSeriesRepository seriesRepository,
                               AttachmentRepository attachmentRepository) {
        this.seriesRepository = seriesRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    protected ContentSeriesRepository getRepository() {
        return seriesRepository;
    }

    @Override
    protected String getEntityName() {
        return "ContentSeries";
    }

    /**
     * Create new series
     */
    @Transactional
    public ContentSeries createSeries(SeriesRequest request, String tenantId) {
        log.info("Creating content series: {}", request.getName());

        // Check slug uniqueness
        if (seriesRepository.existsByTenantIdAndSlugAndDeletedFalse(tenantId, request.getSlug())) {
            throw new IllegalArgumentException("Series with slug '" + request.getSlug() + "' already exists");
        }

        ContentSeries series = new ContentSeries();
        series.setTenantId(tenantId);
        series.setName(request.getName());
        series.setSlug(request.getSlug());
        series.setDescription(request.getDescription());
        series.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        // Set thumbnail if provided
        if (request.getThumbnailId() != null) {
            Attachment thumbnail = attachmentRepository.findById(request.getThumbnailId())
                .orElse(null);
            series.setThumbnail(thumbnail);
        }

        ContentSeries saved = create(series);
        log.info("Series created successfully: {}", saved.getId());
        return saved;
    }

    /**
     * Update series
     */
    @Transactional
    public ContentSeries updateSeries(UUID id, SeriesRequest request) {
        log.info("Updating series: {}", id);

        ContentSeries series = findById(id);

        if (request.getName() != null) {
            series.setName(request.getName());
        }

        if (request.getSlug() != null && !request.getSlug().equals(series.getSlug())) {
            // Check new slug uniqueness
            if (seriesRepository.existsByTenantIdAndSlugAndDeletedFalse(series.getTenantId(), request.getSlug())) {
                throw new IllegalArgumentException("Series with slug '" + request.getSlug() + "' already exists");
            }
            series.setSlug(request.getSlug());
        }

        if (request.getDescription() != null) {
            series.setDescription(request.getDescription());
        }

        if (request.getSortOrder() != null) {
            series.setSortOrder(request.getSortOrder());
        }

        if (request.getThumbnailId() != null) {
            Attachment thumbnail = attachmentRepository.findById(request.getThumbnailId())
                .orElse(null);
            series.setThumbnail(thumbnail);
        }

        ContentSeries updated = update(id, series);
        log.info("Series updated successfully: {}", id);
        return updated;
    }

    /**
     * Find series by slug
     */
    @Transactional(readOnly = true)
    public ContentSeries findBySlug(String slug, String tenantId) {
        return seriesRepository.findByTenantIdAndSlugAndDeletedFalse(tenantId, slug)
            .orElseThrow(() -> new IllegalArgumentException("Series not found with slug: " + slug));
    }

    /**
     * Find all series ordered by sort order
     */
    @Transactional(readOnly = true)
    public List<ContentSeries> findAllByTenant(String tenantId) {
        return seriesRepository.findByTenantIdAndDeletedFalseOrderBySortOrderAsc(tenantId);
    }

    /**
     * Find all series with content count
     */
    @Transactional(readOnly = true)
    public List<Object[]> findAllWithContentCount(String tenantId) {
        return seriesRepository.findAllWithContentCount(tenantId);
    }
}
