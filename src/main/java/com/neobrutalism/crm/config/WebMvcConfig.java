package com.neobrutalism.crm.config;

import com.neobrutalism.crm.common.api.ApiVersionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 *
 * Configures:
 * - API version interceptor
 * - CORS (handled in SecurityConfig)
 * - Custom interceptors
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiVersionInterceptor apiVersionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add API versioning interceptor for all /api/** endpoints
        registry.addInterceptor(apiVersionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/auth/**",  // Exclude auth endpoints
                    "/api/public/**" // Exclude public endpoints
                );
    }
}
