package com.neobrutalism.crm.domain.role.service;

import com.neobrutalism.crm.common.service.EventPublisher;
import com.neobrutalism.crm.common.service.StatefulService;
import com.neobrutalism.crm.domain.role.event.RoleDeletedEvent;
import com.neobrutalism.crm.domain.role.event.RoleUpdatedEvent;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.model.RoleStatus;
import com.neobrutalism.crm.domain.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ✅ PHASE 1 WEEK 3: Service for Role management with Redis caching
 * Cache region: "roles" with 1 hour TTL (roles change infrequently)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService extends StatefulService<Role, RoleStatus> {

    private final RoleRepository roleRepository;
    private final EventPublisher eventPublisher;

    @Override
    protected RoleRepository getRepository() {
        return roleRepository;
    }

    @Override
    protected String getEntityName() {
        return "Role";
    }

    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.of(
            "code", "Role code already exists.",
            "roles_code_key", "Role code already exists."
    );

    @Override
    protected void afterCreate(Role entity) {
        super.afterCreate(entity);
        publishDomainEvents(entity);
    }

    @Override
    protected void afterUpdate(Role entity) {
        super.afterUpdate(entity);
        publishDomainEvents(entity);
        eventPublisher.publish(new RoleUpdatedEvent(
                entity.getId().toString(),
                entity.getName(),
                entity.getUpdatedBy()
        ));
    }

    @Override
    protected void afterDelete(Role entity) {
        super.afterDelete(entity);
        eventPublisher.publish(new RoleDeletedEvent(
                entity.getId().toString(),
                entity.getName(),
                entity.getUpdatedBy()
        ));
    }

    private void publishDomainEvents(Role entity) {
        if (!entity.getDomainEvents().isEmpty()) {
            eventPublisher.publishAll(entity.getDomainEvents());
            entity.clearDomainEvents();
        }
    }

    /**
     * Find role by code
     * Cached: 1 hour TTL, key by code and tenant
     */
    @Cacheable(value = "roles", key = "'code:' + #code + ':tenant:' + T(com.neobrutalism.crm.common.multitenancy.TenantContext).getCurrentTenant()")
    public Optional<Role> findByCode(String code) {
        return roleRepository.findByCode(code);
    }

    /**
     * Find roles by organization
     * Cached: 1 hour TTL, key by organization ID
     */
    @Cacheable(value = "roles", key = "'org:' + #organizationId")
    public List<Role> findByOrganizationId(UUID organizationId) {
        return roleRepository.findByOrganizationId(organizationId);
    }

    /**
     * Find system roles
     * Cached: 1 hour TTL, key by tenant
     */
    @Cacheable(value = "roles", key = "'system:tenant:' + T(com.neobrutalism.crm.common.multitenancy.TenantContext).getCurrentTenant()")
    public List<Role> findSystemRoles() {
        return roleRepository.findByIsSystemTrue();
    }

    /**
     * Activate role
     * Cache eviction: Clears all roles cache
     */
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public Role activate(UUID id, String reason) {
        return transitionTo(id, RoleStatus.ACTIVE, reason);
    }

    /**
     * Find roles by status
     * Cached: 1 hour TTL, key by status and tenant
     */
    @Cacheable(value = "roles", key = "'status:' + #status + ':tenant:' + T(com.neobrutalism.crm.common.multitenancy.TenantContext).getCurrentTenant()")
    public List<Role> findByStatus(RoleStatus status) {
        return roleRepository.findByStatus(status);
    }

    /**
     * Create role with database integrity constraint checking
     * Cache eviction: Clears all roles cache
     */
    @Override
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public Role create(Role entity) {
        return createWithIntegrityCheck(entity, CONSTRAINT_MESSAGES);
    }

    /**
     * Update role with database integrity constraint checking
     * Cache eviction: Clears all roles cache
     */
    @Override
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public Role update(UUID id, Role entity) {
        return updateWithIntegrityCheck(id, entity, CONSTRAINT_MESSAGES);
    }
}
