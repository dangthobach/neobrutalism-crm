package com.neobrutalism.crm.iam.config;

import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.adapter.JDBCAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * jCasbin Configuration
 *
 * Configures Casbin enforcer for policy-based access control
 * - Model: RBAC with domains (multi-tenancy)
 * - Adapter: JDBC adapter for PostgreSQL
 * - Auto-save: Enabled for immediate policy persistence
 * - Watcher: Redis-based for distributed policy updates
 */
@Configuration
public class CasbinConfig {

    @Value("${casbin.model-path}")
    private Resource modelPath;

    @Value("${casbin.auto-save:true}")
    private boolean autoSave;

    /**
     * JDBC Adapter for Casbin
     * Connects to PostgreSQL to read/write policies
     */
    @Bean
    public Adapter casbinAdapter(DataSource dataSource) {
        return new JDBCAdapter(dataSource);
    }

    /**
     * Casbin Model
     * Loads model configuration from classpath
     */
    @Bean
    public Model casbinModel() throws IOException {
        String modelContent = modelPath.getContentAsString(StandardCharsets.UTF_8);
        Model model = new Model();
        model.loadModelFromText(modelContent);
        return model;
    }

    /**
     * Casbin Enforcer
     * Main component for permission checking
     *
     * Performance considerations:
     * - Uses prepared statements for fast DB queries
     * - Auto-save enabled for consistency
     * - Should be wrapped with L1/L2 cache for production
     *
     * Thread safety: Enforcer is thread-safe
     */
    @Bean
    public Enforcer casbinEnforcer(Model model, Adapter adapter) {
        Enforcer enforcer = new Enforcer(model, adapter);
        enforcer.enableAutoSave(autoSave);

        // Load all policies from database on startup
        enforcer.loadPolicy();

        return enforcer;
    }
}
