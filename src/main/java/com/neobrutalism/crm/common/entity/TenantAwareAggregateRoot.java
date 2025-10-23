package com.neobrutalism.crm.common.entity;

import com.neobrutalism.crm.common.multitenancy.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Base aggregate root with tenant awareness
 * Combines AggregateRoot functionality with multi-tenancy support
 */
@Getter
@Setter
@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantAwareAggregateRoot<S extends Enum<S>> extends AggregateRoot<S> {

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 50)
    private String tenantId;

    @PrePersist
    protected void setTenantId() {
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
