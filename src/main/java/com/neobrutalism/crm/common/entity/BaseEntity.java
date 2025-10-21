package com.neobrutalism.crm.common.entity;

import com.neobrutalism.crm.common.generator.UuidV7Generator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Base entity with UUID v7 (time-ordered) and version control
 *
 * UUID v7 Benefits:
 * - Time-ordered: Better B-tree index performance
 * - Sequential inserts: Reduces page splits
 * - Clustered: Better cache locality
 * - Distributed-safe: No coordination needed
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(generator = "uuid-v7")
    @GenericGenerator(name = "uuid-v7", type = UuidV7Generator.class)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Version
    @Column(name = "version")
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public boolean isNew() {
        return id == null;
    }

    public UUID getId() {
        return id;
    }
}
