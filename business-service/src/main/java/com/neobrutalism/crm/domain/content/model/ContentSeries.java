package com.neobrutalism.crm.domain.content.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import com.neobrutalism.crm.domain.attachment.model.Attachment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Content series entity for grouping related content
 */
@Entity
@Table(name = "content_series", indexes = {
    @Index(name = "idx_content_series_tenant", columnList = "tenant_id"),
    @Index(name = "idx_content_series_slug", columnList = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentSeries extends SoftDeletableEntity {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", nullable = false, length = 200)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_id")
    private Attachment thumbnail;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("seriesOrder ASC")
    private List<Content> contents = new ArrayList<>();

    // Business methods

    /**
     * Get the number of contents in this series
     */
    public int getContentCount() {
        return this.contents.size();
    }

    /**
     * Add content to series
     */
    public void addContent(Content content) {
        this.contents.add(content);
        content.setSeries(this);
        content.setSeriesOrder(this.contents.size());
    }

    /**
     * Remove content from series
     */
    public void removeContent(Content content) {
        this.contents.remove(content);
        content.setSeries(null);
        content.setSeriesOrder(0);
        reorderContents();
    }

    /**
     * Reorder contents after removal
     */
    private void reorderContents() {
        for (int i = 0; i < this.contents.size(); i++) {
            this.contents.get(i).setSeriesOrder(i + 1);
        }
    }
}
