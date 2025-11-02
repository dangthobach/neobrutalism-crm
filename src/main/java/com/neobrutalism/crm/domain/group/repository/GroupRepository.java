package com.neobrutalism.crm.domain.group.repository;

import com.neobrutalism.crm.common.repository.StatefulRepository;
import com.neobrutalism.crm.domain.group.model.Group;
import com.neobrutalism.crm.domain.group.model.GroupStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends StatefulRepository<Group, GroupStatus> {
    Optional<Group> findByCode(String code);
    List<Group> findByParentId(UUID parentId);
    List<Group> findByOrganizationId(UUID organizationId);
    List<Group> findByParentIdIsNull();
    boolean existsByCode(String code);
}
