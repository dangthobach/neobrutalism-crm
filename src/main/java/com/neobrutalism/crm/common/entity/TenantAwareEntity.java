package com.neobrutalism.crm.common.entity;

import com.neobrutalism.crm.common.multitenancy.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import org.hibernate.annotations.Filter;

/**
 * Base entity with tenant awareness
 * Automatically sets tenant ID on persist
 * Filters queries by tenant_id
 */
@Getter
@MappedSuperclass
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantAwareEntity extends AuditableEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 50)
    private String tenantId;

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @PrePersist
    protected void autoSetTenantId() {
        if (this.tenantId == null) {
            String currentTenant = TenantContext.getCurrentTenant();
            if (currentTenant == null) {
                throw new IllegalStateException("No tenant context set. Cannot persist tenant-aware entity.");
            }
            this.tenantId = currentTenant;
        }
    }

    @PreUpdate
    protected void validateTenantId() {
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null && !currentTenant.equals(this.tenantId)) {
            throw new IllegalStateException(
                    "Tenant mismatch. Current tenant: " + currentTenant + ", Entity tenant: " + this.tenantId);
        }
    }
}
