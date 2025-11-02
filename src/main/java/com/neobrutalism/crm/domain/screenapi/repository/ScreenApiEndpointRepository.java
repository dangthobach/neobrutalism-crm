package com.neobrutalism.crm.domain.screenapi.repository;

import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.screenapi.model.ScreenApiEndpoint;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScreenApiEndpointRepository extends BaseRepository<ScreenApiEndpoint> {
    List<ScreenApiEndpoint> findByScreenId(UUID screenId);
    List<ScreenApiEndpoint> findByEndpointId(UUID endpointId);
    Optional<ScreenApiEndpoint> findByScreenIdAndEndpointId(UUID screenId, UUID endpointId);
    boolean existsByScreenIdAndEndpointId(UUID screenId, UUID endpointId);
    void deleteByScreenIdAndEndpointId(UUID screenId, UUID endpointId);
}
