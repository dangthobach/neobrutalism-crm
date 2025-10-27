package com.neobrutalism.crm.domain.group.service;

import com.neobrutalism.crm.common.service.EventPublisher;
import com.neobrutalism.crm.common.service.StatefulService;
import com.neobrutalism.crm.domain.group.event.GroupDeletedEvent;
import com.neobrutalism.crm.domain.group.event.GroupUpdatedEvent;
import com.neobrutalism.crm.domain.group.model.Group;
import com.neobrutalism.crm.domain.group.model.GroupStatus;
import com.neobrutalism.crm.domain.group.repository.GroupRepository;
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
public class GroupService extends StatefulService<Group, GroupStatus> {

    private final GroupRepository groupRepository;
    private final EventPublisher eventPublisher;

    @Override
    protected GroupRepository getRepository() {
        return groupRepository;
    }

    @Override
    protected String getEntityName() {
        return "Group";
    }

    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.of(
            "code", "Group code already exists.",
            "groups_code_key", "Group code already exists."
    );

    @Override
    protected void afterCreate(Group entity) {
        super.afterCreate(entity);
        publishDomainEvents(entity);
    }

    @Override
    protected void afterUpdate(Group entity) {
        super.afterUpdate(entity);
        publishDomainEvents(entity);
        eventPublisher.publish(new GroupUpdatedEvent(
                entity.getId().toString(),
                entity.getName(),
                entity.getUpdatedBy()
        ));
    }

    @Override
    protected void afterDelete(Group entity) {
        super.afterDelete(entity);
        eventPublisher.publish(new GroupDeletedEvent(
                entity.getId().toString(),
                entity.getName(),
                entity.getUpdatedBy()
        ));
    }

    private void publishDomainEvents(Group entity) {
        if (!entity.getDomainEvents().isEmpty()) {
            eventPublisher.publishAll(entity.getDomainEvents());
            entity.clearDomainEvents();
        }
    }

    public Optional<Group> findByCode(String code) {
        return groupRepository.findByCode(code);
    }

    public List<Group> findByParentId(UUID parentId) {
        return groupRepository.findByParentId(parentId);
    }

    public List<Group> findByOrganizationId(UUID organizationId) {
        return groupRepository.findByOrganizationId(organizationId);
    }

    public List<Group> findRootGroups() {
        return groupRepository.findByParentIdIsNull();
    }

    @Transactional
    public Group activate(UUID id, String reason) {
        return transitionTo(id, GroupStatus.ACTIVE, reason);
    }

    public List<Group> findByStatus(GroupStatus status) {
        return groupRepository.findByStatus(status);
    }

    @Override
    @Transactional
    public Group create(Group entity) {
        return createWithIntegrityCheck(entity, CONSTRAINT_MESSAGES);
    }

    @Override
    @Transactional
    public Group update(UUID id, Group entity) {
        return updateWithIntegrityCheck(id, entity, CONSTRAINT_MESSAGES);
    }
}
