package com.neobrutalism.crm.domain.apiendpoint.service;

import com.neobrutalism.crm.common.enums.HttpMethod;
import com.neobrutalism.crm.domain.apiendpoint.model.ApiEndpoint;
import com.neobrutalism.crm.domain.apiendpoint.repository.ApiEndpointRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for discovering and registering API endpoints from Spring controllers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EndpointDiscoveryService {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final ApiEndpointRepository apiEndpointRepository;

    /**
     * Scan and register all API endpoints from controllers
     */
    @Transactional
    public Map<String, Object> scanAndRegisterEndpoints() {
        log.info("Starting API endpoint discovery...");

        Map<RequestMappingInfo, HandlerMethod> handlerMethods =
                requestMappingHandlerMapping.getHandlerMethods();

        int totalEndpoints = 0;
        int newEndpoints = 0;
        int updatedEndpoints = 0;
        int skippedEndpoints = 0;

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // Extract endpoint information
            Set<String> patterns = mappingInfo.getPatternValues();
            Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();

            // Skip if no patterns or methods
            if (patterns.isEmpty() || methods.isEmpty()) {
                continue;
            }

            for (String pattern : patterns) {
                for (RequestMethod requestMethod : methods) {
                    totalEndpoints++;

                    try {
                        // Convert Spring RequestMethod to our HttpMethod enum
                        HttpMethod httpMethod = HttpMethod.valueOf(requestMethod.name());

                        // Check if endpoint already exists
                        Optional<ApiEndpoint> existingOpt = apiEndpointRepository
                                .findByMethodAndPath(httpMethod, pattern);

                        if (existingOpt.isPresent()) {
                            // Update existing endpoint
                            ApiEndpoint existing = existingOpt.get();
                            updateEndpointMetadata(existing, handlerMethod);
                            apiEndpointRepository.save(existing);
                            updatedEndpoints++;
                        } else {
                            // Create new endpoint
                            ApiEndpoint newEndpoint = createEndpoint(pattern, httpMethod, handlerMethod);
                            apiEndpointRepository.save(newEndpoint);
                            newEndpoints++;
                        }
                    } catch (Exception e) {
                        log.error("Failed to register endpoint: {} {}", requestMethod, pattern, e);
                        skippedEndpoints++;
                    }
                }
            }
        }

        log.info("API endpoint discovery completed. Total: {}, New: {}, Updated: {}, Skipped: {}",
                totalEndpoints, newEndpoints, updatedEndpoints, skippedEndpoints);

        return Map.of(
                "totalEndpoints", totalEndpoints,
                "newEndpoints", newEndpoints,
                "updatedEndpoints", updatedEndpoints,
                "skippedEndpoints", skippedEndpoints
        );
    }

    /**
     * Create new API endpoint from handler method
     */
    private ApiEndpoint createEndpoint(String path, HttpMethod method, HandlerMethod handlerMethod) {
        ApiEndpoint endpoint = new ApiEndpoint();
        endpoint.setPath(path);
        endpoint.setMethod(method);
        updateEndpointMetadata(endpoint, handlerMethod);
        return endpoint;
    }

    /**
     * Update endpoint metadata from handler method
     */
    private void updateEndpointMetadata(ApiEndpoint endpoint, HandlerMethod handlerMethod) {
        Method javaMethod = handlerMethod.getMethod();
        Class<?> controllerClass = handlerMethod.getBeanType();

        // Extract description from @Operation annotation (Swagger)
        io.swagger.v3.oas.annotations.Operation operation =
                javaMethod.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
        if (operation != null) {
            endpoint.setDescription(operation.summary());
        } else {
            // Build description from controller + method name
            String controllerName = controllerClass.getSimpleName().replace("Controller", "");
            String methodDesc = generateDescriptionFromMethod(javaMethod.getName());
            endpoint.setDescription(controllerName + " - " + methodDesc);
        }

        // Determine if public (check if has authentication annotations)
        endpoint.setIsPublic(isPublicEndpoint(javaMethod, controllerClass));
        endpoint.setRequiresAuth(!endpoint.getIsPublic());

        // Set tag from controller Tag annotation
        Tag tag = controllerClass.getAnnotation(Tag.class);
        if (tag != null) {
            endpoint.setTag(tag.name());
        }
    }

    /**
     * Check if endpoint is public (no authentication required)
     */
    private boolean isPublicEndpoint(Method method, Class<?> controllerClass) {
        // Check for common public endpoint patterns
        String methodName = method.getName().toLowerCase();
        if (methodName.contains("login") || methodName.contains("register") ||
            methodName.contains("public") || methodName.contains("health")) {
            return true;
        }

        // Check if controller or method has @PermitAll or similar annotations
        // This is a simplified check - you may need to add more logic based on your security setup
        return false;
    }

    /**
     * Generate human-readable description from method name
     */
    private String generateDescriptionFromMethod(String methodName) {
        // Convert camelCase to human-readable
        String result = methodName.replaceAll("([a-z])([A-Z])", "$1 $2");
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }

    /**
     * Get all unassigned endpoints (not linked to any screen)
     */
    public List<ApiEndpoint> getUnassignedEndpoints() {
        // This would require a join with screen_api_endpoints table
        // For now, return all endpoints - you can enhance this later
        return apiEndpointRepository.findAll();
    }
}
