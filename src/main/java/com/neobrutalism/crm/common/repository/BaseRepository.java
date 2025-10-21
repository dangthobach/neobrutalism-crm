package com.neobrutalism.crm.common.repository;

import com.neobrutalism.crm.common.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.UUID;

/**
 * Base repository with CRUD operations and Specification support
 * Uses UUID as primary key type
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity>
        extends JpaRepository<T, UUID>, JpaSpecificationExecutor<T> {
}
