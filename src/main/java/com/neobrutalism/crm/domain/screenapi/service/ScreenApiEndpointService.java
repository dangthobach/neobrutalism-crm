package com.neobrutalism.crm.domain.screenapi.service;

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
}
