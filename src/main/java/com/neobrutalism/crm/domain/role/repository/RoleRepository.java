package com.neobrutalism.crm.domain.role.repository;

import com.neobrutalism.crm.common.repository.StatefulRepository;
import com.neobrutalism.crm.domain.role.model.Role;
import com.neobrutalism.crm.domain.role.model.RoleStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends StatefulRepository<Role, RoleStatus> {
    Optional<Role> findByCode(String code);
    Optional<Role> findByCodeAndDeletedFalse(String code);
    List<Role> findByOrganizationId(UUID organizationId);
    List<Role> findByIsSystemTrue();
    boolean existsByCode(String code);
}
