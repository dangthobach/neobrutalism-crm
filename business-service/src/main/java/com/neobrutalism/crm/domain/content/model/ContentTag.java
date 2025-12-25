package com.neobrutalism.crm.domain.content.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Content tag entity for flexible content classification
 */
@Entity
@Table(name = "content_tags", indexes = {
    @Index(name = "idx_content_tags_tenant", columnList = "tenant_id"),
    @Index(name = "idx_content_tags_slug", columnList = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentTag extends SoftDeletableEntity {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Column(name = "color", length = 20)
    private String color;

    @ManyToMany(mappedBy = "tags")
    private Set<Content> contents = new HashSet<>();

    // Business methods

    /**
     * Get the number of contents with this tag
     */
    public int getContentCount() {
        return this.contents.size();
    }
}
