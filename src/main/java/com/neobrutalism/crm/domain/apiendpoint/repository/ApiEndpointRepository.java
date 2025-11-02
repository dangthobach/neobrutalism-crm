package com.neobrutalism.crm.domain.apiendpoint.repository;

import com.neobrutalism.crm.common.enums.HttpMethod;
import com.neobrutalism.crm.common.repository.BaseRepository;
import com.neobrutalism.crm.domain.apiendpoint.model.ApiEndpoint;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiEndpointRepository extends BaseRepository<ApiEndpoint> {
    Optional<ApiEndpoint> findByMethodAndPath(HttpMethod method, String path);
    List<ApiEndpoint> findByTag(String tag);
    List<ApiEndpoint> findByIsPublicTrue();
    boolean existsByMethodAndPath(HttpMethod method, String path);
}
