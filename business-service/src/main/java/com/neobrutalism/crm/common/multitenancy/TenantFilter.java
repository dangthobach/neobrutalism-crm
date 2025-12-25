package com.neobrutalism.crm.common.multitenancy;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter to extract tenant ID from HTTP request
 * Supports multiple strategies:
 * 1. HTTP Header: X-Tenant-ID
 * 2. Subdomain: {tenant}.domain.com
 * 3. Path parameter: /api/{tenant}/...
 * 4. Query parameter: ?tenantId=...
 */
@Slf4j
@Component
@Order(1)
public class TenantFilter implements Filter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT = "default";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Extract tenant ID from request
            String tenantId = extractTenantId(httpRequest);

            // Set tenant context
            TenantContext.setCurrentTenant(tenantId);

            log.debug("Tenant context set to: {}", tenantId);

            // Continue with the request
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error in tenant filter", e);
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid tenant context");
        } finally {
            // Always clear tenant context after request
            TenantContext.clear();
        }
    }

    /**
     * Extract tenant ID from HTTP request
     * Priority: Header > Query Param > Default
     */
    private String extractTenantId(HttpServletRequest request) {
        // Strategy 1: HTTP Header (recommended)
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return tenantId.trim();
        }

        // Strategy 2: Query Parameter (for testing/development)
        tenantId = request.getParameter("tenantId");
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return tenantId.trim();
        }

        // Strategy 3: Subdomain (example: tenant1.api.domain.com)
        String serverName = request.getServerName();
        if (serverName != null && serverName.contains(".")) {
            String subdomain = serverName.split("\\.")[0];
            if (!subdomain.equals("api") && !subdomain.equals("www") && !subdomain.equals("localhost")) {
                return subdomain;
            }
        }

        // Default tenant for development/testing
        return DEFAULT_TENANT;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("TenantFilter initialized");
    }

    @Override
    public void destroy() {
        log.info("TenantFilter destroyed");
    }
}
