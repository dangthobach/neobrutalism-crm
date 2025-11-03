package com.neobrutalism.crm.domain.content.mapper;

import com.neobrutalism.crm.domain.content.dto.*;
import com.neobrutalism.crm.domain.content.model.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper for Content entity and DTOs
 */
@Component
public class ContentMapper {

    /**
     * Convert Content entity to ContentDTO
     */
    public ContentDTO toDTO(Content content) {
        if (content == null) {
            return null;
        }

        ContentDTO dto = new ContentDTO();
        dto.setId(content.getId());
        dto.setTenantId(content.getTenantId());
        dto.setTitle(content.getTitle());
        dto.setSlug(content.getSlug());
        dto.setSummary(content.getSummary());
        dto.setBody(content.getBody());
        dto.setContentType(content.getContentType());
        dto.setStatus(content.getStatus());
        dto.setPublishedAt(content.getPublishedAt());
        dto.setViewCount(content.getViewCount());
        dto.setTierRequired(content.getTierRequired());

        // Featured image
        if (content.getFeaturedImage() != null) {
            dto.setFeaturedImageUrl(content.getFeaturedImage().getFilePath());
        }

        // Author
        if (content.getAuthor() != null) {
            ContentDTO.AuthorDTO authorDTO = new ContentDTO.AuthorDTO();
            authorDTO.setId(content.getAuthor().getId());
            authorDTO.setFullName(content.getAuthor().getFullName());
            authorDTO.setEmail(content.getAuthor().getEmail());
            // authorDTO.setAvatarUrl(content.getAuthor().getAvatarUrl());
            dto.setAuthor(authorDTO);
        }

        // Series
        if (content.getSeries() != null) {
            dto.setSeries(toSeriesDTO(content.getSeries()));
        }

        // Categories
        if (content.getCategories() != null) {
            dto.setCategories(
                content.getCategories().stream()
                    .map(this::toCategoryDTO)
                    .collect(Collectors.toSet())
            );
        }

        // Tags
        if (content.getTags() != null) {
            dto.setTags(
                content.getTags().stream()
                    .map(this::toTagDTO)
                    .collect(Collectors.toSet())
            );
        }

        // SEO
        dto.setSeoTitle(content.getSeoTitle());
        dto.setSeoDescription(content.getSeoDescription());
        dto.setSeoKeywords(content.getSeoKeywords());

        // Audit
        dto.setCreatedAt(content.getCreatedAt());
        dto.setUpdatedAt(content.getUpdatedAt());
        dto.setVersion(content.getVersion());

        return dto;
    }

    /**
     * Convert ContentCategory to DTO
     */
    public ContentCategoryDTO toCategoryDTO(ContentCategory category) {
        if (category == null) {
            return null;
        }

        ContentCategoryDTO dto = new ContentCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());
        dto.setDescription(category.getDescription());
        dto.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        dto.setSortOrder(category.getSortOrder());
        dto.setContentCount(category.getContents() != null ? Integer.valueOf(category.getContents().size()) : 0);
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());

        return dto;
    }

    /**
     * Convert ContentTag to DTO
     */
    public ContentTagDTO toTagDTO(ContentTag tag) {
        if (tag == null) {
            return null;
        }

        ContentTagDTO dto = new ContentTagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setSlug(tag.getSlug());
        dto.setColor(tag.getColor());
        dto.setContentCount(tag.getContents() != null ? tag.getContents().size() : 0);

        return dto;
    }

    /**
     * Convert ContentSeries to DTO
     */
    public ContentSeriesDTO toSeriesDTO(ContentSeries series) {
        if (series == null) {
            return null;
        }

        ContentSeriesDTO dto = new ContentSeriesDTO();
        dto.setId(series.getId());
        dto.setName(series.getName());
        dto.setSlug(series.getSlug());
        dto.setDescription(series.getDescription());
        dto.setThumbnailUrl(series.getThumbnail() != null ? series.getThumbnail().getFilePath() : null);
        dto.setSortOrder(series.getSortOrder());
        dto.setContentCount(series.getContents() != null ? series.getContents().size() : 0);
        dto.setCreatedAt(series.getCreatedAt());
        dto.setUpdatedAt(series.getUpdatedAt());

        return dto;
    }

    /**
     * Convert ContentReadModel to ContentDTO (for queries)
     */
    public ContentDTO fromReadModel(ContentReadModel readModel) {
        if (readModel == null) {
            return null;
        }

        ContentDTO dto = new ContentDTO();
        dto.setId(readModel.getId());
        dto.setTenantId(readModel.getTenantId());
        dto.setTitle(readModel.getTitle());
        dto.setSlug(readModel.getSlug());
        dto.setSummary(readModel.getSummary());
        // Note: Body is not in read model (too large)
        dto.setContentType(readModel.getContentType());
        dto.setStatus(readModel.getStatus());
        dto.setPublishedAt(readModel.getPublishedAt());
        dto.setViewCount(readModel.getViewCount());
        dto.setTierRequired(readModel.getTierRequired());
        dto.setFeaturedImageUrl(readModel.getFeaturedImageUrl());

        // Author (denormalized)
        ContentDTO.AuthorDTO authorDTO = new ContentDTO.AuthorDTO();
        authorDTO.setId(readModel.getAuthorId());
        authorDTO.setFullName(readModel.getAuthorName());
        authorDTO.setAvatarUrl(readModel.getAuthorAvatarUrl());
        dto.setAuthor(authorDTO);

        // SEO
        dto.setSeoTitle(readModel.getSeoTitle());
        dto.setSeoDescription(readModel.getSeoDescription());

        // Audit
        dto.setCreatedAt(readModel.getCreatedAt());
        dto.setUpdatedAt(readModel.getUpdatedAt());

        return dto;
    }
}
