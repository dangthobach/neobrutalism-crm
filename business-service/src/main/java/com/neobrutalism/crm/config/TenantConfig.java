package com.neobrutalism.crm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Tenant Configuration
 * Cấu hình default tenant và multi-tenancy settings
 */
@Configuration
@ConfigurationProperties(prefix = "app.tenant")
@Getter
@Setter
public class TenantConfig {

    /**
     * Default tenant ID - Sử dụng khi không có tenant context
     */
    private String defaultTenantId = "default";

    /**
     * Enable multi-tenancy
     */
    private boolean enabled = true;

    /**
     * Tenant ID header name
     */
    private String headerName = "X-Tenant-ID";

    /**
     * Strict mode - Throw exception if tenant ID is missing
     * False: Sử dụng default tenant
     * True: Throw exception
     */
    private boolean strictMode = false;
}
