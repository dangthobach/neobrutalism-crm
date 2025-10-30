package com.neobrutalism.crm.domain.screenapi.service;

import com.neobrutalism.crm.common.enums.PermissionType;
import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.screenapi.model.ScreenApiEndpoint;
import com.neobrutalism.crm.domain.screenapi.repository.ScreenApiEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenApiEndpointService extends BaseService<ScreenApiEndpoint> {

    private final ScreenApiEndpointRepository screenApiEndpointRepository;

    @Override
    protected ScreenApiEndpointRepository getRepository() {
        return screenApiEndpointRepository;
    }

    @Override
    protected String getEntityName() {
        return "ScreenApiEndpoint";
    }

    public List<ScreenApiEndpoint> findByScreenId(UUID screenId) {
        return screenApiEndpointRepository.findByScreenId(screenId);
    }

    public List<ScreenApiEndpoint> findByEndpointId(UUID endpointId) {
        return screenApiEndpointRepository.findByEndpointId(endpointId);
    }

    public Optional<ScreenApiEndpoint> findByScreenIdAndEndpointId(UUID screenId, UUID endpointId) {
        return screenApiEndpointRepository.findByScreenIdAndEndpointId(screenId, endpointId);
    }

    @Transactional
    public void removeScreenEndpoint(UUID screenId, UUID endpointId) {
        screenApiEndpointRepository.deleteByScreenIdAndEndpointId(screenId, endpointId);
    }

    /**
     * Bulk assign multiple endpoints to a screen
     *
     * @param screenId Screen ID
     * @param assignments List of assignments (endpointId + permission)
     * @return Number of endpoints assigned
     */
    @Transactional
    public int bulkAssignEndpoints(UUID screenId, List<BulkAssignment> assignments) {
        int assigned = 0;

        for (BulkAssignment assignment : assignments) {
            // Check if already exists
            if (findByScreenIdAndEndpointId(screenId, assignment.endpointId()).isPresent()) {
                log.debug("Screen-Endpoint link already exists: {} -> {}", screenId, assignment.endpointId());
                continue;
            }

            ScreenApiEndpoint screenApi = new ScreenApiEndpoint();
            screenApi.setScreenId(screenId);
            screenApi.setEndpointId(assignment.endpointId());
            screenApi.setRequiredPermission(assignment.requiredPermission());

            create(screenApi);
            assigned++;
        }

        log.info("Bulk assigned {} endpoints to screen {}", assigned, screenId);
        return assigned;
    }

    /**
     * Bulk remove multiple endpoints from a screen
     *
     * @param screenId Screen ID
     * @param endpointIds List of endpoint IDs to remove
     * @return Number of endpoints removed
     */
    @Transactional
    public int bulkRemoveEndpoints(UUID screenId, List<UUID> endpointIds) {
        int removed = 0;

        for (UUID endpointId : endpointIds) {
            if (findByScreenIdAndEndpointId(screenId, endpointId).isPresent()) {
                removeScreenEndpoint(screenId, endpointId);
                removed++;
            }
        }

        log.info("Bulk removed {} endpoints from screen {}", removed, screenId);
        return removed;
    }

    /**
     * Record for bulk assignment operations
     */
    public record BulkAssignment(UUID endpointId, PermissionType requiredPermission) {}
}
