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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    public Optional<Role> findByCode(String code) {
        return roleRepository.findByCode(code);
    }

    public List<Role> findByOrganizationId(UUID organizationId) {
        return roleRepository.findByOrganizationId(organizationId);
    }

    public List<Role> findSystemRoles() {
        return roleRepository.findByIsSystemTrue();
    }

    @Transactional
    public Role activate(UUID id, String reason) {
        return transitionTo(id, RoleStatus.ACTIVE, reason);
    }

    @Override
    @Transactional
    public Role create(Role entity) {
        return createWithIntegrityCheck(entity, CONSTRAINT_MESSAGES);
    }

    @Override
    @Transactional
    public Role update(UUID id, Role entity) {
        return updateWithIntegrityCheck(id, entity, CONSTRAINT_MESSAGES);
    }
}
