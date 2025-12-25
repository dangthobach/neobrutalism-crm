package com.neobrutalism.crm.domain.apiendpoint.service;

import com.neobrutalism.crm.common.enums.HttpMethod;
import com.neobrutalism.crm.common.service.BaseService;
import com.neobrutalism.crm.domain.apiendpoint.model.ApiEndpoint;
import com.neobrutalism.crm.domain.apiendpoint.repository.ApiEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiEndpointService extends BaseService<ApiEndpoint> {

    private final ApiEndpointRepository apiEndpointRepository;

    @Override
    protected ApiEndpointRepository getRepository() {
        return apiEndpointRepository;
    }

    @Override
    protected String getEntityName() {
        return "ApiEndpoint";
    }

    public Optional<ApiEndpoint> findByMethodAndPath(HttpMethod method, String path) {
        return apiEndpointRepository.findByMethodAndPath(method, path);
    }

    public List<ApiEndpoint> findByTag(String tag) {
        return apiEndpointRepository.findByTag(tag);
    }

    public List<ApiEndpoint> findPublicEndpoints() {
        return apiEndpointRepository.findByIsPublicTrue();
    }
}
