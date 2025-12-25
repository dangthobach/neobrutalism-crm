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
 * Content category entity for organizing content
 * Supports hierarchical categories (parent-child relationship)
 */
@Entity
@Table(name = "content_categories", indexes = {
    @Index(name = "idx_content_categories_tenant", columnList = "tenant_id"),
    @Index(name = "idx_content_categories_parent", columnList = "parent_id"),
    @Index(name = "idx_content_categories_slug", columnList = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentCategory extends SoftDeletableEntity {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", nullable = false, length = 200)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ContentCategory parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ContentCategory> children = new HashSet<>();

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @ManyToMany(mappedBy = "categories")
    private Set<Content> contents = new HashSet<>();

    // Business methods

    /**
     * Check if this is a root category (no parent)
     */
    public boolean isRoot() {
        return this.parent == null;
    }

    /**
     * Check if this category has children
     */
    public boolean hasChildren() {
        return !this.children.isEmpty();
    }

    /**
     * Add a child category
     */
    public void addChild(ContentCategory child) {
        this.children.add(child);
        child.setParent(this);
    }

    /**
     * Remove a child category
     */
    public void removeChild(ContentCategory child) {
        this.children.remove(child);
        child.setParent(null);
    }
}
